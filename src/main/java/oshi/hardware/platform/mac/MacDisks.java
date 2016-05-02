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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;

import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractDisks;
import oshi.util.ExecutingCommand;

/**
 * Mac hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class MacDisks extends AbstractDisks {

    @Override
    public HWDiskStore[] getDisks() {
        List<HWDiskStore> result;
        result = new ArrayList<>();

        String model = "";
        String serial = "";
        String capacity = "";
        long size = 0L;
        int indent = 0;

        ArrayList<String> hwInfo = ExecutingCommand.runNative("system_profiler SPSerialATADataType");
        // Add a non-indented line to the end of the string to prompt List add
        hwInfo.add("END");
        // Iterate the list and store info as it is found.
        for (String checkLine : hwInfo) {
            // Ignore blank lines
            if (checkLine.length() == 0) {
                continue;
            }
            // If indentation backs up by more than 2 spaces, end of this entry
            String s = checkLine.trim();

            if (checkLine.indexOf(s) < indent - 2) {
                HWDiskStore ds = new HWDiskStore();
                ds.setName(String.format("disk%d", result.size()));
                ds.setModel(model);
                ds.setSerial(serial);
                ds.setSize(size);
                result.add(ds);

                // Clear to prep for next
                model = "";
                serial = "";
                capacity = "";
                size = 0L;
            }
            indent = checkLine.indexOf(s);

            String[] split = s.split(":");
            if (split.length < 2) {
                continue;
            }
            switch (split[0]) {
            case "Model":
                model = split[1].trim();
                break;
            case "Serial Number":
                serial = split[1].trim();
                break;
            case "Capacity":
                // Only use the first Capacity we find
                if (capacity.length() == 0) {
                    // Capacity: 209.7 MB (209,715,200 bytes)
                    capacity = split[1].trim();
                    capacity = capacity.substring(capacity.indexOf("(") + 1, capacity.indexOf(" bytes)"))
                            .replaceAll(",", "");
                    // If successful this line is the desired value
                    try {
                        size = Long.parseLong(capacity);
                    } catch (NumberFormatException e) { // NOPMD
                        // If we failed to parse, give up
                    }
                }
                break;
            default:
                // do nothing
            }
        }

        return result.toArray(new HWDiskStore[result.size()]);
    }
}
