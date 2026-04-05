/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.linux.UdevFunctions;

/**
 * FFM-based Linux operating system implementation.
 * <p>
 * Extends {@link LinuxOperatingSystem}, overriding methods to use FFM implementations as they become available.
 * <p>
 * Udev availability is determined by {@link UdevFunctions#isAvailable()}, which checks both the
 * {@code oshi.os.linux.allowudev} configuration property and whether libudev could be loaded and all symbols bound. FFM
 * consumer classes should import {@link #HAS_UDEV} from this class rather than from {@link LinuxOperatingSystem}.
 */
@ThreadSafe
public class LinuxOperatingSystemFFM extends LinuxOperatingSystem {

    /**
     * Identifies if the udev library was successfully loaded and all symbols bound via FFM. Also respects the
     * {@code oshi.os.linux.allowudev} configuration property, consistent with the JNA-based
     * {@link LinuxOperatingSystem#HAS_UDEV}.
     */
    public static final boolean HAS_UDEV = UdevFunctions.isAvailable();
}
