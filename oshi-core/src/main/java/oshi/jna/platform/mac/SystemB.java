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
package oshi.jna.platform.mac;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import oshi.jna.platform.unix.CLibrary;

/**
 * System class. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 */
public interface SystemB extends com.sun.jna.platform.mac.SystemB, CLibrary {

    SystemB INSTANCE = Native.load("System", SystemB.class);

    int UTX_USERSIZE = 256;
    int UTX_LINESIZE = 32;
    int UTX_IDSIZE = 4;
    int UTX_HOSTSIZE = 256;

    @FieldOrder({ "ut_user", "ut_id", "ut_line", "ut_pid", "ut_type", "ut_tv", "ut_host", "ut_pad" })
    class MacUtmpx extends Structure {
        public byte[] ut_user = new byte[UTX_USERSIZE]; // login name
        public byte[] ut_id = new byte[UTX_IDSIZE]; // id
        public byte[] ut_line = new byte[UTX_LINESIZE]; // tty name
        public int ut_pid; // process id creating the entry
        public short ut_type; // type of this entry
        public Timeval ut_tv; // time entry was created
        public byte[] ut_host = new byte[UTX_HOSTSIZE]; // host name
        public byte[] ut_pad = new byte[16]; // reserved for future use
    }

    /**
     * Reads a line from the current file position in the utmp file. It returns a
     * pointer to a structure containing the fields of the line.
     * <p>
     * Not thread safe
     *
     * @return a {@link MacUtmpx} on success, and NULL on failure (which includes
     *         the "record not found" case)
     */
    MacUtmpx getutxent();

}
