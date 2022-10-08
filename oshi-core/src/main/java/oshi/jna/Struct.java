/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna;

import com.sun.jna.platform.linux.LibC.Sysinfo;
import com.sun.jna.platform.mac.SystemB.HostCpuLoadInfo;
import com.sun.jna.platform.mac.SystemB.ProcTaskAllInfo;
import com.sun.jna.platform.mac.SystemB.ProcTaskInfo;
import com.sun.jna.platform.mac.SystemB.RUsageInfoV2;
import com.sun.jna.platform.mac.SystemB.Timeval;
import com.sun.jna.platform.mac.SystemB.VMStatistics;
import com.sun.jna.platform.mac.SystemB.VnodePathInfo;
import com.sun.jna.platform.mac.SystemB.XswUsage;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_IFROW;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_IF_ROW2;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_TCPSTATS;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_UDPSTATS;
import com.sun.jna.platform.win32.Pdh.PDH_RAW_COUNTER;
import com.sun.jna.platform.win32.Psapi.PERFORMANCE_INFORMATION;
import com.sun.jna.platform.win32.SetupApi.SP_DEVICE_INTERFACE_DATA;
import com.sun.jna.platform.win32.SetupApi.SP_DEVINFO_DATA;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;

import oshi.util.Util;

/**
 * Wrapper classes for JNA clases which extend {@link com.sun.jna.Structure} intended for use in try-with-resources
 * blocks.
 */
public interface Struct {
    /*
     * Linux
     */
    class CloseableSysinfo extends Sysinfo implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    /*
     * macOS
     */

    class CloseableHostCpuLoadInfo extends HostCpuLoadInfo implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableProcTaskInfo extends ProcTaskInfo implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableProcTaskAllInfo extends ProcTaskAllInfo implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableRUsageInfoV2 extends RUsageInfoV2 implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableTimeval extends Timeval implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableVMStatistics extends VMStatistics implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableVnodePathInfo extends VnodePathInfo implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableXswUsage extends XswUsage implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    /*
     * Windows
     */

    class CloseableMibIfRow extends MIB_IFROW implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableMibIfRow2 extends MIB_IF_ROW2 implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableMibTcpStats extends MIB_TCPSTATS implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableMibUdpStats extends MIB_UDPSTATS implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseablePdhRawCounter extends PDH_RAW_COUNTER implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseablePerformanceInformation extends PERFORMANCE_INFORMATION implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableSpDeviceInterfaceData extends SP_DEVICE_INTERFACE_DATA implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableSpDevinfoData extends SP_DEVINFO_DATA implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableSystemInfo extends SYSTEM_INFO implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }
}
