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

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.LARGE_INTEGER;
import com.sun.jna.platform.win32.WinNT.PSID;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.win32.W32APITypeMapper;

/**
 * Windows Remote Desktop Services API. Formerly Terminal Services. This class
 * should be considered non-API as it may be removed if/when its code is
 * incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Wtsapi32 extends com.sun.jna.platform.win32.Wtsapi32 {
    Wtsapi32 INSTANCE = Native.loadLibrary("Wtsapi32", Wtsapi32.class, W32APIOptions.DEFAULT_OPTIONS);

    /**
     * Specifies the current server
     */
    HANDLE WTS_CURRENT_SERVER_HANDLE = new HANDLE(null);

    /**
     * Specifies the current session (SessionId)
     */
    int WTS_CURRENT_SESSION = -1;

    /**
     * Specifies any-session (SessionId)
     */
    int WTS_ANY_SESSION = -2;

    int WTS_PROCESS_INFO_LEVEL_0 = 0;
    int WTS_PROCESS_INFO_LEVEL_1 = 1;

    /**
     * Contains extended information about a process running on a Remote Desktop
     * Session Host (RD Session Host) server. This structure is returned by the
     * WTSEnumerateProcessesEx function when you set the pLevel parameter to
     * one.
     * 
     * @see <A HREF=
     *      "https://docs.microsoft.com/en-us/windows/desktop/api/wtsapi32/ns-wtsapi32-_wts_process_info_exa">WTS_PROCESS_INFO_EXA</A>
     * @see <A HREF=
     *      "https://docs.microsoft.com/en-us/windows/desktop/api/wtsapi32/ns-wtsapi32-_wts_process_info_exw">WTS_PROCESS_INFO_EXW</A>
     */
    class WTS_PROCESS_INFO_EX extends Structure {
        public int SessionId;
        public int ProcessId;
        public String pProcessName; // Either LPSTR or LPWSTR
        public PSID pUserSid;
        public int NumberOfThreads;
        public int HandleCount;
        public int PagefileUsage;
        public int PeakPagefileUsage;
        public int WorkingSetSize;
        public int PeakWorkingSetSize;
        public LARGE_INTEGER UserTime;
        public LARGE_INTEGER KernelTime;

        public WTS_PROCESS_INFO_EX() {
            super(W32APITypeMapper.DEFAULT);
        }

        public WTS_PROCESS_INFO_EX(Pointer p) {
            super(p, Structure.ALIGN_DEFAULT, W32APITypeMapper.DEFAULT);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "SessionId", "ProcessId", "pProcessName", "pUserSid", "NumberOfThreads",
                    "HandleCount", "PagefileUsage", "PeakPagefileUsage", "WorkingSetSize", "PeakWorkingSetSize",
                    "UserTime", "KernelTime" });
        }
    }

    /**
     * Retrieves information about the active processes on the specified Remote
     * Desktop Session Host (RD Session Host) server or Remote Desktop
     * Virtualization Host (RD Virtualization Host) server.
     * 
     * @param hServer
     *            A handle to an RD Session Host server. Specify a handle opened
     *            by the WTSOpenServer function, or specify
     *            WTS_CURRENT_SERVER_HANDLE to indicate the server on which your
     *            application is running.
     * @param pLevel
     *            A pointer to a DWORD variable that, on input, specifies the
     *            type of information to return. To return an array of
     *            WTS_PROCESS_INFO structures, specify zero. To return an array
     *            of WTS_PROCESS_INFO_EX structures, specify one. If you do not
     *            specify a valid value for this parameter, on output,
     *            WTSEnumerateProcessesEx sets this parameter to one and returns
     *            an error. Otherwise, on output, WTSEnumerateProcessesEx does
     *            not change the value of this parameter.
     * @param SessionID
     *            The session for which to enumerate processes. To enumerate
     *            processes for all sessions on the server, specify
     *            WTS_ANY_SESSION.
     * @param ppProcessInfo
     *            A pointer to a variable that receives a pointer to an array of
     *            WTS_PROCESS_INFO or WTS_PROCESS_INFO_EX structures. The type
     *            of structure is determined by the value passed to the pLevel
     *            parameter. Each structure in the array contains information
     *            about an active process. When you have finished using the
     *            array, free it by calling the WTSFreeMemoryEx function. You
     *            should also set the pointer to NULL.
     * @param pCount
     *            A pointer to a variable that receives the number of structures
     *            returned in the buffer referenced by the ppProcessInfo
     *            parameter.
     * @return If the function succeeds, the return value is a nonzero value. If
     *         the function fails, the return value is zero. To get extended
     *         error information, call the GetLastError function.
     */
    boolean WTSEnumerateProcessesEx(HANDLE hServer, IntByReference pLevel, int SessionID,
            PointerByReference ppProcessInfo, IntByReference pCount);

    /**
     * Frees memory that contains WTS_PROCESS_INFO_EX or WTS_SESSION_INFO_1
     * structures allocated by a Remote Desktop Services function.
     * 
     * @param WTSTypeClass
     *            A value of the WTS_TYPE_CLASS enumeration type that specifies
     *            the type of structures contained in the buffer referenced by
     *            the pMemory parameter.
     * @param pMemory
     *            A pointer to the buffer to free.
     * @param NumberOfEntries
     *            The number of elements in the buffer referenced by the pMemory
     *            parameter.
     * @return If the function succeeds, the return value is a nonzero value. If
     *         the function fails, the return value is zero. To get extended
     *         error information, call the GetLastError function.
     */
    boolean WTSFreeMemoryEx(int WTSTypeClass, Pointer pMemory, int NumberOfEntries);
}
