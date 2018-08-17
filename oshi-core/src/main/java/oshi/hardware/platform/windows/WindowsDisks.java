/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32; // NOSONAR squid:S1191

import oshi.hardware.Disks;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.jna.platform.windows.PdhUtil;
import oshi.jna.platform.windows.PdhUtil.PdhEnumObjectItems;
import oshi.jna.platform.windows.PdhUtil.PdhException;
import oshi.jna.platform.windows.WbemcliUtil;
import oshi.jna.platform.windows.WbemcliUtil.WmiQuery;
import oshi.jna.platform.windows.WbemcliUtil.WmiResult;
import oshi.util.MapUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.platform.windows.WmiUtil;

/**
 * Windows hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class WindowsDisks implements Disks {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsDisks.class);

    /**
     * Maps to store read/write bytes per drive index
     */
    private static Map<String, Long> readMap = new HashMap<>();
    private static Map<String, Long> readByteMap = new HashMap<>();
    private static Map<String, Long> writeMap = new HashMap<>();
    private static Map<String, Long> writeByteMap = new HashMap<>();
    private static Map<String, Long> xferTimeMap = new HashMap<>();
    private static Map<String, Long> timeStampMap = new HashMap<>();
    private static Map<String, List<String>> driveToPartitionMap = new HashMap<>();
    private static Map<String, String> partitionToLogicalDriveMap = new HashMap<>();
    private static Map<String, HWPartition> partitionMap = new HashMap<>();

    private static final String PHYSICALDRIVE_PREFIX = "\\\\.\\PHYSICALDRIVE";
    private static final String PHYSICAL_DISK = "PhysicalDisk";
    private static final String PHYSICAL_DISK_LOCALIZED = PdhUtil.PdhLookupPerfNameByIndex(null,
            PdhUtil.PdhLookupPerfIndexByEnglishName(PHYSICAL_DISK));
    private static final String TOTAL_INSTANCE = "_Total";

    private static final Pattern DEVICE_ID = Pattern.compile(".*\\.DeviceID=\"(.*)\"");

    private static final int BUFSIZE = 255;

    enum DiskDriveProperty {
        INDEX, MANUFACTURER, MODEL, NAME, SERIALNUMBER, SIZE;
    }

    private static final WmiQuery<DiskDriveProperty> DISK_DRIVE_QUERY = WbemcliUtil.createQuery("Win32_DiskDrive",
            DiskDriveProperty.class);

    enum DriveToPartitionProperty {
        ANTECEDENT, DEPENDENT;
    }

    private static final WmiQuery<DriveToPartitionProperty> DRIVE_TO_PARTITION_QUERY = WbemcliUtil
            .createQuery("Win32_DiskDriveToDiskPartition", DriveToPartitionProperty.class);
    private static final WmiQuery<DriveToPartitionProperty> DISK_TO_PARTITION_QUERY = WbemcliUtil
            .createQuery("Win32_LogicalDiskToPartition", DriveToPartitionProperty.class);

    enum DiskPartitionProperty {
        DESCRIPTION, DEVICEID, DISKINDEX, INDEX, NAME, SIZE, TYPE;
    }

    private static final WmiQuery<DiskPartitionProperty> PARTITION_QUERY = WbemcliUtil
            .createQuery("Win32_DiskPartition", DiskPartitionProperty.class);

    /*
     * For disk query
     */
    enum PhysicalDiskProperty {
        NAME, DISKREADSPERSEC, DISKREADBYTESPERSEC, DISKWRITESPERSEC, DISKWRITEBYTESPERSEC, PERCENTDISKTIME, TIMESTAMP_SYS100NS;
    }

    // Only one of counter or query will be used
    private static Map<String, PerfCounter> diskReadsCounterMap = new HashMap<>();
    private static Map<String, PerfCounter> diskReadBytesCounterMap = new HashMap<>();
    private static Map<String, PerfCounter> diskWritesCounterMap = new HashMap<>();
    private static Map<String, PerfCounter> diskWriteBytesCounterMap = new HashMap<>();
    private static Map<String, PerfCounter> diskXferTimeCounterMap = new HashMap<>();

    private static WmiQuery<PhysicalDiskProperty> physicalDiskQuery = null;

    static {
        String physicalDisk = PdhUtil.PdhLookupPerfNameByIndex(null,
                PdhUtil.PdhLookupPerfIndexByEnglishName(PHYSICAL_DISK));
        boolean enumeration = true;
        try {
            PdhEnumObjectItems objectItems = PdhUtil.PdhEnumObjectItems(null, null, physicalDisk, 100);
            if (!objectItems.getInstances().isEmpty()) {
                List<String> instances = objectItems.getInstances();
                PerfCounter counter;
                for (int i = 0; i < instances.size(); i++) {
                    String instance = instances.get(i);
                    counter = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "Disk Reads/sec");
                    diskReadsCounterMap.put(instance, counter);
                    if (!PerfDataUtil.addCounterToQuery(counter)) {
                        throw new PdhException(0);
                    }

                    counter = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "Disk Read Bytes/sec");
                    diskReadBytesCounterMap.put(instance, counter);
                    if (!PerfDataUtil.addCounterToQuery(counter)) {
                        throw new PdhException(0);
                    }

                    counter = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "Disk Writes/sec");
                    diskWritesCounterMap.put(instance, counter);
                    if (!PerfDataUtil.addCounterToQuery(counter)) {
                        throw new PdhException(0);
                    }

                    counter = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "Disk Write Bytes/sec");
                    diskWriteBytesCounterMap.put(instance, counter);
                    if (!PerfDataUtil.addCounterToQuery(counter)) {
                        throw new PdhException(0);
                    }

                    counter = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "% Disk Time");
                    diskXferTimeCounterMap.put(instance, counter);
                    if (!PerfDataUtil.addCounterToQuery(counter)) {
                        throw new PdhException(0);
                    }
                }
            }
        } catch (PdhException e) {
            LOG.warn("Unable to enumerate performance counter instances for {}.", physicalDisk);
            enumeration = false;
        }
        if (!enumeration) {
            PerfDataUtil.removeAllCounters(PHYSICAL_DISK);
            diskReadsCounterMap = null;
            diskReadBytesCounterMap = null;
            diskWritesCounterMap = null;
            diskWriteBytesCounterMap = null;
            diskXferTimeCounterMap = null;

            physicalDiskQuery = WbemcliUtil.createQuery("Win32_PerfRawData_PerfDisk_PhysicalDisk",
                    PhysicalDiskProperty.class);
        }
    }

    public static boolean updateDiskStats(HWDiskStore diskStore) {
        String index = null;
        HWPartition[] partitions = diskStore.getPartitions();
        if (partitions.length > 0) {
            // If a partition exists on this drive, the major property
            // corresponds to the disk index, so use it.
            index = Integer.toString(partitions[0].getMajor());
        } else if (diskStore.getName().startsWith(PHYSICALDRIVE_PREFIX)) {
            // If no partition exists, Windows reliably uses a name to match the
            // disk index. That said, the skeptical person might wonder why a
            // disk has read/write statistics without a partition, and wonder
            // why this branch is even relevant as an option. The author of this
            // comment does not have an answer for this valid question.
            index = diskStore.getName().substring(PHYSICALDRIVE_PREFIX.length(), diskStore.getName().length());
        } else {
            // The author of this comment cannot fathom a circumstance in which
            // the code reaches this point, but just in case it does, here's the
            // correct response. If you get this log warning, the circumstances
            // would be of great interest to the project's maintainers.
            LOG.warn("Couldn't match index for {}", diskStore.getName());
            return false;
        }
        populateReadWriteMaps(index);
        if (readMap.containsKey(index)) {
            diskStore.setReads(MapUtil.getOrDefault(readMap, index, 0L));
            diskStore.setReadBytes(MapUtil.getOrDefault(readByteMap, index, 0L));
            diskStore.setWrites(MapUtil.getOrDefault(writeMap, index, 0L));
            diskStore.setWriteBytes(MapUtil.getOrDefault(writeByteMap, index, 0L));
            diskStore.setTransferTime(MapUtil.getOrDefault(xferTimeMap, index, 0L));
            diskStore.setTimeStamp(MapUtil.getOrDefault(timeStampMap, index, 0L));
            return true;
        } else {
            return false;
        }

    }

    @Override
    public HWDiskStore[] getDisks() {
        List<HWDiskStore> result;
        result = new ArrayList<>();
        populateReadWriteMaps(null);
        populatePartitionMaps();

        WmiResult<DiskDriveProperty> vals = WmiUtil.queryWMI(DISK_DRIVE_QUERY);

        for (int i = 0; i < vals.getResultCount(); i++) {
            HWDiskStore ds = new HWDiskStore();
            ds.setName(vals.getString(DiskDriveProperty.NAME, i));
            ds.setModel(String.format("%s %s", vals.getString(DiskDriveProperty.MODEL, i),
                    vals.getString(DiskDriveProperty.MANUFACTURER, i)).trim());
            // Most vendors store serial # as a hex string; convert
            ds.setSerial(ParseUtil.hexStringToString(vals.getString(DiskDriveProperty.SERIALNUMBER, i)));
            String index = vals.getInteger(DiskDriveProperty.INDEX, i).toString();
            ds.setReads(MapUtil.getOrDefault(readMap, index, 0L));
            ds.setReadBytes(MapUtil.getOrDefault(readByteMap, index, 0L));
            ds.setWrites(MapUtil.getOrDefault(writeMap, index, 0L));
            ds.setWriteBytes(MapUtil.getOrDefault(writeByteMap, index, 0L));
            ds.setTransferTime(MapUtil.getOrDefault(xferTimeMap, index, 0L));
            ds.setTimeStamp(MapUtil.getOrDefault(timeStampMap, index, 0L));
            ds.setSize(ParseUtil.parseLongOrDefault(vals.getString(DiskDriveProperty.SIZE, i), 0L));
            // Get partitions
            List<HWPartition> partitions = new ArrayList<>();
            List<String> partList = driveToPartitionMap.get(ds.getName());
            if (partList != null && !partList.isEmpty()) {
                for (String part : partList) {
                    if (partitionMap.containsKey(part)) {
                        partitions.add(partitionMap.get(part));
                    }
                }
            }
            ds.setPartitions(partitions.toArray(new HWPartition[partitions.size()]));
            // Add to list
            result.add(ds);
        }
        return result.toArray(new HWDiskStore[result.size()]);
    }

    /**
     * Populates the maps for the specified index. If the index is null,
     * populates all the maps
     * 
     * @param index
     *            The index to populate/update maps for
     */
    private static void populateReadWriteMaps(String index) {
        // If index is null, start from scratch.
        if (index == null) {
            readMap.clear();
            readByteMap.clear();
            writeMap.clear();
            writeByteMap.clear();
            xferTimeMap.clear();
            timeStampMap.clear();
        }
        // If WMI query is not null, don't use counters
        if (physicalDiskQuery != null) {
            WmiResult<PhysicalDiskProperty> result = WmiUtil.queryWMI(physicalDiskQuery);
            for (int i = 0; i < result.getResultCount(); i++) {
                String name = getIndexFromName(result.getString(PhysicalDiskProperty.NAME, i));
                if ((index != null && !index.equals(name)) || TOTAL_INSTANCE.equals(name)) {
                    continue;
                }
                readMap.put(name, result.getInteger(PhysicalDiskProperty.DISKREADSPERSEC, i).longValue());
                readByteMap.put(name, ParseUtil
                        .parseLongOrDefault(result.getString(PhysicalDiskProperty.DISKREADBYTESPERSEC, i), 0L));
                writeMap.put(name, result.getInteger(PhysicalDiskProperty.DISKWRITESPERSEC, i).longValue());
                writeByteMap.put(name, ParseUtil
                        .parseLongOrDefault(result.getString(PhysicalDiskProperty.DISKWRITEBYTESPERSEC, i), 0L));
                xferTimeMap.put(name,
                        ParseUtil.parseLongOrDefault(result.getString(PhysicalDiskProperty.PERCENTDISKTIME, i), 0L)
                                / 10000L);
                long timestamp = ParseUtil
                        .parseLongOrDefault(result.getString(PhysicalDiskProperty.TIMESTAMP_SYS100NS, i), 0L);
                timeStampMap.put(name,
                        timestamp > 0 ? PerfDataUtil.filetimeToUtcMs(timestamp, false) : System.currentTimeMillis());
            }
            return;
        }
        // Fetch the instance names
        PdhEnumObjectItems objectItems;
        try {
            objectItems = PdhUtil.PdhEnumObjectItems(null, null, PHYSICAL_DISK_LOCALIZED, 100);
        } catch (PdhException e) {
            LOG.error("Unable to enumerate instances for {}.", PHYSICAL_DISK_LOCALIZED);
            return;
        }
        List<String> instances = objectItems.getInstances();
        instances.remove(TOTAL_INSTANCE);

        Set<String> unseenInstances = new HashSet<>(diskReadsCounterMap.keySet());

        for (String instance : instances) {
            unseenInstances.remove(instance);

            // If not in the map, add it
            if (!diskReadsCounterMap.containsKey(instance)) {
                PerfCounter counter = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "Disk Reads/sec");
                diskReadsCounterMap.put(instance, counter);
                if (!PerfDataUtil.addCounterToQuery(counter)) {
                    diskReadsCounterMap.remove(instance);
                }
            }

            if (!diskReadBytesCounterMap.containsKey(instance)) {
                PerfCounter counter = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "Disk Read Bytes/sec");
                diskReadBytesCounterMap.put(instance, counter);
                if (!PerfDataUtil.addCounterToQuery(counter)) {
                    diskReadBytesCounterMap.remove(instance);
                }
            }

            if (!diskWritesCounterMap.containsKey(instance)) {
                PerfCounter counter = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "Disk Writes/sec");
                diskWritesCounterMap.put(instance, counter);
                if (!PerfDataUtil.addCounterToQuery(counter)) {
                    diskWritesCounterMap.remove(instance);
                }
            }

            if (!diskWriteBytesCounterMap.containsKey(instance)) {
                PerfCounter counter = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "Disk Write Bytes/sec");
                diskWriteBytesCounterMap.put(instance, counter);
                if (!PerfDataUtil.addCounterToQuery(counter)) {
                    diskWriteBytesCounterMap.remove(instance);
                }
            }

            if (!diskXferTimeCounterMap.containsKey(instance)) {
                PerfCounter counter = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "% Disk Time");
                diskXferTimeCounterMap.put(instance, counter);
                if (!PerfDataUtil.addCounterToQuery(counter)) {
                    diskXferTimeCounterMap.remove(instance);
                }
            }
        }
        // Update all the counters
        long timestamp = PerfDataUtil.updateQuery(PHYSICAL_DISK);
        // Update the maps
        for (String instance : instances) {
            String name = getIndexFromName(instance);
            if (index != null && !index.equals(name)) {
                continue;
            }
            readMap.put(name, PerfDataUtil.queryCounter(diskReadsCounterMap.get(instance)));
            readByteMap.put(name, PerfDataUtil.queryCounter(diskReadBytesCounterMap.get(instance)));
            writeMap.put(name, PerfDataUtil.queryCounter(diskWritesCounterMap.get(instance)));
            writeByteMap.put(name, PerfDataUtil.queryCounter(diskWriteBytesCounterMap.get(instance)));
            xferTimeMap.put(name, PerfDataUtil.queryCounter(diskXferTimeCounterMap.get(instance)) / 10000L);
            timeStampMap.put(name, timestamp);
        }
        // We've added any new counters; now remove old ones
        for (String instance : unseenInstances) {
            PerfCounter counter = diskReadsCounterMap.get(instance);
            PerfDataUtil.removeCounterFromQuery(counter);

            counter = diskReadBytesCounterMap.get(instance);
            PerfDataUtil.removeCounterFromQuery(counter);

            counter = diskWritesCounterMap.get(instance);
            PerfDataUtil.removeCounterFromQuery(counter);

            counter = diskWriteBytesCounterMap.get(instance);
            PerfDataUtil.removeCounterFromQuery(counter);

            counter = diskXferTimeCounterMap.get(instance);
            PerfDataUtil.removeCounterFromQuery(counter);
        }
    }

    private void populatePartitionMaps() {
        driveToPartitionMap.clear();
        partitionToLogicalDriveMap.clear();
        partitionMap.clear();
        // For Regexp matching DeviceIDs
        Matcher mAnt;
        Matcher mDep;

        // Map drives to partitions
        WmiResult<DriveToPartitionProperty> drivePartitionMap = WmiUtil.queryWMI(DRIVE_TO_PARTITION_QUERY);
        for (int i = 0; i < drivePartitionMap.getResultCount(); i++) {
            mAnt = DEVICE_ID.matcher(drivePartitionMap.getString(DriveToPartitionProperty.ANTECEDENT, i));
            mDep = DEVICE_ID.matcher(drivePartitionMap.getString(DriveToPartitionProperty.DEPENDENT, i));
            if (mAnt.matches() && mDep.matches()) {
                MapUtil.createNewListIfAbsent(driveToPartitionMap, mAnt.group(1).replaceAll("\\\\\\\\", "\\\\"))
                        .add(mDep.group(1));
            }
        }

        // Map partitions to logical disks
        WmiResult<DriveToPartitionProperty> diskPartitionMap = WmiUtil.queryWMI(DISK_TO_PARTITION_QUERY);
        for (int i = 0; i < diskPartitionMap.getResultCount(); i++) {
            mAnt = DEVICE_ID.matcher(diskPartitionMap.getString(DriveToPartitionProperty.ANTECEDENT, i));
            mDep = DEVICE_ID.matcher(diskPartitionMap.getString(DriveToPartitionProperty.DEPENDENT, i));
            if (mAnt.matches() && mDep.matches()) {
                partitionToLogicalDriveMap.put(mAnt.group(1), mDep.group(1) + "\\");
            }
        }

        // Next, get all partitions and create objects
        WmiResult<DiskPartitionProperty> hwPartitionQueryMap = WmiUtil.queryWMI(PARTITION_QUERY);
        for (int i = 0; i < hwPartitionQueryMap.getResultCount(); i++) {
            String deviceID = hwPartitionQueryMap.getString(DiskPartitionProperty.DEVICEID, i);
            String logicalDrive = MapUtil.getOrDefault(partitionToLogicalDriveMap, deviceID, "");
            String uuid = "";
            if (!logicalDrive.isEmpty()) {
                // Get matching volume for UUID
                char[] volumeChr = new char[BUFSIZE];
                Kernel32.INSTANCE.GetVolumeNameForVolumeMountPoint(logicalDrive, volumeChr, BUFSIZE);
                uuid = ParseUtil.parseUuidOrDefault(new String(volumeChr).trim(), "");
            }
            partitionMap.put(deviceID,
                    new HWPartition(hwPartitionQueryMap.getString(DiskPartitionProperty.NAME, i),
                            hwPartitionQueryMap.getString(DiskPartitionProperty.TYPE, i),
                            hwPartitionQueryMap.getString(DiskPartitionProperty.DESCRIPTION, i), uuid,
                            ParseUtil.parseLongOrDefault(hwPartitionQueryMap.getString(DiskPartitionProperty.SIZE, i),
                                    0L),
                            hwPartitionQueryMap.getInteger(DiskPartitionProperty.DISKINDEX, i),
                            hwPartitionQueryMap.getInteger(DiskPartitionProperty.INDEX, i), logicalDrive));
        }
    }

    /**
     * Parse a drive name like "0 C:" to just the index "0"
     * 
     * @param string
     *            A drive name to parse
     * @return The first space-delimited value
     */
    private static String getIndexFromName(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return s.split("\\s")[0];
    }
}
