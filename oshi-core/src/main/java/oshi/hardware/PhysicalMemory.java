/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;
import oshi.util.FormatUtil;

/**
 * The PhysicalMemory class represents a physical memory device located on a computer system and available to the
 * operating system.
 */
@Immutable
public class PhysicalMemory {

    private final String bankLabel;
    private final long capacity;
    private final long clockSpeed;
    private final String manufacturer;
    private final String memoryType;

    public PhysicalMemory(String bankLabel, long capacity, long clockSpeed, String manufacturer, String memoryType) {
        this.bankLabel = bankLabel;
        this.capacity = capacity;
        this.clockSpeed = clockSpeed;
        this.manufacturer = manufacturer;
        this.memoryType = memoryType;
    }

    /**
     * The bank and/or slot label.
     *
     * @return the bank label
     */
    public String getBankLabel() {
        return bankLabel;
    }

    /**
     * The capacity of memory bank in bytes.
     *
     * @return the capacity
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * The configured memory clock speed in hertz.
     * <p>
     * For DDR memory, this is the data transfer rate, which is a multiple of the actual bus clock speed.
     *
     * @return the clock speed, if avaialable. If unknown, returns -1.
     */
    public long getClockSpeed() {
        return clockSpeed;
    }

    /**
     * The manufacturer of the physical memory.
     *
     * @return the manufacturer
     */
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * The type of physical memory
     *
     * @return the memory type
     */
    public String getMemoryType() {
        return memoryType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Bank label: " + getBankLabel());
        sb.append(", Capacity: " + FormatUtil.formatBytes(getCapacity()));
        sb.append(", Clock speed: " + FormatUtil.formatHertz(getClockSpeed()));
        sb.append(", Manufacturer: " + getManufacturer());
        sb.append(", Memory type: " + getMemoryType());
        return sb.toString();
    }
}
