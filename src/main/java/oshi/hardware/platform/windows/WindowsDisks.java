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
import java.util.List;
import java.util.Map;

import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractDisks;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;

/**
 * Windows hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class WindowsDisks extends AbstractDisks {

    private static final long serialVersionUID = 1L;

    @Override
    public HWDiskStore[] getDisks() {
        List<HWDiskStore> result;
        result = new ArrayList<>();

        Map<String, List<String>> vals = WmiUtil.selectStringsFrom(null, "Win32_DiskDrive",
                "Name,Manufacturer,Model,SerialNumber,Size", null);
        for (int i = 0; i < vals.get("Name").size(); i++) {
            HWDiskStore ds = new HWDiskStore();
            ds.setName(vals.get("Name").get(i));
            ds.setModel(String.format("%s %s", vals.get("Model").get(i), vals.get("Manufacturer").get(i)).trim());
            // Most vendors store serial # as a hex string; convert
            ds.setSerial(ParseUtil.hexStringToString(vals.get("SerialNumber").get(i)));
            // If successful this line is the desired value
            try {
                ds.setSize(Long.parseLong(vals.get("Size").get(i)));
            } catch (NumberFormatException e) {
                // If we failed to parse, give up
                // This is expected for an empty string on some drives
                ds.setSize(0L);
            }
            result.add(ds);
        }
        return result.toArray(new HWDiskStore[result.size()]);
    }
}
