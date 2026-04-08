/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

/**
 * FFM mappings for Windows battery structures from PowrProf / batclass.h.
 */
public final class PowrProfFFM extends WindowsForeignFunctions {

    // BATTERY_QUERY_INFORMATION: BatteryTag (DWORD) + InformationLevel (DWORD) + AtRate (LONG)
    public static final StructLayout BATTERY_QUERY_INFORMATION = structLayout(JAVA_INT.withName("BatteryTag"),
            JAVA_INT.withName("InformationLevel"), JAVA_INT.withName("AtRate"));

    // BATTERY_INFORMATION
    public static final StructLayout BATTERY_INFORMATION = structLayout(JAVA_INT.withName("Capabilities"),
            JAVA_BYTE.withName("Technology"), sequenceLayout(3, JAVA_BYTE).withName("Reserved"),
            sequenceLayout(4, JAVA_BYTE).withName("Chemistry"), JAVA_INT.withName("DesignedCapacity"),
            JAVA_INT.withName("FullChargedCapacity"), JAVA_INT.withName("DefaultAlert1"),
            JAVA_INT.withName("DefaultAlert2"), JAVA_INT.withName("CriticalBias"), JAVA_INT.withName("CycleCount"));

    // BATTERY_WAIT_STATUS
    public static final StructLayout BATTERY_WAIT_STATUS = structLayout(JAVA_INT.withName("BatteryTag"),
            JAVA_INT.withName("Timeout"), JAVA_INT.withName("PowerState"), JAVA_INT.withName("LowCapacity"),
            JAVA_INT.withName("HighCapacity"));

    // BATTERY_STATUS
    public static final StructLayout BATTERY_STATUS = structLayout(JAVA_INT.withName("PowerState"),
            JAVA_INT.withName("Capacity"), JAVA_INT.withName("Voltage"), JAVA_INT.withName("Rate"));

    // BATTERY_MANUFACTURE_DATE: Day (BYTE) + Month (BYTE) + Year (WORD)
    public static final StructLayout BATTERY_MANUFACTURE_DATE = structLayout(JAVA_BYTE.withName("Day"),
            JAVA_BYTE.withName("Month"), JAVA_SHORT.withName("Year"));

    public static final long OFF_BQI_TAG = offset(BATTERY_QUERY_INFORMATION, "BatteryTag");
    public static final long OFF_BQI_LEVEL = offset(BATTERY_QUERY_INFORMATION, "InformationLevel");
    public static final long OFF_BQI_ATRATE = offset(BATTERY_QUERY_INFORMATION, "AtRate");
    public static final long OFF_BI_CAPABILITIES = offset(BATTERY_INFORMATION, "Capabilities");
    public static final long OFF_BI_CHEMISTRY = offset(BATTERY_INFORMATION, "Chemistry");
    public static final long OFF_BI_DESIGNED = offset(BATTERY_INFORMATION, "DesignedCapacity");
    public static final long OFF_BI_FULL = offset(BATTERY_INFORMATION, "FullChargedCapacity");
    public static final long OFF_BI_CYCLE = offset(BATTERY_INFORMATION, "CycleCount");
    public static final long OFF_BWS_TAG = offset(BATTERY_WAIT_STATUS, "BatteryTag");
    public static final long OFF_BS_POWERSTATE = offset(BATTERY_STATUS, "PowerState");
    public static final long OFF_BS_CAPACITY = offset(BATTERY_STATUS, "Capacity");
    public static final long OFF_BS_VOLTAGE = offset(BATTERY_STATUS, "Voltage");
    public static final long OFF_BS_RATE = offset(BATTERY_STATUS, "Rate");
    public static final long OFF_BMD_DAY = offset(BATTERY_MANUFACTURE_DATE, "Day");
    public static final long OFF_BMD_MONTH = offset(BATTERY_MANUFACTURE_DATE, "Month");
    public static final long OFF_BMD_YEAR = offset(BATTERY_MANUFACTURE_DATE, "Year");

    private static long offset(StructLayout layout, String name) {
        return layout.byteOffset(MemoryLayout.PathElement.groupElement(name));
    }
}
