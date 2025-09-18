/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.ffm.windows.Advapi32FFM;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.WinNTFFM;
import oshi.ffm.windows.WindowsForeignFunctions;
import oshi.util.GlobalConfig;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.Advapi32FFM.GetTokenInformation;
import static oshi.ffm.windows.Advapi32FFM.OpenProcessToken;
import static oshi.util.Memoizer.memoize;

public final class Advapi32UtilFFM {

    private Advapi32UtilFFM() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(Advapi32UtilFFM.class);

    private static Supplier<String> systemLog = memoize(Advapi32UtilFFM::querySystemLog, TimeUnit.HOURS.toNanos(1));

    public static boolean isCurrentProcessElevated() {
        try (Arena arena = Arena.ofConfined()) {

            Optional<MemorySegment> hProcessOpt = Kernel32FFM.GetCurrentProcess();
            if (hProcessOpt.isEmpty()) {
                return false;
            }
            MemorySegment hProcess = hProcessOpt.get();

            MemorySegment hTokenPtr = arena.allocate(ADDRESS);

            if (!OpenProcessToken(hProcess, WinNTFFM.TOKEN_QUERY, hTokenPtr)) {
                return false;
            }

            MemorySegment hToken = hTokenPtr.get(ADDRESS, 0);

            try {
                MemorySegment elevation = arena.allocate(WinNTFFM.TOKEN_ELEVATION);
                MemorySegment returnLength = arena.allocate(JAVA_INT);

                boolean success = GetTokenInformation(hToken, WinNTFFM.TokenElevation, elevation,
                        (int) WinNTFFM.TOKEN_ELEVATION.byteSize(), returnLength);

                if (!success) {
                    return false;
                }

                int tokenIsElevated = elevation.get(JAVA_INT,
                        WinNTFFM.TOKEN_ELEVATION.byteOffset(MemoryLayout.PathElement.groupElement("TokenIsElevated")));

                return tokenIsElevated > 0;
            } finally {
                Kernel32FFM.CloseHandle(hToken);
            }
        } catch (Throwable t) {
            LOG.debug("Advapi32FFM.isCurrentProcessElevated failed", t);
            return false;
        }
    }

    public static long querySystemBootTime() {
        String eventLog = systemLog.get();
        try (Arena arena = Arena.ofConfined()) {
            Optional<MemorySegment> hEventLogOpt = Advapi32FFM.OpenEventLog(arena, eventLog);
            if (hEventLogOpt.isEmpty()) {
                LOG.warn("Unable to open system Event Log. Falling back to uptime.");
                return System.currentTimeMillis() / 1000L - Kernel32UtilFFM.querySystemUptime();
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
        } catch (Throwable t) {
            LOG.error("Exception while querying system boottime, fallback to boot time from uptime", t);
        }
        // Fallback: approximate boot time from uptime
        return System.currentTimeMillis() / 1000L - Kernel32UtilFFM.querySystemUptime();
    }

    public static String querySystemLog() {
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
