/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.util.Constants;

/**
 * Attempts to create a unique computer identifier. Note that serial numbers won't work on Linux without user
 * cooperation.
 */
public class ComputerID {
    /**
     * <p>
     * main.
     * </p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(String[] args) {
        String unknownHash = String.format("%08x", Constants.UNKNOWN.hashCode());

        System.out.println("Here's a unique (?) id for your computer.");
        System.out.println(getComputerIdentifier());
        System.out.println("If any field is " + unknownHash
                + " then I couldn't find a serial number or uuid, and running as sudo might change this.");
    }

    /**
     * Generates a Computer Identifier, which may be part of a strategy to construct a licence key. (The identifier may
     * not be unique as in one case hashcode could be same for multiple values, and the result may differ based on
     * whether the program is running with sudo/root permission.) The identifier string is based upon the processor
     * serial number, vendor, processor identifier, and total processor count.
     *
     * @return A string containing four hyphen-delimited fields representing the processor; the first 3 are 32-bit
     *         hexadecimal values and the last one is an integer value.
     */
    public static String getComputerIdentifier() {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
        CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();
        ComputerSystem computerSystem = hardwareAbstractionLayer.getComputerSystem();

        String vendor = operatingSystem.getManufacturer();
        String processorSerialNumber = computerSystem.getSerialNumber();
        String uuid = computerSystem.getHardwareUUID();
        String processorIdentifier = centralProcessor.getProcessorIdentifier().getIdentifier();
        int processors = centralProcessor.getLogicalProcessorCount();

        String delimiter = "-";

        return String.format("%08x", vendor.hashCode()) + delimiter
                + String.format("%08x", processorSerialNumber.hashCode()) + delimiter
                + String.format("%08x", uuid.hashCode()) + delimiter
                + String.format("%08x", processorIdentifier.hashCode()) + delimiter + processors;
    }
}
