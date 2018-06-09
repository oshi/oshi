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

import com.sun.jna.platform.win32.Kernel32;

import oshi.hardware.Disks;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.util.MapUtil;
import oshi.util.ParseUtil;
import oshi.util.StringUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;

/**
 * Windows hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class WindowsDisks implements Disks {

    private static final long serialVersionUID = 1L;

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

    private static final String ANTECEDENT_PROPERTY = "Antecedent";
    private static final String DEPENDENT_PROPERTY = "Dependent";
    private static final String DESCRIPTION_PROPERTY = "Description";
    private static final String DEVICE_ID_PROPERTY = "DeviceID";
    private static final String DISK_INDEX_PROPERTY = "DiskIndex";
    private static final String DISK_READ_BYTES_PROPERTY = "DiskReadBytesPerSec";
    private static final String DISK_READS_PROPERTY = "DiskReadsPerSec";
    private static final String DISK_WRITE_BYTES_PROPERTY = "DiskWriteBytesPerSec";
    private static final String DISK_WRITES_PROPERTY = "DiskWritesPerSec";
    private static final String INDEX_PROPERTY = "Index";
    private static final String MANUFACTURER_PROPERTY = "Manufacturer";
    private static final String MODEL_PROPERTY = "Model";
    private static final String NAME_PROPERTY = "Name";
    private static final String PERCENT_DISK_TIME_PROPERTY = "PercentDiskTime";
    private static final String SERIALNUMBER_PROPERTY = "SerialNumber";
    private static final String SIZE_PROPERTY = "Size";
    private static final String TIMESTAMP_PROPERTY = "Timestamp_Sys100NS";
    private static final String TYPE_PROPERTY = "Type";

    private static final String[] DRIVE_PROPERTIES = new String[] { NAME_PROPERTY, MANUFACTURER_PROPERTY,
            MODEL_PROPERTY, SERIALNUMBER_PROPERTY, SIZE_PROPERTY, INDEX_PROPERTY };
    private static final ValueType[] DRIVE_TYPES = { ValueType.STRING, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.UINT32 };

    private static final String DRIVE_TO_PARTITION_PROPERTIES = StringUtil.join(",",
            new String[] { ANTECEDENT_PROPERTY, DEPENDENT_PROPERTY });
    private static final String LOGICAL_DISK_TO_PARTITION_PROPERTIES = StringUtil.join(",",
            new String[] { ANTECEDENT_PROPERTY, DEPENDENT_PROPERTY });

    private static final String[] READ_WRITE_PROPERTIES = new String[] { NAME_PROPERTY, DISK_READS_PROPERTY,
            DISK_READ_BYTES_PROPERTY, DISK_WRITES_PROPERTY, DISK_WRITE_BYTES_PROPERTY, PERCENT_DISK_TIME_PROPERTY,
            TIMESTAMP_PROPERTY };
    private static final ValueType[] READ_WRITE_TYPES = { ValueType.STRING, ValueType.UINT32, ValueType.STRING,
            ValueType.UINT32, ValueType.STRING, ValueType.STRING, ValueType.STRING };

    private static final String[] PARTITION_PROPERTIES = new String[] { NAME_PROPERTY, TYPE_PROPERTY,
            DESCRIPTION_PROPERTY, DEVICE_ID_PROPERTY, SIZE_PROPERTY, DISK_INDEX_PROPERTY, INDEX_PROPERTY };
    private static final ValueType[] PARTITION_TYPES = { ValueType.STRING, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.UINT32, ValueType.UINT32 };

    public static boolean updateDiskStats(HWDiskStore diskStore) {
        Map<String, List<Object>> vals = WmiUtil.selectObjectsFrom(null, DISK_DRIVE_CLASS, INDEX_PROPERTY,
                "WHERE SerialNumber=" + diskStore.getSerial(), DRIVE_TYPES);

        if (vals.get(INDEX_PROPERTY).isEmpty()) {
            return false;
        }

        String index = vals.get(INDEX_PROPERTY).get(0).toString();
        populateReadWriteMaps();

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
        populateReadWriteMaps();
        populatePartitionMaps();

        Map<String, List<Object>> vals = WmiUtil.selectObjectsFrom(null, DISK_DRIVE_CLASS, DRIVE_PROPERTIES, null,
                DRIVE_TYPES);
        for (int i = 0; i < vals.get(NAME_PROPERTY).size(); i++) {
            HWDiskStore ds = new HWDiskStore();
            ds.setName((String) vals.get(NAME_PROPERTY).get(i));
            ds.setModel(String.format("%s %s", vals.get(MODEL_PROPERTY).get(i), vals.get(MANUFACTURER_PROPERTY).get(i))
                    .trim());
            // Most vendors store serial # as a hex string; convert
            ds.setSerial(ParseUtil.hexStringToString((String) vals.get(SERIALNUMBER_PROPERTY).get(i)));
            String index = vals.get(INDEX_PROPERTY).get(i).toString();
            ds.setReads(MapUtil.getOrDefault(readMap, index, 0L));
            ds.setReadBytes(MapUtil.getOrDefault(readByteMap, index, 0L));
            ds.setWrites(MapUtil.getOrDefault(writeMap, index, 0L));
            ds.setWriteBytes(MapUtil.getOrDefault(writeByteMap, index, 0L));
            ds.setTransferTime(MapUtil.getOrDefault(xferTimeMap, index, 0L));
            ds.setTimeStamp(MapUtil.getOrDefault(timeStampMap, index, 0L));
            // If successful this line is the desired value
            ds.setSize(ParseUtil.parseLongOrDefault((String) vals.get(SIZE_PROPERTY).get(i), 0L));
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
     */
    private static void populateReadWriteMaps() {
        readMap.clear();
        readByteMap.clear();
        writeMap.clear();
        writeByteMap.clear();
        xferTimeMap.clear();
        timeStampMap.clear();
        // Although the field names say "PerSec" this is the Raw Data from which
        // the associated fields are populated in the Formatted Data class, so
        // in fact this is the data we want
        Map<String, List<Object>> vals = WmiUtil.selectObjectsFrom(null, "Win32_PerfRawData_PerfDisk_PhysicalDisk",
                READ_WRITE_PROPERTIES, null, READ_WRITE_TYPES);
        for (int i = 0; i < vals.get(NAME_PROPERTY).size(); i++) {
            String name = ParseUtil.whitespaces.split((String) vals.get(NAME_PROPERTY).get(i))[0];
            readMap.put(name, (long) vals.get("DiskReadsPerSec").get(i));
            readByteMap.put(name, ParseUtil.parseLongOrDefault((String) vals.get("DiskReadBytesPerSec").get(i), 0L));
            writeMap.put(name, (long) vals.get("DiskWritesPerSec").get(i));
            writeByteMap.put(name, ParseUtil.parseLongOrDefault((String) vals.get("DiskWriteBytesPerSec").get(i), 0L));
            // Units are 100-ns, divide to get ms
            xferTimeMap.put(name,
                    ParseUtil.parseLongOrDefault((String) vals.get("PercentDiskTime").get(i), 0L) / 10000L);
            timeStampMap.put(name,
                    ParseUtil.parseLongOrDefault((String) vals.get("Timestamp_Sys100NS").get(i), 0L) / 10000L);
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
        Map<String, List<String>> partitionQueryMap = WmiUtil.selectStringsFrom(null, "Win32_DiskDriveToDiskPartition",
                DRIVE_TO_PARTITION_PROPERTIES, null);
        for (int i = 0; i < partitionQueryMap.get(ANTECEDENT_PROPERTY).size(); i++) {
            mAnt = DEVICE_ID.matcher(partitionQueryMap.get(ANTECEDENT_PROPERTY).get(i));
            mDep = DEVICE_ID.matcher(partitionQueryMap.get(DEPENDENT_PROPERTY).get(i));
            if (mAnt.matches() && mDep.matches()) {
                MapUtil.createNewListIfAbsent(driveToPartitionMap, mAnt.group(1).replaceAll("\\\\\\\\", "\\\\"))
                        .add(mDep.group(1));
            }
        }

        // Map partitions to logical disks
        partitionQueryMap = WmiUtil.selectStringsFrom(null, "Win32_LogicalDiskToPartition",
                LOGICAL_DISK_TO_PARTITION_PROPERTIES, null);
        for (int i = 0; i < partitionQueryMap.get(ANTECEDENT_PROPERTY).size(); i++) {
            mAnt = DEVICE_ID.matcher(partitionQueryMap.get(ANTECEDENT_PROPERTY).get(i));
            mDep = DEVICE_ID.matcher(partitionQueryMap.get(DEPENDENT_PROPERTY).get(i));
            if (mAnt.matches() && mDep.matches()) {
                partitionToLogicalDriveMap.put(mAnt.group(1), mDep.group(1) + "\\");
            }
        }

        // Next, get all partitions and create objects
        final Map<String, List<Object>> hwPartitionQueryMap = WmiUtil.selectObjectsFrom(null, "Win32_DiskPartition",
                PARTITION_PROPERTIES, null, PARTITION_TYPES);
        for (int i = 0; i < hwPartitionQueryMap.get(NAME_PROPERTY).size(); i++) {
            String deviceID = (String) hwPartitionQueryMap.get(DEVICE_ID_PROPERTY).get(i);
            String logicalDrive = MapUtil.getOrDefault(partitionToLogicalDriveMap, deviceID, "");
            String uuid = "";
            if (!logicalDrive.isEmpty()) {
                // Get matching volume for UUID
                char[] volumeChr = new char[BUFSIZE];
                Kernel32.INSTANCE.GetVolumeNameForVolumeMountPoint(logicalDrive, volumeChr, BUFSIZE);
                uuid = ParseUtil.parseUuidOrDefault(new String(volumeChr).trim(), "");
            }
            partitionMap.put(deviceID,
                    new HWPartition((String) hwPartitionQueryMap.get(NAME_PROPERTY).get(i),
                            (String) hwPartitionQueryMap.get(TYPE_PROPERTY).get(i),
                            (String) hwPartitionQueryMap.get(DESCRIPTION_PROPERTY).get(i), uuid,
                            ParseUtil.parseLongOrDefault((String) hwPartitionQueryMap.get(SIZE_PROPERTY).get(i), 0L),
                            ((Long) hwPartitionQueryMap.get(DISK_INDEX_PROPERTY).get(i)).intValue(),
                            ((Long) hwPartitionQueryMap.get(INDEX_PROPERTY).get(i)).intValue(), logicalDrive));
        }
    }
}
