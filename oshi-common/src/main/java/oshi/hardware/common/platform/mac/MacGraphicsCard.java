/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Graphics card info obtained by system_profiler SPDisplaysDataType.
 */
@ThreadSafe
public abstract class MacGraphicsCard extends AbstractGraphicsCard {

    protected static final boolean IS_APPLE_SILICON = "aarch64".equals(System.getProperty("os.arch"));

    protected MacGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    protected static List<GraphicsCard> parseGraphicsCards(GraphicsCardFactory factory, SysctlLong sysctl) {
        List<GraphicsCard> cardList = new ArrayList<>();
        List<String> sp = ExecutingCommand.runNative("system_profiler SPDisplaysDataType");
        String name = Constants.UNKNOWN;
        String deviceId = Constants.UNKNOWN;
        String vendor = Constants.UNKNOWN;
        List<String> versionInfoList = new ArrayList<>();
        long vram = 0;
        int cardNum = 0;
        for (String line : sp) {
            String[] split = line.trim().split(":", 2);
            if (split.length == 2) {
                String prefix = split[0].toLowerCase(Locale.ROOT);
                if (prefix.equals("chipset model")) {
                    if (cardNum++ > 0) {
                        cardList.add(factory.create(name, deviceId, vendor,
                                versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList),
                                resolveVram(vram, name, sysctl)));
                        deviceId = Constants.UNKNOWN;
                        vendor = Constants.UNKNOWN;
                        vram = 0;
                        versionInfoList.clear();
                    }
                    name = split[1].trim();
                } else if (prefix.equals("device id")) {
                    deviceId = split[1].trim();
                } else if (prefix.equals("vendor")) {
                    vendor = split[1].trim();
                } else if (prefix.contains("version") || prefix.contains("revision")) {
                    versionInfoList.add(line.trim());
                } else if (prefix.startsWith("vram")) {
                    vram = ParseUtil.parseDecimalMemorySizeToBinary(split[1].trim());
                }
            }
        }
        if (cardNum > 0) {
            cardList.add(factory.create(name, deviceId, vendor,
                    versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList),
                    resolveVram(vram, name, sysctl)));
        }
        return cardList;
    }

    private static long resolveVram(long parsedVram, String chipsetName, SysctlLong sysctl) {
        if (parsedVram > 0) {
            return parsedVram;
        }
        if (chipsetName.contains("Apple")) {
            return sysctl.get("hw.memsize", 0L);
        }
        return parsedVram;
    }

    @FunctionalInterface
    protected interface GraphicsCardFactory {
        GraphicsCard create(String name, String deviceId, String vendor, String versionInfo, long vram);
    }

    @FunctionalInterface
    protected interface SysctlLong {
        long get(String name, long defaultValue);
    }
}
