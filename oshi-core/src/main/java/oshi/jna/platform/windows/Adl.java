/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.windows;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * JNA bindings for the AMD Display Library (ADL) on Windows. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 */
public interface Adl {

    int ADL_OK = 0;
    int ADL_OVERDRIVE_VERSION_N = 8;
    int ADL_FAN_SPEED_MODE_PERCENT = 1;
    int ADL_OVERDRIVE_TEMPERATURE_EDGE = 1;

    /** ADL malloc callback: allocates memory and returns a pointer. */
    interface AdlMallocCallback extends Callback {
        Pointer invoke(int size);
    }

    interface AdlLibrary extends Library {
        int ADL2_Main_Control_Create(AdlMallocCallback callback, int iEnumConnectedAdapters,
                PointerByReference context);

        int ADL2_Main_Control_Destroy(Pointer context);

        int ADL2_Adapter_NumberOfAdapters_Get(Pointer context, IntByReference numAdapters);

        int ADL2_Adapter_AdapterInfo_Get(Pointer context, AdapterInfo[] info, int inputSize);

        int ADL2_Overdrive_Caps(Pointer context, int iAdapterIndex, IntByReference iSupported, IntByReference iEnabled,
                IntByReference iVersion);

        int ADL2_OverdriveN_Temperature_Get(Pointer context, int iAdapterIndex, int iTemperatureType,
                IntByReference iTemperature);

        int ADL2_OverdriveN_PerformanceStatus_Get(Pointer context, int iAdapterIndex,
                ADLODNPerformanceStatus perfStatus);

        int ADL2_OverdriveN_FanControl_Get(Pointer context, int iAdapterIndex, ADLODNFanControl fanControl);

        int ADL2_Overdrive6_CurrentPower_Get(Pointer context, int iAdapterIndex, int iPowerType,
                IntByReference lpCurrentValue);
    }

    @FieldOrder({ "iSize", "iAdapterIndex", "strAdapterName", "strDisplayName", "iPresent", "iExist", "iVendorID",
            "iBusNumber", "iDeviceNumber", "iFunctionNumber" })
    class AdapterInfo extends Structure {
        public int iSize;
        public int iAdapterIndex;
        public byte[] strAdapterName = new byte[256];
        public byte[] strDisplayName = new byte[256];
        public int iPresent;
        public int iExist;
        public int iVendorID;
        public int iBusNumber;
        public int iDeviceNumber;
        public int iFunctionNumber;
    }

    @FieldOrder({ "iGPUActivityPercent", "iCurrentCorePerformanceLevel", "iCurrentMemoryPerformanceLevel", "iCoreClock",
            "iMemoryClock", "iVDDC", "iCurrentBusSpeed", "iCurrentBusLanes", "iMaximumBusLanes", "iReserved" })
    class ADLODNPerformanceStatus extends Structure {
        public int iGPUActivityPercent;
        public int iCurrentCorePerformanceLevel;
        public int iCurrentMemoryPerformanceLevel;
        public int iCoreClock;
        public int iMemoryClock;
        public int iVDDC;
        public int iCurrentBusSpeed;
        public int iCurrentBusLanes;
        public int iMaximumBusLanes;
        public int iReserved;
    }

    @FieldOrder({ "iMode", "iFanControlFlag", "iCurrentFanSpeed", "iTargetFanSpeed", "iTargetTemperature",
            "iMinPerformanceClock", "iThrottlingRPM" })
    class ADLODNFanControl extends Structure {
        public int iMode;
        public int iFanControlFlag;
        public int iCurrentFanSpeed;
        public int iTargetFanSpeed;
        public int iTargetTemperature;
        public int iMinPerformanceClock;
        public int iThrottlingRPM;
    }
}
