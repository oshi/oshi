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

import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractDisks;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;

/**
 * Windows hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class WindowsDisks extends AbstractDisks {

    private static final long serialVersionUID = 1L;

    /**
     * Maps to store read/write bytes per drive index
     */
    private static Map<String, Long> readMap = new HashMap<>();
    private static Map<String, Long> writeMap = new HashMap<>();

    private static final ValueType[] DRIVE_TYPES = { ValueType.STRING, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.UINT32 };

    @Override
    public HWDiskStore[] getDisks() {
        List<HWDiskStore> result;
        result = new ArrayList<>();
        readMap.clear();
        writeMap.clear();
        populateReadWriteMaps();

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
            ds.setWrites(writeMap.getOrDefault(index, 0L));
            // If successful this line is the desired value
            ds.setSize(ParseUtil.parseLongOrDefault((String) vals.get("Size").get(i), 0L));
            result.add(ds);
        }
        return result.toArray(new HWDiskStore[result.size()]);
    }

    private void populateReadWriteMaps() {
        // Although the field names say "PerSec" this is the Raw Data from which
        // the associated fields are populated in the Formatted Data class, so
        // in fact this is the data we want
        Map<String, List<String>> vals = WmiUtil.selectStringsFrom(null, "Win32_PerfRawData_PerfDisk_PhysicalDisk",
                "Name,DiskReadBytesPerSec,DiskWriteBytesPerSec", null);
        for (int i = 0; i < vals.get("Name").size(); i++) {
            String index = vals.get("Name").get(i).split("\\s+")[0];
            readMap.put(index, ParseUtil.parseLongOrDefault(vals.get("DiskReadBytesPerSec").get(i), 0L));
            writeMap.put(index, ParseUtil.parseLongOrDefault(vals.get("DiskWriteBytesPerSec").get(i), 0L));
        }
    }
}
