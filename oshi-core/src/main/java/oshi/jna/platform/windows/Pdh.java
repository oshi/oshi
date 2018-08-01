/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.jna.platform.windows;

import com.sun.jna.Native; // NOSONAR
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * Windows Performance Data Helper (a.k.a. PDH). This class should be considered
 * non-API as it may be removed if/when its code is incorporated into the JNA
 * project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Pdh extends com.sun.jna.platform.win32.Pdh {
    Pdh INSTANCE = Native.loadLibrary("Pdh", Pdh.class, W32APIOptions.DEFAULT_OPTIONS);
    int PDH_MORE_DATA = 0x800007D2;
    int PDH_INVALID_ARGUMENT = 0xC0000BBD;
    int PDH_MEMORY_ALLOCATION_FAILURE = 0xC0000BBB;
    int PDH_CSTATUS_NO_MACHINE = 0x800007D0;
    int PDH_CSTATUS_NO_OBJECT = 0xC0000BB8;

    /**
     * Returns the specified object's counter and instance names that exist on
     * the specified computer or in the specified log file.
     * 
     * @param szDataSource
     *            String that specifies the name of the log file used to
     *            enumerate the counter and instance names. If NULL, the
     *            function uses the computer specified in the szMachineName
     *            parameter to enumerate the names.
     * @param szMachineName
     *            String that specifies the name of the computer that contains
     *            the counter and instance names that you want to enumerate.
     *            Include the leading slashes in the computer name, for example,
     *            \\computername. If the szDataSource parameter is NULL, you can
     *            set szMachineName to NULL to specify the local computer.
     * @param szObjectName
     *            String that specifies the name of the object whose counter and
     *            instance names you want to enumerate.
     * @param mszCounterList
     *            Caller-allocated buffer that receives a list of
     *            null-terminated counter names provided by the specified
     *            object. The list contains unique counter names. The list is
     *            terminated by two NULL characters. Set to NULL if the
     *            pcchCounterListLengthparameter is zero.
     * @param pcchCounterListLength
     *            Size of the mszCounterList buffer, in TCHARs. If zero on input
     *            and the object exists, the function returns PDH_MORE_DATA and
     *            sets this parameter to the required buffer size. If the buffer
     *            is larger than the required size, the function sets this
     *            parameter to the actual size of the buffer that was used. If
     *            the specified size on input is greater than zero but less than
     *            the required size, you should not rely on the returned size to
     *            reallocate the buffer.
     * @param mszInstanceList
     *            Caller-allocated buffer that receives a list of
     *            null-terminated instance names provided by the specified
     *            object. The list contains unique instance names. The list is
     *            terminated by two NULL characters. Set to NULL if
     *            pcchInstanceListLength is zero.
     * @param pcchInstanceListLength
     *            Size of the mszInstanceList buffer, in TCHARs. If zero on
     *            input and the object exists, the function returns
     *            PDH_MORE_DATA and sets this parameter to the required buffer
     *            size. If the buffer is larger than the required size, the
     *            function sets this parameter to the actual size of the buffer
     *            that was used. If the specified size on input is greater than
     *            zero but less than the required size, you should not rely on
     *            the returned size to reallocate the buffer. If the specified
     *            object does not support variable instances, then the returned
     *            value will be zero. If the specified object does support
     *            variable instances, but does not currently have any instances,
     *            then the value returned is 2, which is the size of an empty
     *            MULTI_SZ list string.
     * @param dwDetailLevel
     *            Detail level of the performance items to return. All items
     *            that are of the specified detail level or less will be
     *            returned.
     * @param dwFlags
     *            This parameter must be zero.
     * @return If the function succeeds, it returns ERROR_SUCCESS. If the
     *         function fails, the return value is a system error code or a PDH
     *         error code.
     * @see <A HREF=
     *      "https://msdn.microsoft.com/en-us/library/windows/desktop/aa372677(v=vs.85).aspx">
     *      PdhEnumObjectItems</A>
     */
    int PdhEnumObjectItems(String szDataSource, String szMachineName, String szObjectName, Pointer mszCounterList,
            DWORDByReference pcchCounterListLength, Pointer mszInstanceList, DWORDByReference pcchInstanceListLength,
            int dwDetailLevel, int dwFlags);

    /**
     * Returns the counter index corresponding to the specified counter name.
     * 
     * @param szMachineName
     *            Null-terminated string that specifies the name of the computer
     *            where the specified counter is located. The computer name can
     *            be specified by the DNS name or the IP address. If NULL, the
     *            function uses the local computer.
     * @param szNameBuffer
     *            Null-terminated string that contains the counter name.
     * @param pdwIndex
     *            Index of the counter.
     * @return If the function succeeds, it returns ERROR_SUCCESS. If the
     *         function fails, the return value is a system error code or a PDH
     *         error code.
     * @see <A HREF=
     *      "https://msdn.microsoft.com/en-us/library/windows/desktop/aa372647(v=vs.85).aspx">
     *      PdhLookupPerfIndexByName</A>
     */
    int PdhLookupPerfIndexByName(String szMachineName, String szNameBuffer, DWORDByReference pdwIndex);

    /**
     * Returns the performance object name or counter name corresponding to the
     * specified index.
     * 
     * @param szMachineName
     *            Null-terminated string that specifies the name of the computer
     *            where the specified performance object or counter is located.
     *            The computer name can be specified by the DNS name or the IP
     *            address. If NULL, the function uses the local computer.
     * @param dwNameIndex
     *            Index of the performance object or counter.
     * @param szNameBuffer
     *            Caller-allocated buffer that receives the null-terminated name
     *            of the performance object or counter. Set to NULL if
     *            pcchNameBufferSize is zero.
     * @param pcchNameBufferSize
     *            Size of the szNameBuffer buffer, in TCHARs. If zero on input,
     *            the function returns PDH_MORE_DATA and sets this parameter to
     *            the required buffer size. If the buffer is larger than the
     *            required size, the function sets this parameter to the actual
     *            size of the buffer that was used. If the specified size on
     *            input is greater than zero but less than the required size,
     *            you should not rely on the returned size to reallocate the
     *            buffer.
     * @return If the function succeeds, it returns ERROR_SUCCESS. If the
     *         function fails, the return value is a system error code or a PDH
     *         error code.
     * @see <A HREF=
     *      "https://msdn.microsoft.com/en-us/library/windows/desktop/aa372648(v=vs.85).aspx">
     *      PdhLookupPerfNameByIndex</A>
     */
    int PdhLookupPerfNameByIndex(String szMachineName, int dwNameIndex, Pointer szNameBuffer,
            DWORDByReference pcchNameBufferSize);
}
