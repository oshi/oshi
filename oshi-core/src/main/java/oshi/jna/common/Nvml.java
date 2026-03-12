/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.common;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * JNA bindings for the NVIDIA Management Library (NVML). This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 */
public interface Nvml {

    int NVML_SUCCESS = 0;
    int NVML_TEMPERATURE_GPU = 0;
    int NVML_CLOCK_GRAPHICS = 0;
    int NVML_CLOCK_MEM = 2;
    int NVML_DEVICE_NAME_BUFFER_SIZE = 96;

    interface NvmlLibrary extends Library {
        int nvmlInit_v2();

        int nvmlShutdown();

        int nvmlDeviceGetCount_v2(IntByReference deviceCount);

        int nvmlDeviceGetHandleByIndex_v2(int index, PointerByReference device);

        int nvmlDeviceGetName(Pointer device, byte[] name, int length);

        int nvmlDeviceGetPciInfo_v3(Pointer device, NvmlPciInfo pci);

        int nvmlDeviceGetUtilizationRates(Pointer device, NvmlUtilization utilization);

        int nvmlDeviceGetMemoryInfo(Pointer device, NvmlMemory memory);

        int nvmlDeviceGetTemperature(Pointer device, int sensorType, IntByReference temp);

        int nvmlDeviceGetPowerUsage(Pointer device, IntByReference power);

        int nvmlDeviceGetClockInfo(Pointer device, int clockType, IntByReference clock);

        int nvmlDeviceGetFanSpeed(Pointer device, IntByReference speed);
    }

    @FieldOrder({ "gpu", "memory" })
    class NvmlUtilization extends Structure {
        public int gpu;
        public int memory;
    }

    @FieldOrder({ "total", "used", "free" })
    class NvmlMemory extends Structure {
        public long total;
        public long used;
        public long free;
    }

    @FieldOrder({ "busIdLegacy", "domain", "bus", "device", "pciDeviceId", "pciSubSystemId", "busId" })
    class NvmlPciInfo extends Structure {
        public byte[] busIdLegacy = new byte[16];
        public int domain;
        public int bus;
        public int device;
        public int pciDeviceId;
        public int pciSubSystemId;
        public byte[] busId = new byte[32];
    }
}
