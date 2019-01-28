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

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinError;



/**
 * Kernel32 and WinNT info submitted to JNA as #1050. This class should be
 * considered non-API as it may be removed if/when its code is incorporated into
 * the JNA project.
 */
public interface Kernel32 extends com.sun.jna.platform.win32.Kernel32 {
    Kernel32 INSTANCE = Native.load("Kernel32", Kernel32.class);

    /*
     * WIN32_WINNT version constants
     */
    short WIN32_WINNT_NT4 = 0x0400; // Windows NT 4.0
    short WIN32_WINNT_WIN2K = 0x0500; // Windows 2000
    short WIN32_WINNT_WINXP = 0x0501; // Windows XP
    short WIN32_WINNT_WS03 = 0x0502; // Windows Server 2003
    short WIN32_WINNT_WIN6 = 0x0600; // Windows Vista
    short WIN32_WINNT_VISTA = 0x0600; // Windows Vista
    short WIN32_WINNT_WS08 = 0x0600; // Windows Server 2008
    short WIN32_WINNT_LONGHORN = 0x0600; // Windows Vista
    short WIN32_WINNT_WIN7 = 0x0601; // Windows 7
    short WIN32_WINNT_WIN8 = 0x0602; // Windows 8
    short WIN32_WINNT_WINBLUE = 0x0603; // Windows 8.1
    short WIN32_WINNT_WINTHRESHOLD = 0x0A00; // Windows 10
    short WIN32_WINNT_WIN10 = 0x0A00; // Windows 10

    /**
     * Compares a set of operating system version requirements to the
     * corresponding values for the currently running version of the system.This
     * function is subject to manifest-based behavior.
     * 
     * @param lpVersionInformation
     *            A pointer to an OSVERSIONINFOEX structure containing the
     *            operating system version requirements to compare. The
     *            dwTypeMask parameter indicates the members of this structure
     *            that contain information to compare.
     * 
     *            You must set the dwOSVersionInfoSize member of this structure
     *            to sizeof(OSVERSIONINFOEX). You must also specify valid data
     *            for the members indicated by dwTypeMask. The function ignores
     *            structure members for which the corresponding dwTypeMask bit
     *            is not set.
     * @param dwTypeMask
     *            A mask that indicates the members of the OSVERSIONINFOEX
     *            structure to be tested.
     * @param dwlConditionMask
     *            The type of comparison to be used for each lpVersionInfo
     *            member being compared. To build this value, call the
     *            VerSetConditionMask function once for each OSVERSIONINFOEX
     *            member being compared.
     * @return If the currently running operating system satisfies the specified
     *         requirements, the return value is a nonzero value.
     * 
     *         If the current system does not satisfy the requirements, the
     *         return value is zero and GetLastError returns
     *         ERROR_OLD_WIN_VERSION.
     * 
     *         If the function fails, the return value is zero and GetLastError
     *         returns an error code other than ERROR_OLD_WIN_VERSION.
     */
    boolean VerifyVersionInfoW(OSVERSIONINFOEX lpVersionInformation, int dwTypeMask, long dwlConditionMask);

    /**
     * Sets the bits of a 64-bit value to indicate the comparison operator to
     * use for a specified operating system version attribute. This function is
     * used to build the dwlConditionMask parameter of the VerifyVersionInfo
     * function.
     * 
     * @param conditionMask
     *            A value to be passed as the dwlConditionMask parameter of the
     *            VerifyVersionInfo function. The function stores the comparison
     *            information in the bits of this variable.
     * 
     *            Before the first call to VerSetCondition, initialize this
     *            variable to zero. For subsequent calls, pass in the variable
     *            used in the previous call.
     * @param typeMask
     *            A mask that indicates the member of the OSVERSIONINFOEX
     *            structure whose comparison operator is being set. This value
     *            corresponds to one of the bits specified in the dwTypeMask
     *            parameter for the VerifyVersionInfo function.
     * @param condition
     *            The operator to be used for the comparison. The
     *            VerifyVersionInfo function uses this operator to compare a
     *            specified attribute value to the corresponding value for the
     *            currently running system.
     * @return The function returns the condition mask value.
     */
    long VerSetConditionMask(long conditionMask, int typeMask, byte condition);

    /**
     * Retrieves information about the relationships of logical processors and
     * related hardware.
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
     * @param buffer
     *            A pointer to a buffer that receives an array of
     *            {@link WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX}
     *            structures. If the function fails, the contents of this buffer
     *            are undefined.
     * @param returnedLength
     *            On input, specifies the length of the buffer pointed to by
     *            Buffer, in bytes. If the buffer is large enough to contain all
     *            of the data, this function succeeds and ReturnedLength is set
     *            to the number of bytes returned. If the buffer is not large
     *            enough to contain all of the data, the function fails,
     *            GetLastError returns
     *            {@link WinError#ERROR_INSUFFICIENT_BUFFER}, and ReturnedLength
     *            is set to the buffer length required to contain all of the
     *            data. If the function fails with an error other than
     *            {@link WinError#ERROR_INSUFFICIENT_BUFFER}, the value of
     *            ReturnedLength is undefined.
     * @return If the function succeeds, the return value is {@code TRUE} and at
     *         least one {@link WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX}
     *         structure is written to the output buffer.
     *         <p>
     *         If the function fails, the return value is {@code FALSE}. To get
     *         extended error information, call {@link #GetLastError()}.
     */
    boolean GetLogicalProcessorInformationEx(int relationshipType, Memory buffer, DWORDByReference returnedLength);
}
