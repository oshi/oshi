/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.Disks;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.jna.platform.windows.Kernel32;
import oshi.util.ParseUtil;
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

    private static final ValueType[] DRIVE_TYPES = { ValueType.STRING, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.UINT32 };

    private static final ValueType[] READ_WRITE_TYPES = { ValueType.STRING, ValueType.UINT32, ValueType.STRING,
            ValueType.UINT32, ValueType.STRING, ValueType.STRING, ValueType.STRING };

    private static final ValueType[] PARTITION_TYPES = { ValueType.STRING, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.UINT32, ValueType.UINT32 };

    private static final Pattern DEVICE_ID = Pattern.compile(".*\\.DeviceID=\"(.*)\"");

    private static final int BUFSIZE = 255;

    @Override
    public HWDiskStore[] getDisks() {
        List<HWDiskStore> result;
        result = new ArrayList<>();
        populateReadWriteMaps();
        populatePartitionMaps();

        Map<String, List<Object>> vals = WmiUtil.selectObjectsFrom(null, "Win32_DiskDrive",
                "Name,Manufacturer,Model,SerialNumber,Size,Index", null, DRIVE_TYPES);
        for (int i = 0; i < vals.get("Name").size(); i++) {
            HWDiskStore ds = new HWDiskStore();
            ds.setName((String) vals.get("Name").get(i));
            ds.setModel(String.format("%s %s", vals.get("Model").get(i), vals.get("Manufacturer").get(i)).trim());
            // Most vendors store serial # as a hex string; convert
            ds.setSerial(ParseUtil.hexStringToString((String) vals.get("SerialNumber").get(i)));
            String index = vals.get("Index").get(i).toString();
            ds.setReads(readMap.getOrDefault(index, 0L));
            ds.setReadBytes(readByteMap.getOrDefault(index, 0L));
            ds.setWrites(writeMap.getOrDefault(index, 0L));
            ds.setWriteBytes(writeByteMap.getOrDefault(index, 0L));
            ds.setTransferTime(xferTimeMap.getOrDefault(index, 0L));
            ds.setTimeStamp(timeStampMap.getOrDefault(index, 0L));
            // If successful this line is the desired value
            ds.setSize(ParseUtil.parseLongOrDefault((String) vals.get("Size").get(i), 0L));
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

    private void populateReadWriteMaps() {
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
                "Name,DiskReadsPerSec,DiskReadBytesPerSec,DiskWritesPerSec,DiskWriteBytesPerSec,PercentDiskTime,Timestamp_Sys100NS",
                null, READ_WRITE_TYPES);
        for (int i = 0; i < vals.get("Name").size(); i++) {
            String name = ((String) vals.get("Name").get(i)).split("\\s+")[0];
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
                "Antecedent,Dependent", null);
        for (int i = 0; i < partitionQueryMap.get("Antecedent").size(); i++) {
            mAnt = DEVICE_ID.matcher(partitionQueryMap.get("Antecedent").get(i));
            mDep = DEVICE_ID.matcher(partitionQueryMap.get("Dependent").get(i));
            if (mAnt.matches() && mDep.matches()) {
                driveToPartitionMap
                        .computeIfAbsent(mAnt.group(1).replaceAll("\\\\\\\\", "\\\\"), k -> new ArrayList<String>())
                        .add(mDep.group(1));
            }
        }

        // Map partitions to logical disks
        partitionQueryMap = WmiUtil.selectStringsFrom(null, "Win32_LogicalDiskToPartition", "Antecedent,Dependent",
                null);
        for (int i = 0; i < partitionQueryMap.get("Antecedent").size(); i++) {
            mAnt = DEVICE_ID.matcher(partitionQueryMap.get("Antecedent").get(i));
            mDep = DEVICE_ID.matcher(partitionQueryMap.get("Dependent").get(i));
            if (mAnt.matches() && mDep.matches()) {
                partitionToLogicalDriveMap.put(mAnt.group(1), mDep.group(1) + "\\");
            }
        }

        // Next, get all partitions and create objects
        final Map<String, List<Object>> hwPartitionQueryMap = WmiUtil.selectObjectsFrom(null, "Win32_DiskPartition",
                "Name,Type,Description,DeviceID,Size,DiskIndex,Index", null, PARTITION_TYPES);
        for (int i = 0; i < hwPartitionQueryMap.get("Name").size(); i++) {
            String deviceID = (String) hwPartitionQueryMap.get("DeviceID").get(i);
            String logicalDrive = partitionToLogicalDriveMap.getOrDefault(deviceID, "");
            String uuid = "";
            if (!logicalDrive.isEmpty()) {
                // Get matching volume for UUID
                char[] volumeChr = new char[BUFSIZE];
                Kernel32.INSTANCE.GetVolumeNameForVolumeMountPoint(logicalDrive, volumeChr, BUFSIZE);
                uuid = ParseUtil.parseUuidOrDefault(new String(volumeChr).trim(), "");
            }
            partitionMap.put(deviceID,
                    new HWPartition((String) hwPartitionQueryMap.get("Name").get(i),
                            (String) hwPartitionQueryMap.get("Type").get(i),
                            (String) hwPartitionQueryMap.get("Description").get(i), uuid,
                            ParseUtil.parseLongOrDefault((String) hwPartitionQueryMap.get("Size").get(i), 0L),
                            ((Long) hwPartitionQueryMap.get("DiskIndex").get(i)).intValue(),
                            ((Long) hwPartitionQueryMap.get("Index").get(i)).intValue(), logicalDrive));
        }
    }
}
