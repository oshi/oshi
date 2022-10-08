/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import oshi.util.Util;

/**
 * Power profile stats. This class should be considered non-API as it may be removed if/when its code is incorporated
 * into the JNA project.
 */
public interface PowrProf extends com.sun.jna.platform.win32.PowrProf {
    /** Constant <code>INSTANCE</code> */
    PowrProf INSTANCE = Native.load("PowrProf", PowrProf.class);

    /**
     * Contains information about the current state of the system battery.
     */
    @FieldOrder({ "acOnLine", "batteryPresent", "charging", "discharging", "spare1", "tag", "maxCapacity",
            "remainingCapacity", "rate", "estimatedTime", "defaultAlert1", "defaultAlert2" })
    class SystemBatteryState extends Structure implements AutoCloseable {
        public byte acOnLine;
        public byte batteryPresent;
        public byte charging;
        public byte discharging;
        public byte[] spare1 = new byte[3];
        public byte tag;
        public int maxCapacity;
        public int remainingCapacity;
        public int rate;
        public int estimatedTime;
        public int defaultAlert1;
        public int defaultAlert2;

        public SystemBatteryState(Pointer p) {
            super(p);
            read();
        }

        public SystemBatteryState() {
            super();
        }

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    /**
     * Contains information about a processor.
     */
    @FieldOrder({ "number", "maxMhz", "currentMhz", "mhzLimit", "maxIdleState", "currentIdleState" })
    class ProcessorPowerInformation extends Structure {
        public int number;
        public int maxMhz;
        public int currentMhz;
        public int mhzLimit;
        public int maxIdleState;
        public int currentIdleState;

        public ProcessorPowerInformation(Pointer p) {
            super(p);
            read();
        }

        public ProcessorPowerInformation() {
            super();
        }
    }

    // MOVE?
    @FieldOrder({ "BatteryTag", "InformationLevel", "AtRate" })
    class BATTERY_QUERY_INFORMATION extends Structure implements AutoCloseable {
        public int BatteryTag;
        public int InformationLevel;
        public int AtRate;

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    enum BATTERY_QUERY_INFORMATION_LEVEL {
        BatteryInformation, BatteryGranularityInformation, BatteryTemperature, BatteryEstimatedTime, BatteryDeviceName,
        BatteryManufactureDate, BatteryManufactureName, BatteryUniqueID, BatterySerialNumber
    }

    @FieldOrder({ "Capabilities", "Technology", "Reserved", "Chemistry", "DesignedCapacity", "FullChargedCapacity",
            "DefaultAlert1", "DefaultAlert2", "CriticalBias", "CycleCount" })
    class BATTERY_INFORMATION extends Structure implements AutoCloseable {
        public int Capabilities;
        public byte Technology;
        public byte[] Reserved = new byte[3];
        public byte[] Chemistry = new byte[4];
        public int DesignedCapacity;
        public int FullChargedCapacity;
        public int DefaultAlert1;
        public int DefaultAlert2;
        public int CriticalBias;
        public int CycleCount;

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    @FieldOrder({ "BatteryTag", "Timeout", "PowerState", "LowCapacity", "HighCapacity" })
    class BATTERY_WAIT_STATUS extends Structure implements AutoCloseable {
        public int BatteryTag;
        public int Timeout;
        public int PowerState;
        public int LowCapacity;
        public int HighCapacity;

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    @FieldOrder({ "PowerState", "Capacity", "Voltage", "Rate" })
    class BATTERY_STATUS extends Structure implements AutoCloseable {
        public int PowerState;
        public int Capacity;
        public int Voltage;
        public int Rate;

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    @FieldOrder({ "Day", "Month", "Year" })
    class BATTERY_MANUFACTURE_DATE extends Structure implements AutoCloseable {
        public byte Day;
        public byte Month;
        public short Year;

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }
}
