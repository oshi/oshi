/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.windows;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.slf4j.event.Level.TRACE;
import static oshi.ffm.ForeignFunctions.callInArenaBooleanOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.platform.windows.Advapi32FFM.GetTokenInformation;
import static oshi.ffm.platform.windows.Advapi32FFM.OpenProcessToken;
import static oshi.ffm.platform.windows.Advapi32FFM.RegCloseKey;
import static oshi.ffm.platform.windows.Advapi32FFM.RegEnumKeyEx;
import static oshi.ffm.platform.windows.Advapi32FFM.RegOpenKeyEx;
import static oshi.ffm.platform.windows.Advapi32FFM.RegQueryInfoKey;
import static oshi.ffm.platform.windows.Advapi32FFM.RegQueryValueEx;
import static oshi.ffm.platform.windows.WinErrorFFM.ERROR_INSUFFICIENT_BUFFER;
import static oshi.ffm.platform.windows.WinErrorFFM.ERROR_MORE_DATA;
import static oshi.ffm.platform.windows.WinErrorFFM.ERROR_SUCCESS;
import static oshi.ffm.platform.windows.WinNTFFM.KEY_READ;
import static oshi.ffm.platform.windows.WinNTFFM.REG_BINARY;
import static oshi.ffm.platform.windows.WinNTFFM.REG_DWORD;
import static oshi.ffm.platform.windows.WinNTFFM.REG_EXPAND_SZ;
import static oshi.ffm.platform.windows.WinNTFFM.REG_MULTI_SZ;
import static oshi.ffm.platform.windows.WinNTFFM.REG_QWORD;
import static oshi.ffm.platform.windows.WinNTFFM.REG_SZ;
import static oshi.ffm.platform.windows.WindowsForeignFunctions.checkSuccess;
import static oshi.ffm.platform.windows.WindowsForeignFunctions.readWideString;
import static oshi.ffm.platform.windows.WindowsForeignFunctions.toWideString;
import static oshi.util.Memoizer.memoize;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.platform.windows.Advapi32FFM;
import oshi.ffm.platform.windows.Kernel32FFM;
import oshi.ffm.platform.windows.Win32Exception;
import oshi.ffm.platform.windows.WinNTFFM;
import oshi.util.GlobalConfig;

/**
 * FFM-based utility for Windows Advapi32 registry and security operations.
 */
public final class Advapi32UtilFFM {

    private Advapi32UtilFFM() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(Advapi32UtilFFM.class);

    private static final Supplier<String> SYSTEM_LOG = memoize(Advapi32UtilFFM::querySystemLog,
            TimeUnit.HOURS.toNanos(1));

    /**
     * Checks whether the current process is running with elevated privileges.
     *
     * @return true if the process is elevated, false otherwise
     */
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

    /**
     * Enumerates the subkey names under an open registry key.
     *
     * @param hKey handle to an open registry key
     * @return array of subkey names
     * @throws Throwable if the native call fails
     */
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

    /**
     * Opens a registry key and enumerates its subkey names.
     *
     * @param rootKey         the root key handle (e.g., HKEY_LOCAL_MACHINE)
     * @param keyPath         the registry key path
     * @param samDesiredExtra additional access flags to combine with KEY_READ
     * @return array of subkey names
     * @throws Throwable if the native call fails
     */
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
                    LOG.warn("Failed to close registry key, error code: {}", rc);
                }
            }
        }
    }

    /**
     * Reads a REG_DWORD value from an open registry key.
     *
     * @param hKey      handle to an open registry key
     * @param valueName the value name to read
     * @return the DWORD value
     * @throws Throwable if the native call fails
     */
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

    /**
     * Reads a REG_QWORD value from an open registry key.
     *
     * @param hKey      handle to an open registry key
     * @param valueName the value name to read
     * @return the QWORD value
     * @throws Throwable if the native call fails
     */
    public static long registryGetQword(MemorySegment hKey, String valueName) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pData = arena.allocate(JAVA_LONG);
            MemorySegment lpType = arena.allocate(JAVA_INT);
            MemorySegment lpcbData = arena.allocate(JAVA_INT);
            lpcbData.set(JAVA_INT, 0, Long.BYTES);

            int rc = RegQueryValueEx(hKey, toWideString(arena, valueName), 0, lpType, pData, lpcbData);
            checkSuccess(rc, ERROR_INSUFFICIENT_BUFFER);
            return pData.get(JAVA_LONG, 0);
        }
    }

    /**
     * Reads a REG_BINARY value from an open registry key.
     *
     * @param hKey      handle to an open registry key
     * @param valueName the value name to read
     * @param size      the buffer size in bytes
     * @return the raw bytes
     * @throws Throwable if the native call fails
     */
    public static byte[] registryGetBinary(MemorySegment hKey, String valueName, int size) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment data = arena.allocate(Math.max(size, 1));
            MemorySegment lpType = arena.allocate(JAVA_INT);
            MemorySegment lpcbData = arena.allocate(JAVA_INT);
            lpcbData.set(JAVA_INT, 0, size);

            int rc = RegQueryValueEx(hKey, toWideString(arena, valueName), 0, lpType, data, lpcbData);
            checkSuccess(rc, ERROR_INSUFFICIENT_BUFFER);
            return data.asSlice(0, lpcbData.get(JAVA_INT, 0)).toArray(JAVA_BYTE);
        }
    }

    /**
     * Reads a REG_SZ string value from an open registry key.
     *
     * @param hKey      handle to an open registry key
     * @param valueName the value name to read
     * @param size      the buffer size in bytes
     * @return the string value
     * @throws Throwable if the native call fails
     */
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

    /**
     * Reads a registry value of any supported type from an open registry key.
     *
     * @param hKey      handle to an open registry key
     * @param valueName the value name to read
     * @return the value (Integer for REG_DWORD, String for REG_SZ/REG_EXPAND_SZ), or null if unsupported
     * @throws Throwable if the native call fails
     */
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
                case REG_QWORD -> registryGetQword(hKey, valueName);
                case REG_BINARY -> registryGetBinary(hKey, valueName, size);
                default -> {
                    LOG.warn("Unsupported registry data type {} for {}", type, valueName);
                    yield null;
                }
            };
        }
    }

    /**
     * Reads a registry value from the given root key and path.
     *
     * @param rootKey   The root key handle (e.g., HKEY_LOCAL_MACHINE)
     * @param keyPath   The registry key path
     * @param valueName The value name to read
     * @return The value object (Integer for REG_DWORD, String for REG_SZ/REG_EXPAND_SZ), or null on failure
     */
    public static Object registryGetValue(MemorySegment rootKey, String keyPath, String valueName) {
        return callInArenaOrDefault(arena -> {
            MemorySegment phkResult = arena.allocate(ADDRESS);
            int rc = RegOpenKeyEx(rootKey, toWideString(arena, keyPath), 0, KEY_READ, phkResult);
            if (rc != ERROR_SUCCESS) {
                return null;
            }
            MemorySegment hKey = phkResult.get(ADDRESS, 0);
            try {
                return registryGetValue(hKey, valueName);
            } finally {
                int closeRc = RegCloseKey(hKey);
                if (closeRc != ERROR_SUCCESS) {
                    LOG.debug("Failed to close registry key {}: error {}", keyPath, closeRc);
                }
            }
        }, LOG, TRACE, "Failed to read registry value", null);
    }

    /**
     * Checks whether a registry value exists under the given root key and path.
     *
     * @param rootKey   The root key handle (e.g., HKEY_LOCAL_MACHINE)
     * @param keyPath   The registry key path
     * @param valueName The value name to check
     * @return true if the value exists, false otherwise
     */
    public static boolean registryValueExists(MemorySegment rootKey, String keyPath, String valueName) {
        return callInArenaBooleanOrDefault(arena -> {
            MemorySegment phkResult = arena.allocate(ADDRESS);
            int rc = RegOpenKeyEx(rootKey, toWideString(arena, keyPath), 0, KEY_READ, phkResult);
            if (rc != ERROR_SUCCESS) {
                return false;
            }
            MemorySegment hKey = phkResult.get(ADDRESS, 0);
            try {
                MemorySegment lpType = arena.allocate(JAVA_INT);
                rc = RegQueryValueEx(hKey, toWideString(arena, valueName), 0, lpType, MemorySegment.NULL,
                        arena.allocate(JAVA_INT));
                return rc == ERROR_SUCCESS || rc == ERROR_MORE_DATA || rc == ERROR_INSUFFICIENT_BUFFER;
            } finally {
                int closeRc = RegCloseKey(hKey);
                if (closeRc != ERROR_SUCCESS) {
                    LOG.debug("Failed to close registry key {}: error {}", keyPath, closeRc);
                }
            }
        }, LOG, TRACE, "Failed to check registry value exists", false);
    }

    /**
     * Reads a REG_MULTI_SZ value from an open registry key.
     *
     * @param rootKey   The root key handle (e.g., HKEY_LOCAL_MACHINE)
     * @param keyPath   The registry key path
     * @param valueName The value name to read
     * @return An array of strings from the multi-sz value, or an empty array on failure
     */
    public static String[] registryGetStringArray(MemorySegment rootKey, String keyPath, String valueName) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment phkResult = arena.allocate(ADDRESS);
            int rc = RegOpenKeyEx(rootKey, toWideString(arena, keyPath), 0, KEY_READ, phkResult);
            if (rc != ERROR_SUCCESS) {
                throw new Win32Exception(rc);
            }
            MemorySegment hKey = phkResult.get(ADDRESS, 0);
            try {
                MemorySegment lpType = arena.allocate(JAVA_INT);
                MemorySegment lpcbData = arena.allocate(JAVA_INT);

                rc = RegQueryValueEx(hKey, toWideString(arena, valueName), 0, lpType, MemorySegment.NULL, lpcbData);
                if (rc != ERROR_SUCCESS && rc != ERROR_INSUFFICIENT_BUFFER && rc != ERROR_MORE_DATA) {
                    throw new Win32Exception(rc);
                }
                if (lpType.get(JAVA_INT, 0) != REG_MULTI_SZ) {
                    throw new IllegalStateException(
                            "Unexpected registry type " + lpType.get(JAVA_INT, 0) + ", expected REG_MULTI_SZ");
                }

                int size = lpcbData.get(JAVA_INT, 0);
                MemorySegment data;
                rc = ERROR_MORE_DATA;
                for (int attempt = 0; attempt < 3 && rc != ERROR_SUCCESS; attempt++) {
                    // Allocate extra for double-null terminator
                    data = arena.allocate(size + 4L);
                    data.fill((byte) 0);
                    lpcbData.set(JAVA_INT, 0, size + 4);

                    rc = RegQueryValueEx(hKey, toWideString(arena, valueName), 0, lpType, data, lpcbData);
                    if (rc == ERROR_MORE_DATA || rc == ERROR_INSUFFICIENT_BUFFER) {
                        size = lpcbData.get(JAVA_INT, 0);
                        continue;
                    }
                    if (rc != ERROR_SUCCESS) {
                        throw new Win32Exception(rc);
                    }

                    // Parse multi-sz: null-delimited wide strings, double-null terminated
                    int bytesWritten = lpcbData.get(JAVA_INT, 0);
                    List<String> result = new ArrayList<>();
                    long offset = 0;
                    while (offset < bytesWritten) {
                        String s = readWideString(data.asSlice(offset, bytesWritten - offset));
                        if (s.isEmpty()) {
                            break;
                        }
                        result.add(s);
                        offset += ((long) s.length() + 1) * 2; // chars + null, 2 bytes each
                    }
                    return result.toArray(new String[0]);
                }
                throw new Win32Exception(rc);
            } finally {
                int closeRc = RegCloseKey(hKey);
                if (closeRc != ERROR_SUCCESS) {
                    LOG.debug("Failed to close registry key {}\\\\{}: error {}", keyPath, valueName, closeRc);
                }
            }
        } catch (Throwable t) {
            LOG.warn("Failed to read registry string array {}\\\\{}: {}", keyPath, valueName, t.getMessage());
            return new String[0];
        }
    }

    /**
     * Queries the system boot time from the Windows Event Log.
     *
     * @return the boot time as a Unix epoch timestamp in seconds
     */
    public static long querySystemBootTime() {
        String eventLog = SYSTEM_LOG.get();
        try (Arena arena = Arena.ofConfined()) {
            Optional<MemorySegment> hEventLogOpt = Advapi32FFM.OpenEventLog(arena, eventLog);
            if (hEventLogOpt.isEmpty()) {
                LOG.warn("Unable to open system Event Log. Falling back to uptime.");
                return System.currentTimeMillis() / 1000L - Kernel32UtilFFM.querySystemUptime();
            }

            MemorySegment hEventLog = hEventLogOpt.get();
            try {
                int bufSize = 64 * 1024;
                MemorySegment buffer = arena.allocate(bufSize);
                MemorySegment bytesRead = arena.allocate(JAVA_INT);
                MemorySegment minBytesNeeded = arena.allocate(JAVA_INT);

                long event6005Time = 0L;

                long offsetEventId = WinNTFFM.OFFSET_EVENTID;
                long offsetTimeGenerated = WinNTFFM.OFFSET_TIME_GENERATED;
                long offsetLength = WinNTFFM.OFFSET_LENGTH;

                while (Advapi32FFM.ReadEventLog(hEventLog,
                        WinNTFFM.EVENTLOG_BACKWARDS_READ | WinNTFFM.EVENTLOG_SEQUENTIAL_READ, buffer, bufSize,
                        bytesRead, minBytesNeeded)) {

                    int read = bytesRead.get(JAVA_INT, 0);
                    int offset = 0;

                    while (offset < read) {
                        MemorySegment eventRecord = buffer.asSlice(offset, WinNTFFM.EVENTLOGRECORD.byteSize());

                        int eventId = eventRecord.get(JAVA_INT, (int) offsetEventId);
                        long timeGenerated = Integer
                                .toUnsignedLong(eventRecord.get(JAVA_INT, (int) offsetTimeGenerated));

                        if (eventId == 12) { // system boot
                            return timeGenerated;
                        } else if (eventId == 6005) { // event log startup
                            if (event6005Time > 0) {
                                return event6005Time;
                            }
                            event6005Time = timeGenerated;
                        }

                        // Advance to next record
                        int length = eventRecord.get(JAVA_INT, (int) offsetLength);
                        offset += length;
                    }
                }

                if (event6005Time > 0) {
                    return event6005Time;
                }
            } finally {
                // Close on every path, including a Throwable from a malformed record (matches the fallback contract).
                Advapi32FFM.CloseEventLog(hEventLog);
            }
        } catch (Throwable t) {
            LOG.error("Exception while querying system boottime, fallback to boot time from uptime", t);
        }
        // Fallback: approximate boot time from uptime
        return System.currentTimeMillis() / 1000L - Kernel32UtilFFM.querySystemUptime();
    }

    /**
     * Queries and validates the configured system event log name.
     *
     * @return the event log name, or null if unavailable
     */
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

            // Opened only to validate the configured log name; close it so the handle is not leaked.
            Advapi32FFM.CloseEventLog(hEventLog.get());
            return systemLog;
        } catch (Throwable t) {
            LOG.error("Exception while opening system event log", t);
            return null;
        }
    }
}
