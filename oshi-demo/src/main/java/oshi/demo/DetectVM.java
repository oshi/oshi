/*
 * Copyright 2019-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo;

import java.util.Optional;

import oshi.SystemInfo;
import oshi.annotation.SuppressForbidden;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * Uses OSHI to attempt to identify whether the user is on a Virtual Machine
 */
public class DetectVM {

    /**
     * Private constructor for utility class.
     */
    private DetectVM() {
    }

    /**
     * Entry point, executing the {@link #identifyVM} method.
     *
     * @param args Arguments, ignored.
     */
    @SuppressForbidden(reason = "Using System.out in a demo class")
    public static void main(String[] args) {
        String vmString = identifyVM();

        if (vmString.isEmpty()) {
            System.out.println("You do not appear to be on a Virtual Machine.");
        } else {
            System.out.println("You appear to be on a VM: " + vmString);
        }
    }

    /**
     * The function attempts to identify which Virtual Machine (VM) based on common VM signatures in the CPU vendor
     * string, computer model, and MAC address.
     *
     * @return A string indicating the machine's virtualization info if it can be determined, or an empty string
     *         otherwise.
     * @deprecated Use {@link HardwareAbstractionLayer#getVirtualization()}, which this method delegates to. It
     *             distinguishes "no signature found" from a detected platform without overloading the empty string.
     */
    @Deprecated
    public static String identifyVM() {
        HardwareAbstractionLayer hw = new SystemInfo().getHardware();
        Optional<String> virtualization = hw.getVirtualization();
        return virtualization.isPresent() ? virtualization.get() : "";
    }
}
