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

import oshi.hardware.GlobalMemory;
import oshi.hardware.PhysicalMemory;
import oshi.util.FormatUtil;

/**
 * Memory info.
 */
public abstract class AbstractGlobalMemory implements GlobalMemory {

    @Override
    // Temporarily override in all classes until implemented.
    public PhysicalMemory[] getPhysicalMemory() {
        PhysicalMemory[] physicalMemoryArray = new PhysicalMemory[1];
        physicalMemoryArray[0] = new PhysicalMemory();
        return physicalMemoryArray;
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
