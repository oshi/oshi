/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GlobalMemory;
import oshi.hardware.PhysicalMemory;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;

/**
 * Memory info.
 */
@ThreadSafe
public abstract class AbstractGlobalMemory implements GlobalMemory {

    @Override
    public List<PhysicalMemory> getPhysicalMemory() {
        // dmidecode requires sudo permission but is the only option on Linux
        // and Unix
        List<PhysicalMemory> pmList = new ArrayList<>();
        List<String> dmi = ExecutingCommand.runNative("dmidecode --type 17");
        int bank = 0;
        String bankLabel = Constants.UNKNOWN;
        String locator = "";
        long capacity = 0L;
        long speed = 0L;
        String manufacturer = Constants.UNKNOWN;
        String memoryType = Constants.UNKNOWN;
        for (String line : dmi) {
            if (line.trim().contains("DMI type 17")) {
                // Save previous bank
                if (bank++ > 0) {
                    if (capacity > 0) {
                        pmList.add(new PhysicalMemory(bankLabel + locator, capacity, speed, manufacturer, memoryType));
                    }
                    bankLabel = Constants.UNKNOWN;
                    locator = "";
                    capacity = 0L;
                    speed = 0L;
                }
            } else if (bank > 0) {
                String[] split = line.trim().split(":");
                if (split.length == 2) {
                    switch (split[0]) {
                    case "Bank Locator":
                        bankLabel = split[1].trim();
                        break;
                    case "Locator":
                        locator = "/" + split[1].trim();
                        break;
                    case "Size":
                        capacity = ParseUtil.parseDecimalMemorySizeToBinary(split[1].trim());
                        break;
                    case "Type":
                        memoryType = split[1].trim();
                        break;
                    case "Speed":
                        speed = ParseUtil.parseHertz(split[1]);
                        break;
                    case "Manufacturer":
                        manufacturer = split[1].trim();
                        break;
                    default:
                        break;
                    }
                }
            }
        }
        if (capacity > 0) {
            pmList.add(new PhysicalMemory(bankLabel + locator, capacity, speed, manufacturer, memoryType));
        }
        return pmList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available: ");
        sb.append(FormatUtil.formatBytes(getAvailable()));
        sb.append("/");
        sb.append(FormatUtil.formatBytes(getTotal()));
        return sb.toString();
    }
}
