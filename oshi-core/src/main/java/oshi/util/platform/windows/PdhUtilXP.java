/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.util.platform.windows;

import com.sun.jna.Memory; //NOSONAR
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Pdh;
import com.sun.jna.platform.win32.PdhMsg;
import com.sun.jna.platform.win32.PdhUtil.PdhException;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinError;

/**
 * TODO: This class is compatible with Windows XP and will be removed when the
 * JNA version of it is released.
 */
public class PdhUtilXP {
    private static final int CHAR_TO_BYTES = Boolean.getBoolean("w32.ascii") ? 1 : Native.WCHAR_SIZE;

    /**
     * Utility method to call Pdh's PdhLookupPerfNameByIndex that allocates the
     * required memory for the szNameBuffer parameter based on the type mapping
     * used, calls to PdhLookupPerfNameByIndex, and returns the received string.
     * 
     * @param szMachineName
     *            Null-terminated string that specifies the name of the computer
     *            where the specified performance object or counter is located.
     *            The computer name can be specified by the DNS name or the IP
     *            address. If NULL, the function uses the local computer.
     * @param dwNameIndex
     *            Index of the performance object or counter.
     * @return Returns the name of the performance object or counter.
     */
    public static String PdhLookupPerfNameByIndex(String szMachineName, int dwNameIndex) {
        // Call once with null buffer to get required buffer size
        DWORDByReference pcchNameBufferSize = new DWORDByReference(new DWORD(0));
        int result = Pdh.INSTANCE.PdhLookupPerfNameByIndex(szMachineName, dwNameIndex, null, pcchNameBufferSize);
        Memory mem = null;
        // Windows XP requires a non-null buffer and nonzero buffer size and
        // will return PDH_INVALID_ARGUMENT.
        if (result != PdhMsg.PDH_INVALID_ARGUMENT) {
            // Vista+ branch: use returned buffer size for second query
            if (result != WinError.ERROR_SUCCESS && result != Pdh.PDH_MORE_DATA) {
                throw new PdhException(result);
            }
            // Can't allocate 0 memory
            if (pcchNameBufferSize.getValue().intValue() < 1) {
                return "";
            }
            // Allocate buffer and call again
            mem = new Memory((long) pcchNameBufferSize.getValue().intValue() * CHAR_TO_BYTES);
            result = Pdh.INSTANCE.PdhLookupPerfNameByIndex(szMachineName, dwNameIndex, mem, pcchNameBufferSize);
        } else {
            // XP branch: try increasing buffer sizes until successful
            int bufferSize = 32;
            do {
                pcchNameBufferSize = new DWORDByReference(new DWORD(bufferSize));
                mem = new Memory((long) bufferSize * CHAR_TO_BYTES);
                result = Pdh.INSTANCE.PdhLookupPerfNameByIndex(szMachineName, dwNameIndex, mem, pcchNameBufferSize);
                if (result != PdhMsg.PDH_INVALID_ARGUMENT && result != PdhMsg.PDH_INSUFFICIENT_BUFFER) {
                    break;
                }
                bufferSize *= 2;
            } while (bufferSize <= Pdh.PDH_MAX_COUNTER_NAME);
        }
        if (result != WinError.ERROR_SUCCESS) {
            throw new PdhException(result);
        }

        // Convert buffer to Java String
        if (CHAR_TO_BYTES == 1) {
            return mem.getString(0);
        } else {
            return mem.getWideString(0);
        }
    }
}
