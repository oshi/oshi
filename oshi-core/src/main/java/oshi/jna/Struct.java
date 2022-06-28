/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
import com.sun.jna.platform.win32.WinNT.LUID;

import oshi.util.Util;

/**
 * Wrapper classes for JNA clases which extend {@link com.sun.jna.Structure}
 * intended for use in try-with-resources blocks.
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

    class CloseableLUID extends LUID implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }
}
