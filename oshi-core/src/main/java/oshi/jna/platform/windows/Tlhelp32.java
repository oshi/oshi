/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.jna.platform.windows;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

public interface Tlhelp32 extends com.sun.jna.platform.win32.Tlhelp32 {

    /**
     * Describes an entry from a list of the threads executing in the system when a
     * snapshot was taken.
     */
    @FieldOrder({ "dwSize", "cntUsage", "th32ThreadID", "th32OwnerProcessID", "tpBasePri", "tpDeltaPri", "dwFlags" })
    class THREADENTRY32 extends Structure {

        public static class ByReference extends THREADENTRY32 implements Structure.ByReference {
            public ByReference() {
            }

            public ByReference(Pointer memory) {
                super(memory);
            }
        }

        /**
         * The size of the structure, in bytes. Before calling the Thread32First
         * function, set this member to sizeof(THREADENTRY32). If you do not initialize
         * dwSize, Thread32First fails.
         */
        int dwSize;

        /**
         * This member is no longer used and is always set to zero.
         */
        int cntUsage;

        /**
         * The thread identifier, compatible with the thread identifier returned by the
         * CreateProcess function.
         */
        int th32ThreadID;

        /**
         * The identifier of the process that created the thread.
         */
        int th32OwnerProcessID;

        /**
         * The kernel base priority level assigned to the thread. The priority is a
         * number from 0 to 31, with 0 representing the lowest possible thread priority.
         * For more information, see KeQueryPriorityThread.
         */
        NativeLong tpBasePri;

        /**
         * This member is no longer used and is always set to zero.
         */
        NativeLong tpDeltaPri;

        /**
         * This member is no longer used and is always set to zero.
         */
        int dwFlags;

        public THREADENTRY32() {
            dwSize = size();
        }

        public THREADENTRY32(Pointer memory) {
            super(memory);
            read();
        }
    }
}
