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
 * Uses OSHI to attempt to identify the virtualization or container platform the user is running on
 */
public class DetectVM {

    /**
     * Private constructor for utility class.
     */
    private DetectVM() {
    }

    /**
     * Entry point, demonstrating {@link HardwareAbstractionLayer#getVirtualization()}.
     *
     * @param args Arguments, ignored.
     */
    @SuppressForbidden(reason = "Using System.out in a demo class")
    public static void main(String[] args) {
        Optional<String> virtualization = new SystemInfo().getHardware().getVirtualization();

        if (virtualization.isPresent()) {
            System.out.println("You appear to be on a virtualization or container platform: " + virtualization.get());
        } else {
            // An empty result is not proof of physical hardware, only that no known signature matched
            System.out.println("No virtualization or container platform was detected.");
        }
    }

    /**
     * The function attempts to identify the virtualization or container platform based on common signatures in the CPU
     * vendor string, computer manufacturer and model, and MAC address.
     *
     * @return A string naming the platform if it can be determined, or an empty string otherwise. The name may be a
     *         container runtime such as {@code LXC} rather than a hypervisor.
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
