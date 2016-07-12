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
package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractDisks;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Solaris hard disk implementation.
 *
 * @author widdis[at]gmail[dot]com
 */
public class SolarisDisks extends AbstractDisks {

    private static final long serialVersionUID = 1L;

    @Override
    public HWDiskStore[] getDisks() {
        // Create map indexed by device name for multiple command reference
        Map<String, HWDiskStore> diskMap = new HashMap<>();

        // First, run iostat -er to enumerate disks by name. Sample output:
        ArrayList<String> disks = ExecutingCommand.runNative("iostat -er");
        for (String line : disks) {
            // The -r switch enables comma delimited for easy parsing!
            String[] split = line.split(",");
            if (split.length < 5 || split[0].equals("device")) {
                continue;
            }
            HWDiskStore store = new HWDiskStore();
            store.setName(split[0]);
            diskMap.put(split[0], store);
        }

        // Next, run iostat -Er to get model, etc.
        disks = ExecutingCommand.runNative("iostat -Er");
        // We'll use Model if available, otherwise Vendor+Product
        String disk = "";
        String model = "";
        String vendor = "";
        String product = "";
        String serial = "";
        long size = 0;
        for (String line : disks) {
            // The -r switch enables comma delimited for easy parsing!
            // No guarantees on which line the results appear so we'll nest
            // a
            // loop iterating on the comma splits
            String[] split = line.split(",");
            for (String keyValue : split) {
                keyValue = keyValue.trim();
                // If entry is tne name of a disk, this is beginning of new
                // output for that disk.
                if (diskMap.keySet().contains(keyValue)) {
                    // First, if we have existing output from previous,
                    // update
                    if (!disk.isEmpty()) {
                        updateStore(diskMap.get(disk), model, vendor, product, serial, size);
                    }
                    // Reset values for next iteration
                    disk = keyValue;
                    model = "";
                    vendor = "";
                    product = "";
                    serial = "";
                    size = 0L;
                    continue;
                }
                // Otherwise update variables
                if (keyValue.startsWith("Model:")) {
                    model = keyValue.replace("Model:", "").trim();
                } else if (keyValue.startsWith("Serial No:")) {
                    serial = keyValue.replace("Serial No:", "").trim();
                } else if (keyValue.startsWith("Vendor:")) {
                    vendor = keyValue.replace("Vendor:", "").trim();
                } else if (keyValue.startsWith("Product:")) {
                    product = keyValue.replace("Product:", "").trim();
                } else if (keyValue.startsWith("Size:")) {
                    // Size: 1.23GB <1227563008 bytes>
                    String[] bytes = keyValue.split("<");
                    if (bytes.length > 1) {
                        bytes = bytes[1].split("\\s+");
                        size = ParseUtil.parseLongOrDefault(bytes[0], 0L);
                    }
                }
            }
            // At end of output update last entry
            if (!disk.isEmpty()) {
                updateStore(diskMap.get(disk), model, vendor, product, serial, size);
            }
        }

        // Finally use kstat to get reads/writes
        // simultaneously populate result array
        HWDiskStore[] results = new HWDiskStore[diskMap.keySet().size()];
        int index = 0;
        for (Entry<String, HWDiskStore> entry : diskMap.entrySet()) {
            ArrayList<String> stats = ExecutingCommand.runNative("kstat -p ::" + entry.getKey());
            for (String line : stats) {
                String[] split = line.split("\\s+");
                if (split.length < 2) {
                    continue;
                }
                if (split[0].endsWith(":nread")) {
                    entry.getValue().setReads(ParseUtil.parseLongOrDefault(split[1], 0L));
                } else if (split[0].endsWith(":nwritten")) {
                    entry.getValue().setWrites(ParseUtil.parseLongOrDefault(split[1], 0L));
                }
            }
            results[index++] = entry.getValue();
        }

        return results;
    }

    /**
     * Updates the HWDiskStore. If model name is nonempty it is used, otherwise
     * vendor+product are used for model
     * 
     * @param store
     *            A HWDiskStore
     * @param model
     *            model name, or empty string if none
     * @param vendor
     *            vendor name, or empty string if none
     * @param product
     *            product nmae, or empty string if none
     * @param serial
     *            serial number, or empty string if none
     * @param size
     *            size of the drive in bytes
     */
    private void updateStore(HWDiskStore store, String model, String vendor, String product, String serial, long size) {
        store.setModel(model.isEmpty() ? (vendor + " " + product).trim() : model);
        store.setSerial(serial);
        store.setSize(size);
    }
}
