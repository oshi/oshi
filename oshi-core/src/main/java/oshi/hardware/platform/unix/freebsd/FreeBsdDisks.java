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
package oshi.hardware.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractDisks;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * FreeBSD hard disk implementation.
 *
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdDisks extends AbstractDisks {

    private static final long serialVersionUID = 1L;

    @Override
    public HWDiskStore[] getDisks() {
        // Get list of valid disks
        List<String> devices = Arrays.asList(BsdSysctlUtil.sysctl("kern.disks", "").split("\\s+"));

        // Create map indexed by device name for multiple command reference
        Map<String, HWDiskStore> diskMap = new HashMap<>();

        // Run iostat -Ix to enumerate disks by name and get kb r/w
        ArrayList<String> disks = ExecutingCommand.runNative("iostat -Ix");
        for (String line : disks) {
            String[] split = line.split("\\s+");
            if (split.length < 7 || !devices.contains(split[0])) {
                continue;
            }
            HWDiskStore store = new HWDiskStore();
            store.setName(split[0]);
            store.setReads((long) (ParseUtil.parseDoubleOrDefault(split[3], 0d) * 1024));
            store.setWrites((long) (ParseUtil.parseDoubleOrDefault(split[4], 0d) * 1024));
            diskMap.put(split[0], store);
        }

        // Now grab dmssg output
        List<String> geom = ExecutingCommand.runNative("geom disk list");

        HWDiskStore store = null;
        for (String line : geom) {
            if (line.startsWith("Geom name:")) {
                String device = line.substring(line.lastIndexOf(' ') + 1);
                // Get the device.
                if (devices.contains(device)) {
                    store = diskMap.get(device);
                    // If for some reason we didn't have one, create
                    // a new value here.
                    if (store == null) {
                        store = new HWDiskStore();
                        store.setName(device);
                    }
                }
            }
            // If we don't have a valid store, don't bother parsing anything
            // until we do.
            if (store == null) {
                continue;
            }
            line = line.trim();
            if (line.startsWith("Mediasize:")) {
                String[] split = line.split("\\s+");
                if (split.length > 1) {
                    store.setSize(ParseUtil.parseLongOrDefault(split[1], 0L));
                }
            }
            if (line.startsWith("descr:")) {
                store.setModel(line.replace("descr:", "").trim());
            }
            if (line.startsWith("ident:")) {
                store.setSerial(line.replace("ident:", "").replace("(null)", "").trim());
            }
        }

        // Populate result array
        HWDiskStore[] results = new HWDiskStore[diskMap.keySet().size()];
        int index = 0;
        for (Entry<String, HWDiskStore> entry : diskMap.entrySet()) {
            results[index++] = entry.getValue();
        }

        return results;
    }
}
