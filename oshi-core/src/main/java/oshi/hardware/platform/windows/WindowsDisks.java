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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native; // NOSONAR
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;

import oshi.hardware.Disks;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.jna.platform.windows.Pdh;
import oshi.util.MapUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.PdhUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;

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

    private static final Pattern DEVICE_ID = Pattern.compile(".*\\.DeviceID=\"(.*)\"");

    private static final int BUFSIZE = 255;

    private static final String DISK_DRIVE_CLASS = "Win32_DiskDrive";

    enum WmiProperty {
        ANTECEDENT(ValueType.STRING), //
        DEPENDENT(ValueType.STRING), //
        DESCRIPTION(ValueType.STRING), //
        DEVICEID(ValueType.STRING), //
        DISKINDEX(ValueType.UINT32), //
        INDEX(ValueType.UINT32), //
        MANUFACTURER(ValueType.STRING), //
        MODEL(ValueType.STRING), //
        NAME(ValueType.STRING), //
        SERIALNUMBER(ValueType.STRING), //
        SIZE(ValueType.UINT64), //
        TYPE(ValueType.STRING);

        private ValueType type;

        public ValueType getType() {
            return this.type;
        }

        WmiProperty(ValueType type) {
            this.type = type;
        }
    }

    // Win32_DiskDrive
    private static final WmiProperty[] DRIVE_PROPERTIES = new WmiProperty[] { WmiProperty.NAME,
            WmiProperty.MANUFACTURER, WmiProperty.MODEL, WmiProperty.SERIALNUMBER, WmiProperty.SIZE,
            WmiProperty.INDEX };
    private static final String[] DRIVE_STRINGS = new String[DRIVE_PROPERTIES.length];
    static {
        for (int i = 0; i < DRIVE_PROPERTIES.length; i++) {
            DRIVE_STRINGS[i] = DRIVE_PROPERTIES[i].name();
        }
    }
    private static final ValueType[] DRIVE_TYPES = new ValueType[DRIVE_PROPERTIES.length];
    static {
        for (int i = 0; i < DRIVE_PROPERTIES.length; i++) {
            DRIVE_TYPES[i] = DRIVE_PROPERTIES[i].getType();
        }
    }

    // Win32_DiskDriveToDiskPartition and Win32_LogicalDiskToPartition
    private static final WmiProperty[] DISK_TO_PARTITION_PROPERTIES = new WmiProperty[] {
            WmiProperty.ANTECEDENT, WmiProperty.DEPENDENT };
    private static final String[] DISK_TO_PARTITION_STRINGS = new String[DISK_TO_PARTITION_PROPERTIES.length];
    static {
        for (int i = 0; i < DISK_TO_PARTITION_PROPERTIES.length; i++) {
            DISK_TO_PARTITION_STRINGS[i] = DISK_TO_PARTITION_PROPERTIES[i].name();
        }
    }
    private static final ValueType[] DISK_TO_PARTITION_TYPES = new ValueType[DISK_TO_PARTITION_PROPERTIES.length];
    static {
        for (int i = 0; i < DISK_TO_PARTITION_PROPERTIES.length; i++) {
            DISK_TO_PARTITION_TYPES[i] = DISK_TO_PARTITION_PROPERTIES[i].getType();
        }
    }

    // Win32_Paritition
    private static final WmiProperty[] PARTITION_PROPERTIES = new WmiProperty[] { WmiProperty.NAME,
            WmiProperty.TYPE, WmiProperty.DESCRIPTION, WmiProperty.DEVICEID, WmiProperty.SIZE,
            WmiProperty.DISKINDEX, WmiProperty.INDEX };
    private static final String[] PARTITION_STRINGS = new String[PARTITION_PROPERTIES.length];
    static {
        for (int i = 0; i < PARTITION_PROPERTIES.length; i++) {
            PARTITION_STRINGS[i] = PARTITION_PROPERTIES[i].name();
        }
    }
    private static final ValueType[] PARTITION_TYPES = new ValueType[PARTITION_PROPERTIES.length];
    static {
        for (int i = 0; i < PARTITION_PROPERTIES.length; i++) {
            PARTITION_TYPES[i] = PARTITION_PROPERTIES[i].getType();
        }
    }

    private static final String PDH_DISK_READS_FORMAT = "\\PhysicalDisk(%s)\\Disk Reads/sec";
    private static final String PDH_DISK_READ_BYTES_FORMAT = "\\PhysicalDisk(%s)\\Disk Read Bytes/sec";
    private static final String PDH_DISK_WRITES_FORMAT = "\\PhysicalDisk(%s)\\Disk Writes/sec";
    private static final String PDH_DISK_WRITE_BYTES_FORMAT = "\\PhysicalDisk(%s)\\Disk Write Bytes/sec";
    private static final String PDH_DISK_TIME_FORMAT = "\\PhysicalDisk(%s)\\%% Disk Time";

    private static final String PHYSICALDRIVE_PREFIX = "\\\\.\\PHYSICALDRIVE";

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

        Map<String, List<Object>> vals = WmiUtil.selectObjectsFrom(null, DISK_DRIVE_CLASS, DRIVE_STRINGS, null,
                DRIVE_TYPES);
        for (int i = 0; i < vals.get(WmiProperty.NAME.name()).size(); i++) {
            HWDiskStore ds = new HWDiskStore();
            ds.setName((String) vals.get(WmiProperty.NAME.name()).get(i));
            ds.setModel(String
                    .format("%s %s", vals.get(WmiProperty.MODEL.name()).get(i),
                            vals.get(WmiProperty.MANUFACTURER.name()).get(i))
                    .trim());
            // Most vendors store serial # as a hex string; convert
            ds.setSerial(ParseUtil.hexStringToString((String) vals.get(WmiProperty.SERIALNUMBER.name()).get(i)));
            String index = vals.get(WmiProperty.INDEX.name()).get(i).toString();
            ds.setReads(MapUtil.getOrDefault(readMap, index, 0L));
            ds.setReadBytes(MapUtil.getOrDefault(readByteMap, index, 0L));
            ds.setWrites(MapUtil.getOrDefault(writeMap, index, 0L));
            ds.setWriteBytes(MapUtil.getOrDefault(writeByteMap, index, 0L));
            ds.setTransferTime(MapUtil.getOrDefault(xferTimeMap, index, 0L));
            ds.setTimeStamp(MapUtil.getOrDefault(timeStampMap, index, 0L));
            ds.setSize((Long) vals.get(WmiProperty.SIZE.name()).get(i));
            // Get partitions
            List<HWPartition> partitions = new ArrayList<>();
            List<String> partList = driveToPartitionMap.get(ds.getName());
            if (partList != null && !partList.isEmpty()) {
                for (String part : partList) {
                    partitions.add(partitionMap.get(part));
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
        // Fetch the instance names
        // Call once to get string lengths
        DWORDByReference pcchCounterListLength = new DWORDByReference(new DWORD(0));
        DWORDByReference pcchInstanceListLength = new DWORDByReference(new DWORD(0));
        Pdh.INSTANCE.PdhEnumObjectItems(null, null, "PhysicalDisk", null, pcchCounterListLength, null,
                pcchInstanceListLength, 100, 0);
        // Allocate memory and call again to populate strings
        char[] mszCounterList = new char[pcchCounterListLength.getValue().intValue()];
        char[] mszInstanceList = new char[pcchInstanceListLength.getValue().intValue()];
        Pdh.INSTANCE.PdhEnumObjectItems(null, null, "PhysicalDisk", mszCounterList, pcchCounterListLength,
                mszInstanceList, pcchInstanceListLength, 100, 0);
        List<String> instances = Native.toStringList(mszInstanceList);
        instances.remove("_Total");
        // At this point we have a list of strings that PDH understands. Fetch
        // the counters.
        // Although the field names say "PerSec" this is the Raw Data/counters
        // from which the associated fields are populated in the Formatted Data
        for (String i : instances) {
            String name = ParseUtil.whitespaces.split(i)[0];
            String readString = String.format(PDH_DISK_READS_FORMAT, i);
            if (!PdhUtil.isCounter(readString)) {
                PdhUtil.addCounter(readString);
            }
            String readBytesString = String.format(PDH_DISK_READ_BYTES_FORMAT, i);
            if (!PdhUtil.isCounter(readBytesString)) {
                PdhUtil.addCounter(readBytesString);
            }
            String writeString = String.format(PDH_DISK_WRITES_FORMAT, i);
            if (!PdhUtil.isCounter(writeString)) {
                PdhUtil.addCounter(writeString);
            }
            String writeBytesString = String.format(PDH_DISK_WRITE_BYTES_FORMAT, i);
            if (!PdhUtil.isCounter(writeBytesString)) {
                PdhUtil.addCounter(writeBytesString);
            }
            String xferTimeString = String.format(PDH_DISK_TIME_FORMAT, i);
            if (!PdhUtil.isCounter(xferTimeString)) {
                PdhUtil.addCounter(xferTimeString);
            }
            readMap.put(name, PdhUtil.queryCounter(readString));
            readByteMap.put(name, PdhUtil.queryCounter(readBytesString));
            writeMap.put(name, PdhUtil.queryCounter(writeString));
            writeByteMap.put(name, PdhUtil.queryCounter(writeBytesString));
            xferTimeMap.put(name, PdhUtil.queryCounter(xferTimeString) / 10000L);
            timeStampMap.put(name, PdhUtil.queryCounterTimestamp(xferTimeString));
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
        Map<String, List<Object>> partitionQueryMap = WmiUtil.selectObjectsFrom(null, "Win32_DiskDriveToDiskPartition",
                DISK_TO_PARTITION_STRINGS, null, DISK_TO_PARTITION_TYPES);
        for (int i = 0; i < partitionQueryMap.get(WmiProperty.ANTECEDENT.name()).size(); i++) {
            mAnt = DEVICE_ID.matcher((String) partitionQueryMap.get(WmiProperty.ANTECEDENT.name()).get(i));
            mDep = DEVICE_ID.matcher((String) partitionQueryMap.get(WmiProperty.DEPENDENT.name()).get(i));
            if (mAnt.matches() && mDep.matches()) {
                MapUtil.createNewListIfAbsent(driveToPartitionMap, mAnt.group(1).replaceAll("\\\\\\\\", "\\\\"))
                        .add(mDep.group(1));
            }
        }

        // Map partitions to logical disks
        partitionQueryMap = WmiUtil.selectObjectsFrom(null, "Win32_LogicalDiskToPartition",
                DISK_TO_PARTITION_STRINGS, null, DISK_TO_PARTITION_TYPES);
        for (int i = 0; i < partitionQueryMap.get(WmiProperty.ANTECEDENT.name()).size(); i++) {
            mAnt = DEVICE_ID.matcher((String) partitionQueryMap.get(WmiProperty.ANTECEDENT.name()).get(i));
            mDep = DEVICE_ID.matcher((String) partitionQueryMap.get(WmiProperty.DEPENDENT.name()).get(i));
            if (mAnt.matches() && mDep.matches()) {
                partitionToLogicalDriveMap.put(mAnt.group(1), mDep.group(1) + "\\");
            }
        }

        // Next, get all partitions and create objects
        final Map<String, List<Object>> hwPartitionQueryMap = WmiUtil.selectObjectsFrom(null, "Win32_DiskPartition",
                PARTITION_STRINGS, null, PARTITION_TYPES);
        for (int i = 0; i < hwPartitionQueryMap.get(WmiProperty.NAME.name()).size(); i++) {
            String deviceID = (String) hwPartitionQueryMap.get(WmiProperty.DEVICEID.name()).get(i);
            String logicalDrive = MapUtil.getOrDefault(partitionToLogicalDriveMap, deviceID, "");
            String uuid = "";
            if (!logicalDrive.isEmpty()) {
                // Get matching volume for UUID
                char[] volumeChr = new char[BUFSIZE];
                Kernel32.INSTANCE.GetVolumeNameForVolumeMountPoint(logicalDrive, volumeChr, BUFSIZE);
                uuid = ParseUtil.parseUuidOrDefault(new String(volumeChr).trim(), "");
            }
            partitionMap
                    .put(deviceID,
                            new HWPartition(
                                    (String) hwPartitionQueryMap
                                            .get(WmiProperty.NAME.name()).get(
                                                    i),
                                    (String) hwPartitionQueryMap.get(WmiProperty.TYPE.name()).get(i),
                                    (String) hwPartitionQueryMap.get(WmiProperty.DESCRIPTION.name()).get(i), uuid,
                                    (Long) hwPartitionQueryMap.get(WmiProperty.SIZE.name()).get(i),
                                    ((Long) hwPartitionQueryMap.get(WmiProperty.DISKINDEX.name()).get(i)).intValue(),
                                    ((Long) hwPartitionQueryMap.get(WmiProperty.INDEX.name()).get(i)).intValue(),
                                    logicalDrive));
        }
    }
}
