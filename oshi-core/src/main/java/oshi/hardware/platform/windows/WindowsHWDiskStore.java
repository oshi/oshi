/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.perfmon.PhysicalDisk;
import oshi.driver.windows.perfmon.PhysicalDisk.PhysicalDiskProperty;
import oshi.driver.windows.wmi.Win32DiskDrive;
import oshi.driver.windows.wmi.Win32DiskDrive.DiskDriveProperty;
import oshi.driver.windows.wmi.Win32DiskDriveToDiskPartition;
import oshi.driver.windows.wmi.Win32DiskDriveToDiskPartition.DriveToPartitionProperty;
import oshi.driver.windows.wmi.Win32DiskPartition;
import oshi.driver.windows.wmi.Win32DiskPartition.DiskPartitionProperty;
import oshi.driver.windows.wmi.Win32LogicalDiskToPartition;
import oshi.driver.windows.wmi.Win32LogicalDiskToPartition.DiskToPartitionProperty;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;

/**
 * Windows hard disk implementation.
 */
@ThreadSafe
public final class WindowsHWDiskStore extends AbstractHWDiskStore {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsHWDiskStore.class);

    private static final String PHYSICALDRIVE_PREFIX = "\\\\.\\PHYSICALDRIVE";
    private static final Pattern DEVICE_ID = Pattern.compile(".*\\.DeviceID=\"(.*)\"");

    // A reasonable size for the buffer to accommodate the largest possible volume
    // GUID path is 50 characters.
    private static final int GUID_BUFSIZE = 100;

    private long reads = 0L;
    private long readBytes = 0L;
    private long writes = 0L;
    private long writeBytes = 0L;
    private long currentQueueLength = 0L;
    private long transferTime = 0L;
    private long timeStamp = 0L;
    private List<HWPartition> partitionList;

    private WindowsHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    @Override
    public long getReads() {
        return reads;
    }

    @Override
    public long getReadBytes() {
        return readBytes;
    }

    @Override
    public long getWrites() {
        return writes;
    }

    @Override
    public long getWriteBytes() {
        return writeBytes;
    }

    @Override
    public long getCurrentQueueLength() {
        return currentQueueLength;
    }

    @Override
    public long getTransferTime() {
        return transferTime;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public List<HWPartition> getPartitions() {
        return this.partitionList;
    }

    @Override
    public boolean updateAttributes() {
        String index = null;
        List<HWPartition> partitions = getPartitions();
        if (!partitions.isEmpty()) {
            // If a partition exists on this drive, the major property
            // corresponds to the disk index, so use it.
            index = Integer.toString(partitions.get(0).getMajor());
        } else if (getName().startsWith(PHYSICALDRIVE_PREFIX)) {
            // If no partition exists, Windows reliably uses a name to match the
            // disk index. That said, the skeptical person might wonder why a
            // disk has read/write statistics without a partition, and wonder
            // why this branch is even relevant as an option. The author of this
            // comment does not have an answer for this valid question.
            index = getName().substring(PHYSICALDRIVE_PREFIX.length(), getName().length());
        } else {
            // The author of this comment cannot fathom a circumstance in which
            // the code reaches this point, but just in case it does, here's the
            // correct response. If you get this log warning, the circumstances
            // would be of great interest to the project's maintainers.
            LOG.warn("Couldn't match index for {}", getName());
            return false;
        }
        DiskStats stats = queryReadWriteStats(index);
        if (stats.readMap.containsKey(index)) {
            this.reads = stats.readMap.getOrDefault(index, 0L);
            this.readBytes = stats.readByteMap.getOrDefault(index, 0L);
            this.writes = stats.writeMap.getOrDefault(index, 0L);
            this.writeBytes = stats.writeByteMap.getOrDefault(index, 0L);
            this.currentQueueLength = stats.queueLengthMap.getOrDefault(index, 0L);
            this.transferTime = stats.diskTimeMap.getOrDefault(index, 0L);
            this.timeStamp = stats.timeStamp;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the disks on this machine
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        WmiQueryHandler h = Objects.requireNonNull(WmiQueryHandler.createInstance());
        boolean comInit = false;
        try {
            comInit = h.initCOM();
            List<HWDiskStore> result;
            result = new ArrayList<>();
            DiskStats stats = queryReadWriteStats(null);
            PartitionMaps maps = queryPartitionMaps(h);

            WmiResult<DiskDriveProperty> vals = Win32DiskDrive.queryDiskDrive(h);
            for (int i = 0; i < vals.getResultCount(); i++) {
                WindowsHWDiskStore ds = new WindowsHWDiskStore(WmiUtil.getString(vals, DiskDriveProperty.NAME, i),
                        String.format("%s %s", WmiUtil.getString(vals, DiskDriveProperty.MODEL, i),
                                WmiUtil.getString(vals, DiskDriveProperty.MANUFACTURER, i)).trim(),
                        // Most vendors store serial # as a hex string; convert
                        ParseUtil.hexStringToString(WmiUtil.getString(vals, DiskDriveProperty.SERIALNUMBER, i)),
                        WmiUtil.getUint64(vals, DiskDriveProperty.SIZE, i));

                String index = Integer.toString(WmiUtil.getUint32(vals, DiskDriveProperty.INDEX, i));
                ds.reads = stats.readMap.getOrDefault(index, 0L);
                ds.readBytes = stats.readByteMap.getOrDefault(index, 0L);
                ds.writes = stats.writeMap.getOrDefault(index, 0L);
                ds.writeBytes = stats.writeByteMap.getOrDefault(index, 0L);
                ds.currentQueueLength = stats.queueLengthMap.getOrDefault(index, 0L);
                // DiskTime (sum of readTime+writeTime) slightly overestimates actual transfer
                // time because it includes waiting time in the queue and can exceed 100%.
                // However, alternative calculations require use of a timestamp with 1/64-second
                // resolution producing unacceptable variation in what should be a monotonically
                // increasing counter. See extended discussion and experiments here:
                // https://github.com/oshi/oshi/issues/1504
                ds.transferTime = stats.diskTimeMap.getOrDefault(index, 0L);
                ds.timeStamp = stats.timeStamp;
                // Get partitions
                List<HWPartition> partitions = new ArrayList<>();
                List<String> partList = maps.driveToPartitionMap.get(ds.getName());
                if (partList != null && !partList.isEmpty()) {
                    for (String part : partList) {
                        if (maps.partitionMap.containsKey(part)) {
                            partitions.addAll(maps.partitionMap.get(part));
                        }
                    }
                }
                ds.partitionList = Collections.unmodifiableList(partitions.stream()
                        .sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList()));
                // Add to list
                result.add(ds);
            }
            return result;
        } catch (COMException e) {
            LOG.warn("COM exception: {}", e.getMessage());
            return Collections.emptyList();
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
    }

    /**
     * Gets disk stats for the specified index. If the index is null, populates all
     * the maps
     *
     * @param index
     *            The index to populate/update maps for
     * @return An object encapsulating maps with the stats
     */
    private static DiskStats queryReadWriteStats(String index) {
        // Create object to hold and return results
        DiskStats stats = new DiskStats();
        Pair<List<String>, Map<PhysicalDiskProperty, List<Long>>> instanceValuePair = PhysicalDisk.queryDiskCounters();
        List<String> instances = instanceValuePair.getA();
        Map<PhysicalDiskProperty, List<Long>> valueMap = instanceValuePair.getB();
        stats.timeStamp = System.currentTimeMillis();
        List<Long> readList = valueMap.get(PhysicalDiskProperty.DISKREADSPERSEC);
        List<Long> readByteList = valueMap.get(PhysicalDiskProperty.DISKREADBYTESPERSEC);
        List<Long> writeList = valueMap.get(PhysicalDiskProperty.DISKWRITESPERSEC);
        List<Long> writeByteList = valueMap.get(PhysicalDiskProperty.DISKWRITEBYTESPERSEC);
        List<Long> queueLengthList = valueMap.get(PhysicalDiskProperty.CURRENTDISKQUEUELENGTH);
        List<Long> diskTimeList = valueMap.get(PhysicalDiskProperty.PERCENTDISKTIME);

        if (instances.isEmpty() || readList == null || readByteList == null || writeList == null
                || writeByteList == null || queueLengthList == null || diskTimeList == null) {
            return stats;
        }
        for (int i = 0; i < instances.size(); i++) {
            String name = getIndexFromName(instances.get(i));
            // If index arg passed, only update passed arg
            if (index != null && !index.equals(name)) {
                continue;
            }
            stats.readMap.put(name, readList.get(i));
            stats.readByteMap.put(name, readByteList.get(i));
            stats.writeMap.put(name, writeList.get(i));
            stats.writeByteMap.put(name, writeByteList.get(i));
            stats.queueLengthMap.put(name, queueLengthList.get(i));
            stats.diskTimeMap.put(name, diskTimeList.get(i) / 10_000L);
        }
        return stats;
    }

    private static PartitionMaps queryPartitionMaps(WmiQueryHandler h) {
        // Create object to hold and return results
        PartitionMaps maps = new PartitionMaps();

        // For Regexp matching DeviceIDs
        Matcher mAnt;
        Matcher mDep;

        // Map drives to partitions
        WmiResult<DriveToPartitionProperty> drivePartitionMap = Win32DiskDriveToDiskPartition.queryDriveToPartition(h);
        for (int i = 0; i < drivePartitionMap.getResultCount(); i++) {
            mAnt = DEVICE_ID.matcher(WmiUtil.getRefString(drivePartitionMap, DriveToPartitionProperty.ANTECEDENT, i));
            mDep = DEVICE_ID.matcher(WmiUtil.getRefString(drivePartitionMap, DriveToPartitionProperty.DEPENDENT, i));
            if (mAnt.matches() && mDep.matches()) {
                maps.driveToPartitionMap.computeIfAbsent(mAnt.group(1).replace("\\\\", "\\"), x -> new ArrayList<>())
                        .add(mDep.group(1));
            }
        }

        // Map partitions to logical disks
        WmiResult<DiskToPartitionProperty> diskPartitionMap = Win32LogicalDiskToPartition.queryDiskToPartition(h);
        for (int i = 0; i < diskPartitionMap.getResultCount(); i++) {
            mAnt = DEVICE_ID.matcher(WmiUtil.getRefString(diskPartitionMap, DiskToPartitionProperty.ANTECEDENT, i));
            mDep = DEVICE_ID.matcher(WmiUtil.getRefString(diskPartitionMap, DiskToPartitionProperty.DEPENDENT, i));
            long size = WmiUtil.getUint64(diskPartitionMap, DiskToPartitionProperty.ENDINGADDRESS, i)
                    - WmiUtil.getUint64(diskPartitionMap, DiskToPartitionProperty.STARTINGADDRESS, i) + 1L;
            if (mAnt.matches() && mDep.matches()) {
                if (maps.partitionToLogicalDriveMap.containsKey(mAnt.group(1))) {
                    maps.partitionToLogicalDriveMap.get(mAnt.group(1)).add(new Pair<>(mDep.group(1) + "\\", size));
                } else {
                    List<Pair<String, Long>> list = new ArrayList<>();
                    list.add(new Pair<>(mDep.group(1) + "\\", size));
                    maps.partitionToLogicalDriveMap.put(mAnt.group(1), list);
                }
            }
        }

        // Next, get all partitions and create objects
        WmiResult<DiskPartitionProperty> hwPartitionQueryMap = Win32DiskPartition.queryPartition(h);
        for (int i = 0; i < hwPartitionQueryMap.getResultCount(); i++) {
            String deviceID = WmiUtil.getString(hwPartitionQueryMap, DiskPartitionProperty.DEVICEID, i);
            List<Pair<String, Long>> logicalDrives = maps.partitionToLogicalDriveMap.get(deviceID);
            if (logicalDrives == null) {
                continue;
            }
            for (int j = 0; j < logicalDrives.size(); j++) {
                Pair<String, Long> logicalDrive = logicalDrives.get(j);
                if (logicalDrive != null && !logicalDrive.getA().isEmpty()) {
                    char[] volumeChr = new char[GUID_BUFSIZE];
                    Kernel32.INSTANCE.GetVolumeNameForVolumeMountPoint(logicalDrive.getA(), volumeChr, GUID_BUFSIZE);
                    String uuid = ParseUtil.parseUuidOrDefault(new String(volumeChr).trim(), "");
                    HWPartition pt = new HWPartition(
                            WmiUtil.getString(hwPartitionQueryMap, DiskPartitionProperty.NAME, i),
                            WmiUtil.getString(hwPartitionQueryMap, DiskPartitionProperty.TYPE, i),
                            WmiUtil.getString(hwPartitionQueryMap, DiskPartitionProperty.DESCRIPTION, i), uuid,
                            logicalDrive.getB(),
                            WmiUtil.getUint32(hwPartitionQueryMap, DiskPartitionProperty.DISKINDEX, i),
                            WmiUtil.getUint32(hwPartitionQueryMap, DiskPartitionProperty.INDEX, i),
                            logicalDrive.getA());
                    if (maps.partitionMap.containsKey(deviceID)) {
                        maps.partitionMap.get(deviceID).add(pt);
                    } else {
                        List<HWPartition> ptlist = new ArrayList<>();
                        ptlist.add(pt);
                        maps.partitionMap.put(deviceID, ptlist);
                    }
                }
            }
        }
        return maps;
    }

    /**
     * Parse a drive name like "0 C:" to just the index "0"
     *
     * @param s
     *            A drive name to parse
     * @return The first space-delimited value
     */
    private static String getIndexFromName(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return s.split("\\s")[0];
    }

    /**
     * Maps to store read/write bytes per drive index
     */
    private static final class DiskStats {
        private final Map<String, Long> readMap = new HashMap<>();
        private final Map<String, Long> readByteMap = new HashMap<>();
        private final Map<String, Long> writeMap = new HashMap<>();
        private final Map<String, Long> writeByteMap = new HashMap<>();
        private final Map<String, Long> queueLengthMap = new HashMap<>();
        private final Map<String, Long> diskTimeMap = new HashMap<>();
        private long timeStamp;
    }

    /**
     * Maps for the partition structure
     */
    private static final class PartitionMaps {
        private final Map<String, List<String>> driveToPartitionMap = new HashMap<>();
        private final Map<String, List<Pair<String, Long>>> partitionToLogicalDriveMap = new HashMap<>();
        private final Map<String, List<HWPartition>> partitionMap = new HashMap<>();
    }
}
