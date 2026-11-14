/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Memory obtained by kstat.
 */
public abstract class SolarisGlobalMemory extends AbstractGlobalMemory {

    private final Supplier<Long> available = memoize(this::queryAvailable, defaultExpiration());
    private final Supplier<Long> total = memoize(this::queryTotal);
    private final Supplier<Long> pageSize = memoize(this::queryPageSize);
    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    @Override
    public long getAvailable() {
        return available.get();
    }

    @Override
    public long getTotal() {
        return total.get();
    }

    @Override
    public long getPageSize() {
        return pageSize.get();
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
    }

    /**
     * Queries available memory in bytes.
     *
     * @return available memory in bytes
     */
    protected abstract long queryAvailable();

    /**
     * Queries total physical memory in bytes.
     *
     * @return total physical memory in bytes
     */
    protected abstract long queryTotal();

    /**
     * Queries the system page size in bytes via the {@code pagesize} command.
     *
     * @return the page size in bytes
     */
    protected long queryPageSize() {
        return ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("pagesize"), 4096L);
    }

    /**
     * Constructs the {@link SolarisVirtualMemory} paired with this GlobalMemory instance.
     *
     * @return the VirtualMemory paired with this GlobalMemory implementation
     */
    protected VirtualMemory createVirtualMemory() {
        return new SolarisVirtualMemory(this);
    }

    @Override
    public List<PhysicalMemory> getPhysicalMemory() {
        // Solaris has no dmidecode; smbios reads the same SMBIOS table without privilege. SPARC has no SMBIOS table,
        // and
        // reports no modules
        return parseSmbiosMemoryDevices(ExecutingCommand.runNative("smbios -t SMB_TYPE_MEMDEVICE"));
    }

    /**
     * Parses the output of {@code smbios -t SMB_TYPE_MEMDEVICE}, one record per memory device (SMBIOS type 17).
     * Package-private for testing.
     * <p>
     * Each record begins with an {@code ID SIZE TYPE} row naming {@code SMB_TYPE_MEMDEVICE}, followed by
     * {@code Key: value} lines. A slot with nothing installed reports no size and is skipped. The bank label joins the
     * {@code Bank Locator} and {@code Device Locator}, as {@code dmidecode}'s bank and locator are joined on Linux.
     *
     * @param smbios the lines emitted by {@code smbios}
     * @return the installed memory modules
     */
    static List<PhysicalMemory> parseSmbiosMemoryDevices(List<String> smbios) {
        List<PhysicalMemory> modules = new ArrayList<>();
        SmbiosMemoryDevice device = null;
        for (String line : smbios) {
            if (line.contains("SMB_TYPE_MEMDEVICE")) {
                addIfInstalled(modules, device);
                device = new SmbiosMemoryDevice();
            } else if (device != null) {
                device.parse(line);
            }
        }
        addIfInstalled(modules, device);
        return modules;
    }

    private static void addIfInstalled(List<PhysicalMemory> modules, @Nullable SmbiosMemoryDevice device) {
        if (device != null && device.capacity > 0) {
            String label = device.bank.isEmpty() ? device.locator : device.bank + "/" + device.locator;
            modules.add(new PhysicalMemory(ParseUtil.getStringValueOrUnknown(label), device.capacity, device.speed,
                    ParseUtil.getStringValueOrUnknown(device.manufacturer),
                    ParseUtil.getStringValueOrUnknown(device.memoryType),
                    ParseUtil.getStringValueOrUnknown(device.partNumber),
                    ParseUtil.getStringValueOrUnknown(device.serialNumber)));
        }
    }

    private static final class SmbiosMemoryDevice {
        private String manufacturer = Constants.UNKNOWN;
        private String locator = "";
        private String bank = "";
        private long capacity;
        private String memoryType = Constants.UNKNOWN;
        private long speed = -1L;
        private String partNumber = Constants.UNKNOWN;
        private String serialNumber = Constants.UNKNOWN;

        /**
         * Reads one {@code Key: value} line of the record into this device.
         *
         * @param line a line of the record
         */
        private void parse(String line) {
            String[] split = line.trim().split(":", 2);
            if (split.length < 2) {
                return;
            }
            String value = split[1].trim();
            switch (split[0]) {
                case "Manufacturer":
                    this.manufacturer = value;
                    break;
                case "Device Locator":
                    this.locator = value;
                    break;
                case "Bank Locator":
                    this.bank = value;
                    break;
                case "Size":
                    // "6442450944 bytes"
                    this.capacity = ParseUtil.parseLongOrDefault(ParseUtil.whitespaces.split(value, 2)[0], 0L);
                    break;
                case "Memory Type":
                    // "26 (DDR4)": the name is in parentheses
                    int open = value.indexOf('(');
                    int close = value.lastIndexOf(')');
                    this.memoryType = open >= 0 && close > open ? value.substring(open + 1, close) : value;
                    break;
                case "Speed":
                    this.speed = ParseUtil.parseSpeed(value);
                    break;
                case "Part Number":
                    this.partNumber = value;
                    break;
                case "Serial Number":
                    this.serialNumber = value;
                    break;
                default:
                    break;
            }
        }
    }
}
