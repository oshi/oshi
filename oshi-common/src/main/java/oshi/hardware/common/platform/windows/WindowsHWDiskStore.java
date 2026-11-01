/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static oshi.driver.common.windows.wmi.MSFTStorage.PD_OBJECT_ID;
import static oshi.driver.common.windows.wmi.MSFTStorage.VD_OBJECT_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PhysicalDisk.PhysicalDiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage;
import oshi.driver.common.windows.wmi.MSFTStorage.DiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.PhysicalDiskIdProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.PhysicalDiskMediaTypeProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.VirtualDiskIdProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.VirtualDiskToPhysicalDiskProperty;
import oshi.driver.common.windows.wmi.Win32DiskDrive.DiskDriveProperty;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.tuples.Pair;

/**
 * Common Windows hard disk implementation shared between JNA and FFM.
 */
@ThreadSafe
public abstract class WindowsHWDiskStore extends AbstractHWDiskStore {
    private static final Pattern WHITESPACE_CHAR = Pattern.compile("\\s");

    private static final Logger LOG = LoggerFactory.getLogger(WindowsHWDiskStore.class);

    /** The prefix for physical drive paths. */
    protected static final String PHYSICALDRIVE_PREFIX = "\\\\.\\PHYSICALDRIVE";
    /** Pattern to extract DeviceID from WMI association strings. */
    protected static final Pattern DEVICE_ID = Pattern.compile(".*\\.DeviceID=\"(.*)\"");

    /** Buffer size for volume GUID paths. */
    protected static final int GUID_BUFSIZE = 100;
    /** Buffer size for volume labels. */
    protected static final int LABEL_BUFSIZE = 33;

    /** Disk type reported when the media technology could not be determined. */
    protected static final String DISK_TYPE_UNKNOWN = "Unknown";

    private final Supplier<Map<String, List<String>>> backingStores;
    private static final String DISK_TYPE_SSD = "SSD";
    private static final String DISK_TYPE_HDD = "HDD";
    private static final String DISK_TYPE_REMOVABLE = "Removable";
    private static final String DISK_TYPE_VIRTUAL = "Virtual";

    /** IOCTL code to query storage device properties. */
    protected static final int IOCTL_STORAGE_QUERY_PROPERTY = 0x002D1400;
    /** {@code STORAGE_PROPERTY_ID} selecting {@code STORAGE_DEVICE_DESCRIPTOR}. */
    protected static final int STORAGE_DEVICE_PROPERTY = 0;
    /** {@code STORAGE_PROPERTY_ID} selecting {@code DEVICE_SEEK_PENALTY_DESCRIPTOR}. Requires Windows 7. */
    protected static final int STORAGE_DEVICE_SEEK_PENALTY_PROPERTY = 7;
    /** {@code STORAGE_QUERY_TYPE} for a standard property query. */
    protected static final int PROPERTY_STANDARD_QUERY = 0;

    /** Size of {@code STORAGE_PROPERTY_QUERY}: PropertyId(4) + QueryType(4) + AdditionalParameters(1), padded. */
    protected static final int STORAGE_PROPERTY_QUERY_SIZE = 12;
    /** Buffer size for {@code STORAGE_DEVICE_DESCRIPTOR}, whose trailing RawDeviceProperties is variable length. */
    protected static final int STORAGE_DEVICE_DESCRIPTOR_BUFSIZE = 1024;
    /** Byte offset of {@code STORAGE_DEVICE_DESCRIPTOR}'s RemovableMedia (BOOLEAN) member. */
    protected static final int SDD_REMOVABLE_MEDIA_OFFSET = 10;
    /** Byte offset of {@code STORAGE_DEVICE_DESCRIPTOR}'s BusType ({@code STORAGE_BUS_TYPE}) member. */
    protected static final int SDD_BUS_TYPE_OFFSET = 28;
    /** Size of {@code DEVICE_SEEK_PENALTY_DESCRIPTOR}: Version(4) + Size(4) + IncursSeekPenalty(1), padded. */
    protected static final int SEEK_PENALTY_DESCRIPTOR_SIZE = 12;
    /** Byte offset of {@code DEVICE_SEEK_PENALTY_DESCRIPTOR}'s IncursSeekPenalty (BOOLEAN) member. */
    protected static final int SPD_INCURS_SEEK_PENALTY_OFFSET = 8;

    // STORAGE_BUS_TYPE values identifying a disk that is not a directly attached physical device
    private static final int BUS_TYPE_FILE_BACKED_VIRTUAL = 15;
    private static final int BUS_TYPE_SPACES = 16;

    /**
     * Timeout for the {@code MSFT_PhysicalDisk} fallback query, used when no finite timeout is configured.
     * <p>
     * {@code oshi.util.wmi.timeout} defaults to infinite, and this query runs in a path callers poll. The Storage WMI
     * provider is markedly less reliable than the storage driver, so it is bounded here: timing out yields the
     * {@code Unknown} the caller would have had anyway, where hanging would block {@code getDiskStores()} forever.
     */
    protected static final int MSFT_PHYSICAL_DISK_TIMEOUT_MS = 5_000;

    // MSFT_PhysicalDisk MediaType values
    private static final int MSFT_MEDIA_TYPE_HDD = 3;
    private static final int MSFT_MEDIA_TYPE_SSD = 4;
    private static final int MSFT_MEDIA_TYPE_SCM = 5;

    /**
     * Constructor.
     *
     * @param name          the name
     * @param model         the model
     * @param serial        the serial
     * @param size          the size
     * @param diskType      the diskType
     * @param backingStores supplies the backing stores of every disk in the enumeration this disk came from, keyed by
     *                      disk name; shared by those disks so that the Storage namespace is queried at most once
     */
    protected WindowsHWDiskStore(String name, String model, String serial, long size, String diskType,
            Supplier<Map<String, List<String>>> backingStores) {
        super(name, model, serial, size, diskType);
        this.backingStores = backingStores;
    }

    @Override
    public Optional<List<String>> getBackingStores() {
        return Optional.ofNullable(backingStores.get().get(getName()));
    }

    /**
     * Queries the Storage namespace for what each disk is built on. Requires Windows 8 or Server 2012.
     *
     * @param h       an instantiated {@link WmiQueryExecutor}, with COM already initialized
     * @param mountOf returns the mount point of the volume holding a path, or an empty string if it cannot be read
     * @return the backing stores of each disk the Storage namespace reports, keyed by disk name
     */
    protected static Map<String, List<String>> queryBackingStores(WmiQueryExecutor h, UnaryOperator<String> mountOf) {
        return buildBackingStores(MSFTStorage.queryDisks(h), MSFTStorage.queryVirtualDiskIds(h),
                MSFTStorage.queryVirtualDiskToPhysicalDisks(h), MSFTStorage.queryPhysicalDiskIds(h), mountOf);
    }

    /**
     * Correlates the Storage namespace query results into each disk's backing stores. A file-backed virtual disk is
     * built on the volume holding its backing file. A Storage Spaces disk is built on the physical disks holding its
     * virtual disk, which Windows does not list as disks of their own once they are pooled. Any other disk is built on
     * nothing. A disk whose backing stores cannot be determined is left out, so it reports none rather than an empty
     * list.
     *
     * @param disks   the {@code MSFT_Disk} query result
     * @param vds     the {@code MSFT_VirtualDisk} identity query result
     * @param vdpd    the virtual disk to physical disk association query result
     * @param pds     the {@code MSFT_PhysicalDisk} identity query result
     * @param mountOf returns the mount point of the volume holding a path, or an empty string if it cannot be read
     * @return the backing stores of each disk, keyed by disk name, sorted
     */
    protected static Map<String, List<String>> buildBackingStores(WmiResult<DiskProperty> disks,
            WmiResult<VirtualDiskIdProperty> vds, WmiResult<VirtualDiskToPhysicalDiskProperty> vdpd,
            WmiResult<PhysicalDiskIdProperty> pds, UnaryOperator<String> mountOf) {
        try {
            Map<String, String> deviceIdByPd = new HashMap<>();
            for (int i = 0; i < pds.getResultCount(); i++) {
                String pd = objectGuid(PD_OBJECT_ID, WmiUtil.getString(pds, PhysicalDiskIdProperty.OBJECTID, i));
                deviceIdByPd.put(pd, WmiUtil.getString(pds, PhysicalDiskIdProperty.DEVICEID, i));
            }
            Map<String, String> vdByUniqueId = new HashMap<>();
            for (int i = 0; i < vds.getResultCount(); i++) {
                vdByUniqueId.put(WmiUtil.getString(vds, VirtualDiskIdProperty.UNIQUEID, i),
                        objectGuid(VD_OBJECT_ID, WmiUtil.getString(vds, VirtualDiskIdProperty.OBJECTID, i)));
            }
            Map<String, List<String>> pdsByVd = new HashMap<>();
            for (int i = 0; i < vdpd.getResultCount(); i++) {
                String vd = objectGuid(VD_OBJECT_ID,
                        WmiUtil.getRefString(vdpd, VirtualDiskToPhysicalDiskProperty.VIRTUALDISK, i));
                String pd = objectGuid(PD_OBJECT_ID,
                        WmiUtil.getRefString(vdpd, VirtualDiskToPhysicalDiskProperty.PHYSICALDISK, i));
                pdsByVd.computeIfAbsent(vd, k -> new ArrayList<>()).add(pd);
            }
            Map<String, List<String>> result = new HashMap<>();
            for (int i = 0; i < disks.getResultCount(); i++) {
                String name = PHYSICALDRIVE_PREFIX + WmiUtil.getUint32(disks, DiskProperty.NUMBER, i);
                int busType = WmiUtil.getUint16(disks, DiskProperty.BUSTYPE, i);
                if (busType == BUS_TYPE_FILE_BACKED_VIRTUAL) {
                    String location = WmiUtil.getString(disks, DiskProperty.LOCATION, i);
                    String mount = location.isEmpty() ? "" : mountOf.apply(location);
                    if (!mount.isEmpty()) {
                        result.put(name, Collections.singletonList(StoreKind.FILE.entry(mount)));
                    }
                } else if (busType == BUS_TYPE_SPACES) {
                    String vd = vdByUniqueId.get(WmiUtil.getString(disks, DiskProperty.UNIQUEID, i));
                    TreeSet<String> stores = new TreeSet<>();
                    for (String pd : pdsByVd.getOrDefault(vd == null ? "" : vd, Collections.emptyList())) {
                        String deviceId = deviceIdByPd.get(pd);
                        if (deviceId != null && !deviceId.isEmpty()) {
                            stores.add(StoreKind.DISK.entry(PHYSICALDRIVE_PREFIX + deviceId));
                        }
                    }
                    if (!stores.isEmpty()) {
                        result.put(name, Collections.unmodifiableList(new ArrayList<>(stores)));
                    }
                } else {
                    result.put(name, Collections.emptyList());
                }
            }
            return Collections.unmodifiableMap(result);
        } catch (ClassCastException e) {
            LOG.debug("Unexpected Storage namespace property type", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Extracts the GUIDs identifying a storage object from its object ID or a reference to it.
     *
     * @param pattern  the pattern matching that kind of object
     * @param objectId the object ID or reference
     * @return the captured GUIDs, or the whole string if it does not match
     */
    private static String objectGuid(Pattern pattern, String objectId) {
        Matcher m = pattern.matcher(objectId);
        if (!m.find()) {
            return objectId;
        }
        StringBuilder sb = new StringBuilder();
        for (int g = 1; g <= m.groupCount(); g++) {
            sb.append(m.group(g));
        }
        return sb.toString();
    }

    /**
     * Sets all disk statistics from a DiskStats object for the given index.
     *
     * @param stats the disk stats
     * @param index the disk index key
     */
    protected void setDiskStats(DiskStats stats, String index) {
        super.setDiskStats(stats.getReadMap().getOrDefault(index, 0L), stats.getReadByteMap().getOrDefault(index, 0L),
                stats.getWriteMap().getOrDefault(index, 0L), stats.getWriteByteMap().getOrDefault(index, 0L),
                stats.getQueueLengthMap().getOrDefault(index, 0L), stats.getDiskTimeMap().getOrDefault(index, 0L),
                stats.getTimeStamp());
    }

    @Override
    public synchronized boolean updateAttributes() {
        String index = null;
        List<HWPartition> partitions = getPartitions();
        if (!partitions.isEmpty()) {
            index = Integer.toString(partitions.get(0).getMajor());
        } else if (getName().startsWith(PHYSICALDRIVE_PREFIX)) {
            index = getName().substring(PHYSICALDRIVE_PREFIX.length());
        } else {
            LOG.warn("Couldn't match index for {}", getName());
            return false;
        }
        DiskStats stats = queryReadWriteStats(index);
        if (stats.getReadMap().containsKey(index)) {
            setDiskStats(stats, index);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Queries performance counter data for disk read/write stats.
     *
     * @param index The disk index to query, or null for all disks
     * @return A non-null {@link DiskStats} object; its maps (e.g. {@link DiskStats#getReadMap()}) are never null but
     *         may be empty if no counters matched the given index
     */
    protected abstract DiskStats queryReadWriteStats(String index);

    /**
     * Populates a DiskStats object from performance counter data.
     *
     * @param index             The disk index to filter on, or null for all
     * @param instanceValuePair The raw PDH counter data
     * @return A populated DiskStats object
     */
    protected static DiskStats populateDiskStats(@Nullable String index,
            Pair<List<String>, Map<PhysicalDiskProperty, List<Long>>> instanceValuePair) {
        DiskStats stats = new DiskStats();
        List<String> instances = instanceValuePair.getA();
        Map<PhysicalDiskProperty, List<Long>> valueMap = instanceValuePair.getB();
        stats.setTimeStamp(System.currentTimeMillis());
        List<Long> readList = valueMap.get(PhysicalDiskProperty.DISKREADSPERSEC);
        List<Long> readByteList = valueMap.get(PhysicalDiskProperty.DISKREADBYTESPERSEC);
        List<Long> writeList = valueMap.get(PhysicalDiskProperty.DISKWRITESPERSEC);
        List<Long> writeByteList = valueMap.get(PhysicalDiskProperty.DISKWRITEBYTESPERSEC);
        List<Long> queueLengthList = valueMap.get(PhysicalDiskProperty.CURRENTDISKQUEUELENGTH);
        List<Long> diskTimeList = valueMap.get(PhysicalDiskProperty.PERCENTDISKTIME);

        if (instances.isEmpty() || readList == null || readByteList == null || writeList == null
                || writeByteList == null || queueLengthList == null || diskTimeList == null) {
            LOG.debug("Empty disk stats for index {}: instances={}, counters null: r={} rb={} w={} wb={} ql={} dt={}",
                    index, instances.isEmpty(), readList == null, readByteList == null, writeList == null,
                    writeByteList == null, queueLengthList == null, diskTimeList == null);
            return stats;
        }
        for (int i = 0; i < instances.size(); i++) {
            String name = getIndexFromName(instances.get(i));
            if (index != null && !index.equals(name)) {
                continue;
            }
            stats.getReadMap().put(name, readList.get(i));
            stats.getReadByteMap().put(name, readByteList.get(i));
            stats.getWriteMap().put(name, writeList.get(i));
            stats.getWriteByteMap().put(name, writeByteList.get(i));
            stats.getQueueLengthMap().put(name, queueLengthList.get(i));
            stats.getDiskTimeMap().put(name, diskTimeList.get(i) / 10_000L);
        }
        return stats;
    }

    /**
     * Populates the drive-to-partition map from WMI association data.
     *
     * @param maps       The PartitionMaps to populate
     * @param antecedent The antecedent device ID string
     * @param dependent  The dependent device ID string
     */
    protected static void mapDriveToPartition(PartitionMaps maps, String antecedent, String dependent) {
        Matcher mAnt = DEVICE_ID.matcher(antecedent);
        Matcher mDep = DEVICE_ID.matcher(dependent);
        if (mAnt.matches() && mDep.matches()) {
            maps.getDriveToPartitionMap().computeIfAbsent(mAnt.group(1).replace("\\\\", "\\"), x -> new ArrayList<>())
                    .add(mDep.group(1));
        }
    }

    /**
     * Populates the partition-to-logical-drive map from WMI association data.
     *
     * @param maps       The PartitionMaps to populate
     * @param antecedent The antecedent device ID string
     * @param dependent  The dependent device ID string
     * @param size       The partition size
     */
    protected static void mapPartitionToLogicalDrive(PartitionMaps maps, String antecedent, String dependent,
            long size) {
        Matcher mAnt = DEVICE_ID.matcher(antecedent);
        Matcher mDep = DEVICE_ID.matcher(dependent);
        if (mAnt.matches() && mDep.matches()) {
            maps.getPartitionToLogicalDriveMap().computeIfAbsent(mAnt.group(1), x -> new ArrayList<>())
                    .add(new Pair<>(mDep.group(1) + "\\", size));
        }
    }

    /**
     * Builds an unmodifiable sorted partition list from the maps.
     *
     * @param maps     The partition maps
     * @param diskName The disk name to look up
     * @return A sorted unmodifiable list of partitions
     */
    protected static List<HWPartition> buildPartitionList(PartitionMaps maps, String diskName) {
        List<HWPartition> partitions = new ArrayList<>();
        List<String> partList = maps.getDriveToPartitionMap().get(diskName);
        if (partList != null && !partList.isEmpty()) {
            for (String part : partList) {
                if (maps.getPartitionMap().containsKey(part)) {
                    partitions.addAll(maps.getPartitionMap().get(part));
                }
            }
        }
        return Collections.unmodifiableList(
                partitions.stream().sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList()));
    }

    /**
     * Parse a drive name like "0 C:" to just the index "0"
     *
     * @param s A drive name to parse
     * @return The first space-delimited value
     */
    protected static String getIndexFromName(String s) {
        return WHITESPACE_CHAR.split(s, 2)[0];
    }

    /**
     * Maps to store read/write bytes per drive index
     */
    protected static final class DiskStats {
        private final Map<String, Long> readMap = new HashMap<>();
        private final Map<String, Long> readByteMap = new HashMap<>();
        private final Map<String, Long> writeMap = new HashMap<>();
        private final Map<String, Long> writeByteMap = new HashMap<>();
        private final Map<String, Long> queueLengthMap = new HashMap<>();
        private final Map<String, Long> diskTimeMap = new HashMap<>();
        private long timeStamp;

        /** Creates an empty DiskStats instance. */
        public DiskStats() {
        }

        /**
         * Gets the read count map.
         *
         * @return map of disk index to read count
         */
        public Map<String, Long> getReadMap() {
            return readMap;
        }

        /**
         * Gets the read bytes map.
         *
         * @return map of disk index to read bytes
         */
        public Map<String, Long> getReadByteMap() {
            return readByteMap;
        }

        /**
         * Gets the write count map.
         *
         * @return map of disk index to write count
         */
        public Map<String, Long> getWriteMap() {
            return writeMap;
        }

        /**
         * Gets the write bytes map.
         *
         * @return map of disk index to write bytes
         */
        public Map<String, Long> getWriteByteMap() {
            return writeByteMap;
        }

        /**
         * Gets the queue length map.
         *
         * @return map of disk index to queue length
         */
        public Map<String, Long> getQueueLengthMap() {
            return queueLengthMap;
        }

        /**
         * Gets the disk time map.
         *
         * @return map of disk index to disk time in milliseconds
         */
        public Map<String, Long> getDiskTimeMap() {
            return diskTimeMap;
        }

        /**
         * Gets the timestamp when stats were collected.
         *
         * @return the timestamp in milliseconds
         */
        public long getTimeStamp() {
            return timeStamp;
        }

        /**
         * Sets the timestamp.
         *
         * @param timeStamp the timestamp in milliseconds
         */
        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }
    }

    /**
     * Maps for the partition structure
     */
    protected static final class PartitionMaps {
        private final Map<String, List<String>> driveToPartitionMap = new HashMap<>();
        private final Map<String, List<Pair<String, Long>>> partitionToLogicalDriveMap = new HashMap<>();
        private final Map<String, List<HWPartition>> partitionMap = new HashMap<>();

        /** Creates an empty PartitionMaps instance. */
        public PartitionMaps() {
        }

        /**
         * Gets the drive-to-partition map.
         *
         * @return map of drive name to its partition identifiers
         */
        public Map<String, List<String>> getDriveToPartitionMap() {
            return driveToPartitionMap;
        }

        /**
         * Gets the partition-to-logical-drive map.
         *
         * @return map of partition to logical drive letter and size pairs
         */
        public Map<String, List<Pair<String, Long>>> getPartitionToLogicalDriveMap() {
            return partitionToLogicalDriveMap;
        }

        /**
         * Gets the partition map.
         *
         * @return map of partition identifier to HWPartition objects
         */
        public Map<String, List<HWPartition>> getPartitionMap() {
            return partitionMap;
        }
    }

    /**
     * Parses the {@code Win32_DiskDrive} MediaType string into a disk type.
     * <p>
     * This property is mapped from {@code DISK_GEOMETRY.MediaType} and describes only removability and form factor; its
     * entire value domain is "Fixed hard disk media", "Removable media other than floppy", "External hard disk media"
     * and "Format is unknown". It therefore cannot distinguish an SSD from an HDD, and a fixed or external disk is
     * reported as {@code Unknown} rather than guessed at. Use {@link #classifyDiskType(StorageProperties, String)} to
     * reach the media technology.
     *
     * @param mediaType the WMI MediaType value from {@code Win32_DiskDrive}
     * @return {@code Removable} for removable media, otherwise {@code Unknown}
     */
    protected static String parseWindowsMediaType(@Nullable String mediaType) {
        if (mediaType != null && mediaType.toLowerCase(Locale.ROOT).contains("removable")) {
            return DISK_TYPE_REMOVABLE;
        }
        return DISK_TYPE_UNKNOWN;
    }

    /**
     * Classifies a disk from the storage driver's response, falling back to the WMI media type string when the driver
     * did not answer.
     *
     * @param props     the storage properties read from the device, or {@code null} if the device could not be opened
     *                  or did not answer
     * @param mediaType the WMI MediaType value from {@code Win32_DiskDrive}
     * @return {@code SSD}, {@code HDD}, {@code Removable}, {@code Virtual}, or {@code Unknown}
     */
    protected static String classifyDiskType(@Nullable StorageProperties props, @Nullable String mediaType) {
        if (props == null) {
            // The device did not answer at all, so the WMI string is the only source left. It identifies
            // removable media and nothing else.
            return parseWindowsMediaType(mediaType);
        }
        // Checked before the media technology to match the Linux implementation, where a removable
        // device's rotational attribute is likewise not consulted.
        if (props.isRemovableMedia()) {
            return DISK_TYPE_REMOVABLE;
        }
        if (props.getBusType() == BUS_TYPE_FILE_BACKED_VIRTUAL || props.getBusType() == BUS_TYPE_SPACES) {
            return DISK_TYPE_VIRTUAL;
        }
        Boolean seekPenalty = props.getIncursSeekPenalty();
        if (seekPenalty != null) {
            // A device with no seek penalty is solid state. A RAM disk also answers no here, which
            // matches what Linux reports for one from queue/rotational.
            return seekPenalty.booleanValue() ? DISK_TYPE_HDD : DISK_TYPE_SSD;
        }
        // The descriptor was read but the driver does not implement the seek penalty property. The WMI
        // string is not consulted here: the descriptor already answered the only question it can.
        return DISK_TYPE_UNKNOWN;
    }

    /**
     * Converts an {@code MSFT_PhysicalDisk} MediaType, and the SpindleSpeed that accompanies it, into a disk type.
     *
     * @param mediaType    the MediaType value: 3 is HDD, 4 is SSD, 5 is storage class memory, 0 is unspecified
     * @param spindleSpeed the SpindleSpeed in RPM, as an unsigned value. {@code UINT32_MAX} means rotating at an
     *                     unknown speed.
     * @return {@code SSD}, {@code HDD}, or {@code Unknown}
     */
    protected static String diskTypeFromMsftMediaType(int mediaType, long spindleSpeed) {
        switch (mediaType) {
            case MSFT_MEDIA_TYPE_HDD:
                return DISK_TYPE_HDD;
            case MSFT_MEDIA_TYPE_SSD:
            case MSFT_MEDIA_TYPE_SCM:
                return DISK_TYPE_SSD;
            default:
                // For an unspecified MediaType a nonzero SpindleSpeed still identifies rotating media. A zero
                // is not read as solid state, because that is also what an absent property reads as.
                return spindleSpeed > 0L ? DISK_TYPE_HDD : DISK_TYPE_UNKNOWN;
        }
    }

    /**
     * Maps each {@code MSFT_PhysicalDisk} DeviceId to a disk type. The DeviceId of a directly attached disk is its
     * physical drive number, matching {@code Win32_DiskDrive}'s Index.
     *
     * @param pd the physical disk query result
     * @return a map of DeviceId to disk type, omitting disks whose type could not be determined
     */
    protected static Map<String, String> buildMsftDiskTypeMap(WmiResult<PhysicalDiskMediaTypeProperty> pd) {
        Map<String, String> diskTypeMap = new HashMap<>();
        for (int i = 0; i < pd.getResultCount(); i++) {
            String deviceId = WmiUtil.getString(pd, PhysicalDiskMediaTypeProperty.DEVICEID, i);
            String diskType = diskTypeFromMsftMediaType(
                    WmiUtil.getUint16(pd, PhysicalDiskMediaTypeProperty.MEDIATYPE, i),
                    WmiUtil.getUint32asLong(pd, PhysicalDiskMediaTypeProperty.SPINDLESPEED, i));
            if (!deviceId.isEmpty() && !DISK_TYPE_UNKNOWN.equals(diskType)) {
                diskTypeMap.put(deviceId, diskType);
            }
        }
        return diskTypeMap;
    }

    /**
     * Classifies every disk in a {@code Win32_DiskDrive} query result from the storage driver.
     *
     * @param vals     the disk drive query result
     * @param resolver reads a physical drive's storage properties, which requires the binding's native calls
     * @return a disk type for each result, in query order
     */
    protected static String[] classifyDiskTypes(WmiResult<DiskDriveProperty> vals, StoragePropertiesResolver resolver) {
        int count = vals.getResultCount();
        String[] diskTypes = new String[count];
        for (int i = 0; i < count; i++) {
            diskTypes[i] = classifyDiskType(resolver.resolve(WmiUtil.getString(vals, DiskDriveProperty.NAME, i)),
                    WmiUtil.getString(vals, DiskDriveProperty.MEDIATYPE, i));
        }
        return diskTypes;
    }

    /**
     * Tests whether any disk type is still undetermined, and so whether the Storage namespace fallback is worth its
     * cost. On hardware whose driver answers the seek penalty query this is {@code false} and the fallback is skipped.
     *
     * @param diskTypes the disk types from {@link #classifyDiskTypes(WmiResult, StoragePropertiesResolver)}
     * @return {@code true} if at least one disk type is {@code Unknown}
     */
    protected static boolean anyDiskTypeUnknown(String[] diskTypes) {
        for (String diskType : diskTypes) {
            if (DISK_TYPE_UNKNOWN.equals(diskType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fills in the disk types the storage driver could not determine from {@code MSFT_PhysicalDisk}. Types the driver
     * did determine are left alone, so a removable or virtual disk is not relabeled by its media technology.
     *
     * @param diskTypes the disk types to update in place
     * @param vals      the disk drive query result the types were derived from
     * @param pd        the physical disk media type query result
     */
    protected static void fillUnknownDiskTypes(String[] diskTypes, WmiResult<DiskDriveProperty> vals,
            WmiResult<PhysicalDiskMediaTypeProperty> pd) {
        Map<String, String> diskTypeMap = buildMsftDiskTypeMap(pd);
        if (diskTypeMap.isEmpty()) {
            return;
        }
        for (int i = 0; i < diskTypes.length; i++) {
            if (DISK_TYPE_UNKNOWN.equals(diskTypes[i])) {
                // MSFT_PhysicalDisk's DeviceId is the physical drive number, as Win32_DiskDrive's Index is
                String deviceId = Integer.toString(WmiUtil.getUint32(vals, DiskDriveProperty.INDEX, i));
                diskTypes[i] = diskTypeMap.getOrDefault(deviceId, DISK_TYPE_UNKNOWN);
            }
        }
    }

    /**
     * Reads a physical drive's storage properties. Implemented by each binding, which supplies the native calls.
     */
    @FunctionalInterface
    protected interface StoragePropertiesResolver {
        /**
         * Reads the storage properties of the named device.
         *
         * @param name the device path, normally {@code \\.\PHYSICALDRIVE<n>}
         * @return the storage properties, or {@code null} if the device could not be opened or did not answer
         */
        @Nullable
        StorageProperties resolve(String name);
    }

    /**
     * The subset of {@code IOCTL_STORAGE_QUERY_PROPERTY} results used to classify a disk.
     */
    protected static final class StorageProperties {
        private final int busType;
        private final boolean removableMedia;
        private final @Nullable Boolean incursSeekPenalty;

        /**
         * Creates a StorageProperties instance.
         *
         * @param busType           the {@code STORAGE_BUS_TYPE} of the device
         * @param removableMedia    whether the device holds removable media
         * @param incursSeekPenalty whether the device incurs a seek penalty, or {@code null} if the storage driver does
         *                          not implement the seek penalty property
         */
        public StorageProperties(int busType, boolean removableMedia, @Nullable Boolean incursSeekPenalty) {
            this.busType = busType;
            this.removableMedia = removableMedia;
            this.incursSeekPenalty = incursSeekPenalty;
        }

        /**
         * Gets the bus type.
         *
         * @return the {@code STORAGE_BUS_TYPE} of the device
         */
        public int getBusType() {
            return busType;
        }

        /**
         * Gets whether the media is removable.
         *
         * @return {@code true} if the device holds removable media
         */
        public boolean isRemovableMedia() {
            return removableMedia;
        }

        /**
         * Gets whether the device incurs a seek penalty.
         *
         * @return {@code TRUE} for rotating media, {@code FALSE} for solid state, or {@code null} if the storage driver
         *         does not implement the seek penalty property
         */
        public @Nullable Boolean getIncursSeekPenalty() {
            return incursSeekPenalty;
        }
    }
}
