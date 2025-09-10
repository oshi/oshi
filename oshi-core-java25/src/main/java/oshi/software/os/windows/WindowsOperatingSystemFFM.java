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
import oshi.ffm.windows.WindowsForeignFunctions;
import oshi.ffm.windows.WinNTFFM;
import oshi.util.GlobalConfig;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.Kernel32FFM.GetLastError;
import static oshi.ffm.windows.WinNTFFM.PERFORMANCE_INFORMATION;
import static oshi.ffm.windows.WindowsForeignFunctions.setupTokenPrivileges;
import static oshi.util.Memoizer.memoize;

public class WindowsOperatingSystemFFM extends WindowsOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystemFFM.class);

    private static Supplier<String> systemLog = memoize(WindowsOperatingSystemFFM::querySystemLog,
            TimeUnit.HOURS.toNanos(1));

    private static final long BOOTTIME = querySystemBootTime();

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
        return Advapi32FFM.isCurrentProcessElevated();
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
        return querySystemUptime();
    }

    private static long querySystemUptime() {
        return Kernel32FFM.GetTickCount().orElse(-1) / 1000L;
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

    private static long querySystemBootTime() {
        String eventLog = systemLog.get();
        try (Arena arena = Arena.ofConfined()) {
            Optional<MemorySegment> hEventLogOpt = Advapi32FFM.OpenEventLog(arena, eventLog);
            if (hEventLogOpt.isEmpty()) {
                LOG.warn("Unable to open system Event Log. Falling back to uptime.");
                return System.currentTimeMillis() / 1000L - querySystemUptime();
            }

            MemorySegment hEventLog = hEventLogOpt.get();

            int bufSize = 64 * 1024;
            MemorySegment buffer = arena.allocate(bufSize);
            MemorySegment bytesRead = arena.allocate(JAVA_INT);
            MemorySegment minBytesNeeded = arena.allocate(JAVA_INT);

            long event6005Time = 0L;

            long OFFSET_EVENTID = WinNTFFM.OFFSET_EVENTID;
            long OFFSET_TIME_GENERATED = WinNTFFM.OFFSET_TIME_GENERATED;
            long OFFSET_LENGTH = WinNTFFM.OFFSET_LENGTH;

            while (Advapi32FFM.ReadEventLog(hEventLog,
                    WinNTFFM.EVENTLOG_BACKWARDS_READ | WinNTFFM.EVENTLOG_SEQUENTIAL_READ, buffer, bufSize, bytesRead,
                    minBytesNeeded)) {

                int read = bytesRead.get(JAVA_INT, 0);
                int offset = 0;

                while (offset < read) {
                    MemorySegment record = buffer.asSlice(offset, WinNTFFM.EVENTLOGRECORD.byteSize());

                    int eventId = record.get(JAVA_INT, (int) OFFSET_EVENTID);
                    long timeGenerated = Integer.toUnsignedLong(record.get(JAVA_INT, (int) OFFSET_TIME_GENERATED));

                    if (eventId == 12) { // system boot
                        Advapi32FFM.CloseEventLog(hEventLog);
                        return timeGenerated;
                    } else if (eventId == 6005) { // event log startup
                        if (event6005Time > 0) {
                            Advapi32FFM.CloseEventLog(hEventLog);
                            return event6005Time;
                        }
                        event6005Time = timeGenerated;
                    }

                    // Advance to next record
                    int length = record.get(JAVA_INT, (int) OFFSET_LENGTH);
                    offset += length;
                }
            }

            Advapi32FFM.CloseEventLog(hEventLog);

            if (event6005Time > 0) {
                return event6005Time;
            }
        }

        // Fallback: approximate boot time from uptime
        return System.currentTimeMillis() / 1000L - querySystemUptime();
    }

    private static String querySystemLog() {
        String systemLog = GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_EVENTLOG, "System");
        if (systemLog.isEmpty()) {
            return null;
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sourceName = WindowsForeignFunctions.toWideString(arena, systemLog);

            Optional<MemorySegment> hEventLog = Advapi32FFM.OpenEventLog(MemorySegment.NULL, sourceName);
            if (hEventLog.isEmpty()) {
                LOG.warn("Unable to open configured system Event log \"{}\". Calculating boot time from uptime.",
                        systemLog);
                return null;
            }

            return systemLog;
        } catch (Throwable t) {
            LOG.error("Exception while opening system event log", t);
            return null;
        }
    }
}
