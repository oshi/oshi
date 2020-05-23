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

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.win32.W32APITypeMapper;

public interface Netapi32 extends com.sun.jna.platform.win32.Netapi32 {

    Netapi32 INSTANCE = Native.load("Netapi32", Netapi32.class, W32APIOptions.DEFAULT_OPTIONS);

    int MAX_PREFERRED_LENGTH = -1;

    /**
     * Contains information about the session, including name of the computer; name
     * of the user; and active and idle times for the session.
     */
    @FieldOrder({ "sesi10_cname", "sesi10_username", "sesi10_time", "sesi10_idle_time" })
    class SESSION_INFO_10 extends Structure {
        public String sesi10_cname;
        public String sesi10_username;
        public int sesi10_time;
        public int sesi10_idle_time;

        public SESSION_INFO_10() {
            super(W32APITypeMapper.UNICODE);
        }

        public SESSION_INFO_10(Pointer p) {
            super(p, Structure.ALIGN_DEFAULT, W32APITypeMapper.UNICODE);
            read();
        }
    }

    /**
     * Provides information about sessions established on a server.
     *
     * @param servername
     *            Pointer to a string that specifies the DNS or NetBIOS name of the
     *            remote server on which the function is to execute. If this
     *            parameter is NULL, the local computer is used.
     * @param UncClientName
     *            Pointer to a string that specifies the name of the computer
     *            session for which information is to be returned. If this parameter
     *            is NULL, NetSessionEnum returns information for all computer
     *            sessions on the server.
     * @param username
     *            Pointer to a string that specifies the name of the user for which
     *            information is to be returned. If this parameter is NULL,
     *            NetSessionEnum returns information for all users.
     * @param level
     *            Specifies the information level of the data. This parameter can be
     *            one of 0, 1, 2, 10, 502.
     * @param bufptr
     *            Pointer to the buffer that receives the data. The format of this
     *            data depends on the value of the level parameter, for example
     *            {@code SESSION_INFO_0} for level 0.
     *            <p>
     *            This buffer is allocated by the system and must be freed using the
     *            {@link #NetApiBufferFree} function. Note that you must free the
     *            buffer even if the function fails with {@code ERROR_MORE_DATA}.
     * @param prefmaxlen
     *            Specifies the preferred maximum length of returned data, in bytes.
     *            If you specify {@link #MAX_PREFERRED_LENGTH}, the function
     *            allocates the amount of memory required for the data. If you
     *            specify another value in this parameter, it can restrict the
     *            number of bytes that the function returns. If the buffer size is
     *            insufficient to hold all entries, the function returns
     *            {@code ERROR_MORE_DATA}.
     * @param entriesread
     *            Pointer to a value that receives the count of elements actually
     *            enumerated.
     * @param totalentries
     *            Pointer to a value that receives the total number of entries that
     *            could have been enumerated from the current resume position. Note
     *            that applications should consider this value only as a hint.
     * @param resume_handle
     *            Pointer to a value that contains a resume handle which is used to
     *            continue an existing session search. The handle should be zero on
     *            the first call and left unchanged for subsequent calls. If
     *            resume_handle is NULL, no resume handle is stored.
     * @return If the function succeeds, the return value is NERR_Success (0). If
     *         the function fails, the return value is an error code.
     */
    int NetSessionEnum(WString servername, WString UncClientName, WString username, int level,
            PointerByReference bufptr, int prefmaxlen, IntByReference entriesread, IntByReference totalentries,
            IntByReference resume_handle);
}
