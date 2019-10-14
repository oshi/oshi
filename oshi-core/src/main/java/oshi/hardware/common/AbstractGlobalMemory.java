/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.common;

import java.util.ArrayList;
import java.util.List;

import oshi.hardware.GlobalMemory;
import oshi.hardware.PhysicalMemory;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;

/**
 * Memory info.
 */
public abstract class AbstractGlobalMemory implements GlobalMemory {

    @Override
    public PhysicalMemory[] getPhysicalMemory() {
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
                        capacity = parsePhysicalMemorySize(split[1].trim());
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
        return pmList.toArray(new PhysicalMemory[0]);
    }

    /**
     * Parses a string such as "4096 MB" to its long. Used to parse macOS and
     * *nix memory chip sizes. Although the units given are decimal they must
     * parse to binary units.
     *
     * @param size
     *            A string of memory sizes like "4096 MB"
     * @return the size parsed to a long
     */
    protected long parsePhysicalMemorySize(String size) {
        String[] mem = ParseUtil.whitespaces.split(size);
        long capacity = ParseUtil.parseLongOrDefault(mem[0], 0L);
        if (mem.length == 2 && mem[1].length() > 1) {
            switch (mem[1].charAt(0)) {
            case 'T':
                capacity <<= 40;
                break;
            case 'G':
                capacity <<= 30;
                break;
            case 'M':
                capacity <<= 20;
                break;
            case 'K':
            case 'k':
                capacity <<= 10;
                break;
            default:
                break;
            }
        }
        return capacity;
    }

    /**
     * Convert SMBIOS type number to a human readable string
     *
     * @param type
     *            The SMBIOS type
     * @return A string describing the type
     */
    protected static String smBiosMemoryType(int type) {
        // https://www.dmtf.org/sites/default/files/standards/documents/DSP0134_3.2.0.pdf
        // table 76
        switch (type) {
        case 0x01:
            return "Other";
        case 0x03:
            return "DRAM";
        case 0x04:
            return "EDRAM";
        case 0x05:
            return "VRAM";
        case 0x06:
            return "SRAM";
        case 0x07:
            return "RAM";
        case 0x08:
            return "ROM";
        case 0x09:
            return "FLASH";
        case 0x0A:
            return "EEPROM";
        case 0x0B:
            return "FEPROM";
        case 0x0C:
            return "EPROM";
        case 0x0D:
            return "CDRAM";
        case 0x0E:
            return "3DRAM";
        case 0x0F:
            return "SDRAM";
        case 0x10:
            return "SGRAM";
        case 0x11:
            return "RDRAM";
        case 0x12:
            return "DDR";
        case 0x13:
            return "DDR2";
        case 0x14:
            return "DDR2 FB-DIMM";
        case 0x18:
            return "DDR3";
        case 0x19:
            return "FBD2";
        case 0x1A:
            return "DDR4";
        case 0x1B:
            return "LPDDR";
        case 0x1C:
            return "LPDDR2";
        case 0x1D:
            return "LPDDR3";
        case 0x1E:
            return "LPDDR4";
        case 0x1F:
            return "Logical non-volatile device";
        case 0x02:
        default:
            return "Unknown";
        }
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
