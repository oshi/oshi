/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.ffm.windows.Advapi32FFM;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.WinNTFFM;
import oshi.ffm.windows.WindowsForeignFunctions;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Optional;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static oshi.ffm.windows.Kernel32FFM.GetLastError;
import static oshi.ffm.windows.WindowsForeignFunctions.setupTokenPrivileges;

public class WindowsOperatingSystemFFM extends WindowsOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystemFFM.class);

    private static final boolean IS_VISTA_OR_GREATER = WindowsForeignFunctions.isVistaOrGreater();

    static {
        enableDebugPrivilege();
    }

    private static boolean enableDebugPrivilege() {

        MemorySegment hToken = null;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment hTokenPtr = arena.allocate(ADDRESS);

            Optional<MemorySegment> hProcess = Kernel32FFM.GetCurrentProcess();
            if (hProcess.isEmpty()) {
                return false;
            }
            boolean success = Advapi32FFM.OpenProcessToken(hProcess.get(),
                    WinNTFFM.TOKEN_QUERY | WinNTFFM.TOKEN_ADJUST_PRIVILEGES, hTokenPtr);
            if (!success) {
                LOG.error("OpenProcessToken failed, error: " + GetLastError());
                return false;
            }
            hToken = hTokenPtr.get(ADDRESS, 0);

            MemorySegment luid = arena.allocate(WinNTFFM.LUID);
            success = Advapi32FFM.LookupPrivilegeValue("SeDebugPrivilege", luid, arena);
            if (!success) {
                LOG.error("LookupPrivilegeValue failed, error: " + GetLastError());
                return false;
            }

            MemorySegment tkp = setupTokenPrivileges(arena, luid);
            success = Advapi32FFM.AdjustTokenPrivileges(hToken, tkp);
            if (!success) {
                LOG.error("AdjustTokenPrivileges failed, error: " + GetLastError());
                return false;
            }

            return true;
        } catch (Throwable t) {
            LOG.error("enableDebugPrivilege exception: " + t);
            return false;
        } finally {
            if (hToken != null && hToken.address() != 0) {
                Kernel32FFM.CloseHandle(hToken);
            }
        }
    }

    @Override
    public int getProcessId() {
        return Kernel32FFM.GetCurrentProcessId().orElse(-1);
    }

    @Override
    public long getSystemUptime() {
        return querySystemUptime();
    }

    private static long querySystemUptime() {
        return IS_VISTA_OR_GREATER ? Kernel32FFM.GetTickCount64().orElse(-1) / 1000L
                : (long) Kernel32FFM.GetTickCount().orElse(-1) / 1000L;
    }
}
