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
package oshi.jna.platform.windows;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.LOGICAL_PROCESSOR_RELATIONSHIP;

import oshi.jna.platform.windows.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX;

/**
 * Kernel32Util.
 */
public class Kernel32Util extends com.sun.jna.platform.win32.Kernel32Util {

    /**
     * Convenience method to get the processor information. Takes care of
     * auto-growing the array and populating variable-length arrays in
     * structures.
     * 
     * @param relationshipType
     *            The type of relationship to retrieve. This parameter can be
     *            one of the following values:
     *            {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationCache},
     *            {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationGroup},
     *            {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationNumaNode},
     *            {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorCore},
     *            {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorPackage},
     *            or {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationAll}
     * @return the array of processor information.
     */
    public static final SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] getLogicalProcessorInformationEx(
            int relationshipType) {
        WinDef.DWORDByReference bufferSize = new WinDef.DWORDByReference(new WinDef.DWORD(1));
        Memory memory;
        while (true) {
            memory = new Memory(bufferSize.getValue().intValue());
            if (!Kernel32.INSTANCE.GetLogicalProcessorInformationEx(relationshipType, memory, bufferSize)) {
                int err = Kernel32.INSTANCE.GetLastError();
                if (err != WinError.ERROR_INSUFFICIENT_BUFFER)
                    throw new Win32Exception(err);
            } else {
                break;
            }
        }
        // Array elements have variable size; iterate to populate array
        List<SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX> procInfoList = new ArrayList<>();
        int offset = 0;
        while (offset < bufferSize.getValue().intValue()) {
            SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX information = SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.fromPointer(memory.share(offset));
            procInfoList.add(information);
            offset += information.size;
        }
        return procInfoList.toArray(new SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[procInfoList.size()]);
    }
}
