/*
 * Copyright 2019-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import oshi.SystemInfo;
import oshi.demo.annotation.SuppressForbidden;
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

    public static final List<String> NON_UNIQUE_UUIDS = Arrays.asList("03000200-0400-0500-0006-000700080009",
            "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF", "00000000-0000-0000-0000-000000000000");

    /**
     * <p>
     * main.
     * </p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    @SuppressForbidden(reason = "Using System.out in a demo class")
    public static void main(String[] args) {
        String unknownHash = String.format(Locale.ROOT, "%08x", Constants.UNKNOWN.hashCode());

        System.out.println("Here's a unique (?) id for your computer.");
        System.out.println(getComputerIdentifier());
        System.out.println("If any field is " + unknownHash
                + " then I couldn't find a serial number or unique uuid, and running as sudo might change this.");
    }

    /**
     * Generates a Computer Identifier, which may be part of a strategy to construct a licence key. (The identifier may
     * not be unique as in one case hashcode could be same for multiple values, and the result may differ based on
     * whether the program is running with sudo/root permission.) The identifier string is based upon the processor
     * serial number, vendor, system UUID, processor identifier, and total processor count.
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
        if (NON_UNIQUE_UUIDS.contains(uuid.toUpperCase(Locale.ROOT))) {
            uuid = Constants.UNKNOWN;
        }
        String processorIdentifier = centralProcessor.getProcessorIdentifier().getIdentifier();
        int processors = centralProcessor.getLogicalProcessorCount();

        String delimiter = "-";

        return String.format(Locale.ROOT, "%08x", vendor.hashCode()) + delimiter
                + String.format(Locale.ROOT, "%08x", processorSerialNumber.hashCode()) + delimiter
                + String.format(Locale.ROOT, "%08x", uuid.hashCode()) + delimiter
                + String.format(Locale.ROOT, "%08x", processorIdentifier.hashCode()) + delimiter + processors;
    }
}
