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
        /** CURRENTVOLTAGE property. */
        CURRENTVOLTAGE,
        /** VOLTAGECAPS property. */
        VOLTAGECAPS
    }

    /**
     * Processor ID property.
     */
    public enum ProcessorIdProperty {
        /** The processor ID string. */
        PROCESSORID;
    }

    /**
     * Processor bitness property.
     */
    public enum BitnessProperty {
        /** The processor address width in bits. */
        ADDRESSWIDTH;
    }

    /**
     * Constructor.
     */
    protected Win32Processor() {
    }

    /**
     * Queries processor voltage.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return Processor voltage.
     */
    public static WmiResult<VoltProperty> queryVoltage(WmiQueryExecutor h) {
        WmiQuery<VoltProperty> query = new WmiQuery<>(WIN32_PROCESSOR, VoltProperty.class);
        return h.queryWMI(query);
    }

    /**
     * Queries processor ID.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return Processor ID.
     */
    public static WmiResult<ProcessorIdProperty> queryProcessorId(WmiQueryExecutor h) {
        WmiQuery<ProcessorIdProperty> query = new WmiQuery<>(WIN32_PROCESSOR, ProcessorIdProperty.class);
        return h.queryWMI(query);
    }

    /**
     * Queries processor bitness.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return Processor bitness.
     */
    public static WmiResult<BitnessProperty> queryBitness(WmiQueryExecutor h) {
        WmiQuery<BitnessProperty> query = new WmiQuery<>(WIN32_PROCESSOR, BitnessProperty.class);
        return h.queryWMI(query);
    }
}
