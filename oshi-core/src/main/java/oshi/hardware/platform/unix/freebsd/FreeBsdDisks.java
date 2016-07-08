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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractDisks;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * FreeBSD hard disk implementation.
 *
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdDisks extends AbstractDisks {

    private static final long serialVersionUID = 1L;

    private static final Pattern MODEL = Pattern.compile("<([^>]+)>.*");
    private static final Pattern SERIAL = Pattern.compile("Serial Number (.*)");
    private static final Pattern SIZE = Pattern.compile("\\S+ \\((\\d+) (\\d+) byte sectors.*");

    @Override
    public HWDiskStore[] getDisks() {
        // Create map indexed by device name for multiple command reference
        Map<String, HWDiskStore> diskMap = new HashMap<>();

        // First, run iostat -Ix to enumerate disks by name and get kb r/w
        ArrayList<String> disks = ExecutingCommand.runNative("iostat -Ix");
        for (String line : disks) {
            String[] split = line.split("\\s+");
            if (split.length < 7 || split[0].equals("device") || split[0].startsWith("pass")) {
                continue;
            }
            HWDiskStore store = new HWDiskStore();
            store.setName(split[0]);
            store.setReads((long) (ParseUtil.parseDoubleOrDefault(split[3], 0d) * 1024));
            store.setWrites((long) (ParseUtil.parseDoubleOrDefault(split[4], 0d) * 1024));
            diskMap.put(split[0], store);
        }

        // Now grab dmssg output
        List<String> dmesg = FileUtil.readFile("/var/run/dmesg.boot");

        // Now for each device, parse dmesg
        for (Entry<String, HWDiskStore> entry : diskMap.entrySet()) {
            String disk = entry.getKey();
            HWDiskStore store = entry.getValue();
            String startsWith = disk + ":";
            Matcher m;
            for (String line : dmesg) {
                if (!line.startsWith(startsWith)) {
                    continue;
                }
                line = line.replace(startsWith, "").trim();
                m = MODEL.matcher(line);
                if (m.matches()) {
                    store.setModel(m.group(1));
                    continue;
                }
                m = SERIAL.matcher(line);
                if (m.matches()) {
                    store.setSerial(m.group(1));
                    continue;
                }
                m = SIZE.matcher(line);
                if (m.matches()) {
                    store.setSize(ParseUtil.parseLongOrDefault(m.group(1), 0L)
                            * ParseUtil.parseLongOrDefault(m.group(2), 0L));
                    continue;
                }
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
