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
import oshi.ffm.windows.Win32Exception;
import oshi.util.GlobalConfig;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.Advapi32FFM.*;
import static oshi.ffm.windows.Advapi32FFM.RegQueryValueEx;
import static oshi.ffm.windows.WinErrorFFM.ERROR_INSUFFICIENT_BUFFER;
import static oshi.ffm.windows.WinErrorFFM.ERROR_SUCCESS;
import static oshi.ffm.windows.Advapi32FFM.OpenProcessToken;
import static oshi.ffm.windows.Advapi32FFM.GetTokenInformation;
import static oshi.ffm.windows.Advapi32FFM.RegCloseKey;
import static oshi.ffm.windows.Advapi32FFM.RegEnumKeyEx;
import static oshi.ffm.windows.Advapi32FFM.RegOpenKeyEx;
import static oshi.ffm.windows.Advapi32FFM.RegQueryInfoKey;
import static oshi.ffm.windows.WinNTFFM.KEY_READ;
import static oshi.ffm.windows.WinNTFFM.REG_DWORD;
import static oshi.ffm.windows.WinNTFFM.REG_EXPAND_SZ;
import static oshi.ffm.windows.WinNTFFM.REG_SZ;
import static oshi.ffm.windows.WindowsForeignFunctions.readWideString;
import static oshi.ffm.windows.WindowsForeignFunctions.toWideString;
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

    public static String[] registryGetKeys(MemorySegment hKey) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment lpcSubKeys = arena.allocate(JAVA_INT);
            MemorySegment lpcMaxSubKeyLen = arena.allocate(JAVA_INT);

            int rc = RegQueryInfoKey(hKey, NULL, NULL, NULL, lpcSubKeys, lpcMaxSubKeyLen, NULL, NULL, NULL, NULL, NULL,
                    NULL);

            if (rc != ERROR_SUCCESS) {
                throw new Win32Exception(rc);
            }

            int subKeyCount = lpcSubKeys.get(JAVA_INT, 0);
            int maxNameLen = lpcMaxSubKeyLen.get(JAVA_INT, 0);

            List<String> keys = new ArrayList<>(subKeyCount);
            for (int i = 0; i < subKeyCount; i++) {
                MemorySegment nameBuf = arena.allocate((maxNameLen + 1) * 2);
                MemorySegment nameLen = arena.allocate(JAVA_INT);
                nameLen.set(JAVA_INT, 0, maxNameLen + 1);

                rc = RegEnumKeyEx(hKey, i, nameBuf, nameLen, NULL, NULL, NULL, NULL);
                if (rc != ERROR_SUCCESS) {
                    throw new Win32Exception(rc);
                }

                keys.add(readWideString(nameBuf));
            }

            return keys.toArray(String[]::new);
        }
    }

    public static String[] registryGetKeys(MemorySegment rootKey, String keyPath, int samDesiredExtra)
            throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment phkResult = arena.allocate(ADDRESS);
            int rc = RegOpenKeyEx(rootKey, toWideString(arena, keyPath), 0, KEY_READ | samDesiredExtra, phkResult);
            if (rc != ERROR_SUCCESS) {
                throw new Win32Exception(rc);
            }

            MemorySegment hKey = phkResult.get(ADDRESS, 0);
            try {
                return registryGetKeys(hKey);
            } finally {
                rc = RegCloseKey(hKey);
                if (rc != ERROR_SUCCESS) {
                    throw new Win32Exception(rc);
                }
            }
        }
    }

    public static int registryGetDword(MemorySegment hKey, String valueName) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pData = arena.allocate(JAVA_INT);
            MemorySegment lpType = arena.allocate(JAVA_INT);
            MemorySegment lpcbData = arena.allocate(JAVA_INT);
            lpcbData.set(JAVA_INT, 0, Integer.BYTES);

            int rc = RegQueryValueEx(hKey, toWideString(arena, valueName), 0, lpType, pData, lpcbData);
            checkSuccess(rc, ERROR_INSUFFICIENT_BUFFER);
            return pData.get(JAVA_INT, 0);
        }
    }

    public static String registryGetString(MemorySegment hKey, String valueName, int size) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment data = arena.allocate(size + 2);
            MemorySegment lpType = arena.allocate(JAVA_INT);
            MemorySegment lpcbData = arena.allocate(JAVA_INT);
            lpcbData.set(JAVA_INT, 0, size);

            int rc = RegQueryValueEx(hKey, toWideString(arena, valueName), 0, lpType, data, lpcbData);
            checkSuccess(rc, ERROR_INSUFFICIENT_BUFFER);
            return readWideString(data);
        }
    }

    public static Object registryGetValue(MemorySegment hKey, String valueName) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment lpType = arena.allocate(JAVA_INT);
            MemorySegment lpcbData = arena.allocate(JAVA_INT);

            int rc = RegQueryValueEx(hKey, toWideString(arena, valueName), 0, lpType, MemorySegment.NULL, lpcbData);
            checkSuccess(rc, ERROR_INSUFFICIENT_BUFFER);

            int type = lpType.get(JAVA_INT, 0);
            int size = lpcbData.get(JAVA_INT, 0);

            return switch (type) {
                case REG_SZ, REG_EXPAND_SZ -> registryGetString(hKey, valueName, size);
                case REG_DWORD -> registryGetDword(hKey, valueName);
                default -> {
                    LOG.warn("Unsupported registry data type " + type + " for " + valueName);
                    yield null;
                }
            };
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
            MemorySegment sourceName = toWideString(arena, systemLog);

            Optional<MemorySegment> hEventLog = Advapi32FFM.OpenEventLog(NULL, sourceName);
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
