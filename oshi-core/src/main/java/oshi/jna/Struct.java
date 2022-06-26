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

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB.HostCpuLoadInfo;
import com.sun.jna.platform.mac.SystemB.ProcTaskAllInfo;
import com.sun.jna.platform.mac.SystemB.ProcTaskInfo;
import com.sun.jna.platform.mac.SystemB.VMStatistics;
import com.sun.jna.platform.mac.SystemB.VnodePathInfo;
import com.sun.jna.platform.win32.Pdh.PDH_RAW_COUNTER;
import com.sun.jna.platform.win32.Psapi.PERFORMANCE_INFORMATION;

/**
 * Wrapper classes for JNA clases which extend {@link com.sun.jna.Structure}
 * intended for use in try-with-resources blocks.
 */
public interface Struct {

    class CloseablePdhRawCounter extends PDH_RAW_COUNTER implements AutoCloseable {
        @Override
        public void close() {
            Pointer p = this.getPointer();
            if (p instanceof Memory) {
                ((Memory) p).close();
            }
        }
    }

    class CloseableHostCpuLoadInfo extends HostCpuLoadInfo implements AutoCloseable {
        @Override
        public void close() {
            Pointer p = this.getPointer();
            if (p instanceof Memory) {
                ((Memory) p).close();
            }
        }
    }

    class CloseableProcTaskInfo extends ProcTaskInfo implements AutoCloseable {
        @Override
        public void close() {
            Pointer p = this.getPointer();
            if (p instanceof Memory) {
                ((Memory) p).close();
            }
        }
    }

    class CloseableProcTaskAllInfo extends ProcTaskAllInfo implements AutoCloseable {
        @Override
        public void close() {
            Pointer p = this.getPointer();
            if (p instanceof Memory) {
                ((Memory) p).close();
            }
        }
    }

    class CloseableVnodePathInfo extends VnodePathInfo implements AutoCloseable {
        @Override
        public void close() {
            Pointer p = this.getPointer();
            if (p instanceof Memory) {
                ((Memory) p).close();
            }
        }
    }

    class CloseablePerformanceInformation extends PERFORMANCE_INFORMATION implements AutoCloseable {
        @Override
        public void close() {
            Pointer p = this.getPointer();
            if (p instanceof Memory) {
                ((Memory) p).close();
            }
        }
    }

    class CloseableVMStatistics extends VMStatistics implements AutoCloseable {
        @Override
        public void close() {
            Pointer p = this.getPointer();
            if (p instanceof Memory) {
                ((Memory) p).close();
            }
        }
    }
}
