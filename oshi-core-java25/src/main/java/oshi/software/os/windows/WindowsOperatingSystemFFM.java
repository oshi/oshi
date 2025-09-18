/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.ffm.windows.Advapi32FFM;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.PsapiFFM;
import oshi.ffm.windows.WinNTFFM;
import oshi.util.platform.windows.Advapi32UtilFFM;
import oshi.util.platform.windows.Kernel32UtilFFM;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.Optional;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.Kernel32FFM.GetLastError;
import static oshi.ffm.windows.WinNTFFM.PERFORMANCE_INFORMATION;
import static oshi.ffm.windows.WindowsForeignFunctions.setupTokenPrivileges;

public class WindowsOperatingSystemFFM extends WindowsOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystemFFM.class);

    private static final long BOOTTIME = Advapi32UtilFFM.querySystemBootTime();

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
    public boolean isElevated() {
        return Advapi32UtilFFM.isCurrentProcessElevated();
    }

    @Override
    public int getProcessId() {
        return Kernel32FFM.GetCurrentProcessId().orElse(-1);
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    @Override
    public long getSystemUptime() {
        return Kernel32UtilFFM.querySystemUptime();
    }

    public int getThreadCount() {

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment perfInfo = arena.allocate(PERFORMANCE_INFORMATION);
            int size = (int) PERFORMANCE_INFORMATION.byteSize();
            perfInfo.set(JAVA_INT, PERFORMANCE_INFORMATION.byteOffset(MemoryLayout.PathElement.groupElement("cb")),
                    size);
            if (!PsapiFFM.GetPerformanceInfo(perfInfo, size)) {
                LOG.error("Failed to get Performance Info. Error code: {}", GetLastError());
                return 0;
            }

            int threadCount = perfInfo.get(JAVA_INT,
                    PERFORMANCE_INFORMATION.byteOffset(MemoryLayout.PathElement.groupElement("ThreadCount")));
            return threadCount;
        } catch (Throwable t) {
            LOG.error("Exception getting thread count", t);
            return 0;
        }
    }

    @Override
    public int getThreadId() {
        return Kernel32FFM.GetCurrentThreadId().orElse(-1);
    }
}
