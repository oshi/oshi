/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enums for WMI class {@code Win32_Processor}.
 */
@ThreadSafe
public class Win32Processor {

    /**
     * The WMI class name.
     */
    public static final String WIN32_PROCESSOR = "Win32_Processor";

    /**
     * Processor voltage properties.
     */
    public enum VoltProperty {
        CURRENTVOLTAGE, VOLTAGECAPS;
    }

    /**
     * Processor ID property.
     */
    public enum ProcessorIdProperty {
        PROCESSORID;
    }

    /**
     * Processor bitness property.
     */
    public enum BitnessProperty {
        ADDRESSWIDTH;
    }

    protected Win32Processor() {
    }
}
