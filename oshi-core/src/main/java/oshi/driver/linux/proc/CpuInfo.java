/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux.proc;

import static oshi.util.platform.linux.ProcPath.CPUINFO;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * Utility to read CPU info from {@code /proc/cpuinfo}
 */
@ThreadSafe
public final class CpuInfo {

    private CpuInfo() {
    }

    /**
     * Gets the CPU manufacturer from {@code /proc/cpuinfo}
     *
     * @return The manufacturer if known, null otherwise
     */
    public static String queryCpuManufacturer() {
        List<String> cpuInfo = FileUtil.readFile(CPUINFO);
        for (String line : cpuInfo) {
            if (line.startsWith("CPU implementer")) {
                int part = ParseUtil.parseLastInt(line, 0);
                switch (part) {
                case 0x41:
                    return "ARM";
                case 0x42:
                    return "Broadcom";
                case 0x43:
                    return "Cavium";
                case 0x44:
                    return "DEC";
                case 0x4e:
                    return "Nvidia";
                case 0x50:
                    return "APM";
                case 0x51:
                    return "Qualcomm";
                case 0x53:
                    return "Samsung";
                case 0x56:
                    return "Marvell";
                case 0x66:
                    return "Faraday";
                case 0x69:
                    return "Intel";
                default:
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Gets the board manufacturer, model, version, and serial number from {@code /proc/cpuinfo}
     *
     * @return A quartet of strings for manufacturer, model, version, and serial number. Each one may be null if
     *         unknown.
     */
    public static Quartet<String, String, String, String> queryBoardInfo() {
        String pcManufacturer = null;
        String pcModel = null;
        String pcVersion = null;
        String pcSerialNumber = null;

        List<String> cpuInfo = FileUtil.readFile(CPUINFO);
        for (String line : cpuInfo) {
            String[] splitLine = ParseUtil.whitespacesColonWhitespace.split(line);
            if (splitLine.length < 2) {
                continue;
            }
            switch (splitLine[0]) {
            case "Hardware":
                pcModel = splitLine[1];
                break;
            case "Revision":
                pcVersion = splitLine[1];
                if (pcVersion.length() > 1) {
                    pcManufacturer = queryBoardManufacturer(pcVersion.charAt(1));
                }
                break;
            case "Serial":
                pcSerialNumber = splitLine[1];
                break;
            default:
                // Do nothing
            }
        }
        return new Quartet<>(pcManufacturer, pcModel, pcVersion, pcSerialNumber);
    }

    private static String queryBoardManufacturer(char digit) {
        switch (digit) {
        case '0':
            return "Sony UK";
        case '1':
            return "Egoman";
        case '2':
            return "Embest";
        case '3':
            return "Sony Japan";
        case '4':
            return "Embest";
        case '5':
            return "Stadium";
        default:
            return Constants.UNKNOWN;
        }
    }
}
