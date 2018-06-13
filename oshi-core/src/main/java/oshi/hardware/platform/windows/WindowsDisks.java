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
import oshi.util.StringUtil;
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

    private static final String ANTECEDENT_PROPERTY = "Antecedent";
    private static final String DEPENDENT_PROPERTY = "Dependent";
    private static final String DESCRIPTION_PROPERTY = "Description";
    private static final String DEVICE_ID_PROPERTY = "DeviceID";
    private static final String DISK_INDEX_PROPERTY = "DiskIndex";
    private static final String INDEX_PROPERTY = "Index";
    private static final String MANUFACTURER_PROPERTY = "Manufacturer";
    private static final String MODEL_PROPERTY = "Model";
    private static final String NAME_PROPERTY = "Name";
    private static final String SERIALNUMBER_PROPERTY = "SerialNumber";
    private static final String SIZE_PROPERTY = "Size";
    private static final String TYPE_PROPERTY = "Type";

    private static final String[] DRIVE_PROPERTIES = new String[] { NAME_PROPERTY, MANUFACTURER_PROPERTY,
            MODEL_PROPERTY, SERIALNUMBER_PROPERTY, SIZE_PROPERTY, INDEX_PROPERTY };
    private static final ValueType[] DRIVE_TYPES = { ValueType.STRING, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.UINT32 };

    private static final String DRIVE_TO_PARTITION_PROPERTIES = StringUtil.join(",",
            new String[] { ANTECEDENT_PROPERTY, DEPENDENT_PROPERTY });
    private static final String LOGICAL_DISK_TO_PARTITION_PROPERTIES = StringUtil.join(",",
            new String[] { ANTECEDENT_PROPERTY, DEPENDENT_PROPERTY });

    private static final String[] PARTITION_PROPERTIES = new String[] { NAME_PROPERTY, TYPE_PROPERTY,
            DESCRIPTION_PROPERTY, DEVICE_ID_PROPERTY, SIZE_PROPERTY, DISK_INDEX_PROPERTY, INDEX_PROPERTY };
    private static final ValueType[] PARTITION_TYPES = { ValueType.STRING, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.UINT32, ValueType.UINT32 };

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
