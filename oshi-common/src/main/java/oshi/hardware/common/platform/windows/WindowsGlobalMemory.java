/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractGlobalMemory;

/**
 * Common Windows global memory logic shared between JNA and FFM implementations.
 */
@ThreadSafe
public abstract class WindowsGlobalMemory extends AbstractGlobalMemory {

    protected static String memoryType(int type) {
        switch (type) {
            case 0:
                return "Unknown";
            case 1:
                return "Other";
            case 2:
                return "DRAM";
            case 3:
                return "Synchronous DRAM";
            case 4:
                return "Cache DRAM";
            case 5:
                return "EDO";
            case 6:
                return "EDRAM";
            case 7:
                return "VRAM";
            case 8:
                return "SRAM";
            case 9:
                return "RAM";
            case 10:
                return "ROM";
            case 11:
                return "Flash";
            case 12:
                return "EEPROM";
            case 13:
                return "FEPROM";
            case 14:
                return "EPROM";
            case 15:
                return "CDRAM";
            case 16:
                return "3DRAM";
            case 17:
                return "SDRAM";
            case 18:
                return "SGRAM";
            case 19:
                return "RDRAM";
            case 20:
                return "DDR";
            case 21:
                return "DDR2";
            case 22:
                return "BRAM";
            case 23:
                return "DDR FB-DIMM";
            default:
                return smBiosMemoryType(type);
        }
    }

    protected static String smBiosMemoryType(int type) {
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
            case 0x20:
                return "HBM";
            case 0x21:
                return "HBM2";
            case 0x22:
                return "DDR5";
            case 0x23:
                return "LPDDR5";
            case 0x24:
                return "HBM3";
            case 0x02:
            default:
                return "Unknown";
        }
    }
}
