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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class WindowsOperatingSystemFFM extends WindowsOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystemFFM.class);

    static {
        enableDebugPrivilege();
    }

    private static boolean enableDebugPrivilege() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment hTokenOut = arena.allocate(ADDRESS);

            MemorySegment hProcess = Kernel32FFM.getCurrentProcess();
            boolean success = Advapi32FFM.openProcessToken(hProcess,
                    WinNTFFM.TOKEN_QUERY | WinNTFFM.TOKEN_ADJUST_PRIVILEGES, hTokenOut);
            if (!success) {
                int err = Kernel32FFM.getLastError();
                LOG.error("OpenProcessToken failed, error: " + err);
                return false;
            }
            MemorySegment hToken = hTokenOut.get(ADDRESS, 0);

            MemorySegment luid = arena.allocate(WinNTFFM.LUID);
            success = Advapi32FFM.lookupPrivilegeValue("SeDebugPrivilege", luid, arena);
            if (!success) {
                int err = Kernel32FFM.getLastError();
                LOG.error("LookupPrivilegeValue failed, error: " + err);
                Kernel32FFM.closeHandle(hToken);
                return false;
            }

            MemorySegment tkp = arena.allocate(WinNTFFM.TOKEN_PRIVILEGES_1);
            tkp.set(JAVA_INT, 0, 1);
            tkp.asSlice(4, WinNTFFM.LUID.byteSize()).copyFrom(luid);
            tkp.setAtIndex(JAVA_INT, 3, WinNTFFM.SE_PRIVILEGE_ENABLED);
            success = Advapi32FFM.adjustTokenPrivileges(hToken, tkp);
            if (!success) {
                int err = Kernel32FFM.getLastError();
                LOG.error("AdjustTokenPrivileges failed, error: " + err);
                Kernel32FFM.closeHandle(hToken);
                return false;
            }

            Kernel32FFM.closeHandle(hToken);
            return true;
        } catch (Throwable t) {
            LOG.error("enableDebugPrivilege exception: " + t);
            return false;
        }
    }

    @Override
    public int getProcessId() {
        return Kernel32FFM.getCurrentProcessId();
    }

    @Override
    public long getSystemUptime() {
        return Kernel32FFM.querySystemUptime(isVistaOrGreater());
    }

    public static boolean isVistaOrGreater() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");

        if (!osName.startsWith("Windows")) {
            return false;
        }

        String[] parts = osVersion.split("\\.");
        if (parts.length >= 2) {
            int major = Integer.parseInt(parts[0]);
            return major >= 6;
        }
        return false;
    }
}
