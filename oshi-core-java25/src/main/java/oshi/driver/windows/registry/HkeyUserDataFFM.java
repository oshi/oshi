/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.windows.Advapi32FFM.ConvertStringSidToSid;
import static oshi.ffm.windows.Advapi32FFM.LookupAccountSid;
import static oshi.ffm.windows.Advapi32FFM.RegCloseKey;
import static oshi.ffm.windows.Advapi32FFM.RegOpenKeyEx;
import static oshi.ffm.windows.Advapi32FFM.RegQueryInfoKey;
import static oshi.ffm.windows.WinErrorFFM.ERROR_SUCCESS;
import static oshi.ffm.windows.WinNTFFM.KEY_READ;
import static oshi.ffm.windows.WindowsForeignFunctions.readWideString;
import static oshi.ffm.windows.WindowsForeignFunctions.toWideString;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.WinRegFFM;
import oshi.software.os.OSSession;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.Advapi32UtilFFM;

/**
 * Utility to read session data from HKEY_USERS
 */
@ThreadSafe
public final class HkeyUserDataFFM {

    private static final String PATH_DELIMITER = "\\";
    private static final String DEFAULT_DEVICE = "Console";
    private static final String VOLATILE_ENV_SUBKEY = "Volatile Environment";
    private static final String CLIENTNAME = "CLIENTNAME";
    private static final String SESSIONNAME = "SESSIONNAME";

    private static final Logger LOG = LoggerFactory.getLogger(HkeyUserDataFFM.class);

    private HkeyUserDataFFM() {
    }

    public static List<OSSession> queryUserSessions() {
        List<OSSession> sessions = new ArrayList<>();
        MemorySegment hKeyUsers = MemorySegment.ofAddress(WinRegFFM.HKEY_USERS);
        try {
            String[] sidKeys = Advapi32UtilFFM.registryGetKeys(hKeyUsers);
            for (String sidKey : sidKeys) {
                if (!sidKey.startsWith(".") && !sidKey.endsWith("_Classes")) {
                    try {
                        String[] account = lookupAccountBySid(sidKey);
                        if (account == null) {
                            continue;
                        }
                        String name = account[0];
                        String device = DEFAULT_DEVICE;
                        String host = account[1]; // domain as default host
                        long loginTime = 0;
                        String keyPath = sidKey + PATH_DELIMITER + VOLATILE_ENV_SUBKEY;
                        if (registryKeyExists(hKeyUsers, keyPath)) {
                            loginTime = queryKeyWriteTime(hKeyUsers, keyPath);
                            // Check subkeys for session/client name
                            String[] subKeys = getSubKeys(hKeyUsers, keyPath);
                            for (String subKey : subKeys) {
                                String subKeyPath = keyPath + PATH_DELIMITER + subKey;
                                if (Advapi32UtilFFM.registryValueExists(hKeyUsers, subKeyPath, SESSIONNAME)) {
                                    Object val = Advapi32UtilFFM.registryGetValue(hKeyUsers, subKeyPath, SESSIONNAME);
                                    if (val instanceof String s && !s.isEmpty()) {
                                        device = s;
                                    }
                                }
                                if (Advapi32UtilFFM.registryValueExists(hKeyUsers, subKeyPath, CLIENTNAME)) {
                                    Object val = Advapi32UtilFFM.registryGetValue(hKeyUsers, subKeyPath, CLIENTNAME);
                                    if (val instanceof String s && !s.isEmpty() && !DEFAULT_DEVICE.equals(s)) {
                                        host = s;
                                    }
                                }
                            }
                        }
                        sessions.add(new OSSession(name, device, loginTime, host));
                    } catch (Throwable ex) {
                        LOG.warn("Error querying user registry entry: {}", ex.getMessage());
                        LOG.debug("Failed SID: {}", sidKey);
                    }
                }
            }
        } catch (Throwable t) {
            LOG.warn("Error enumerating HKEY_USERS: {}", t.getMessage());
        }
        return sessions;
    }

    private static String[] lookupAccountBySid(String sidString) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pSidPtr = arena.allocate(ADDRESS);
            if (!ConvertStringSidToSid(toWideString(arena, sidString), pSidPtr)) {
                return null;
            }
            MemorySegment pSid = pSidPtr.get(ADDRESS, 0);
            try {
                MemorySegment cchName = arena.allocate(JAVA_INT);
                MemorySegment cchDomain = arena.allocate(JAVA_INT);
                MemorySegment peUse = arena.allocate(JAVA_INT);
                cchName.set(JAVA_INT, 0, 0);
                cchDomain.set(JAVA_INT, 0, 0);
                // First call to get sizes
                LookupAccountSid(NULL, pSid, NULL, cchName, NULL, cchDomain, peUse);
                int nameLen = cchName.get(JAVA_INT, 0);
                int domainLen = cchDomain.get(JAVA_INT, 0);
                if (nameLen == 0) {
                    return null;
                }
                MemorySegment nameBuf = arena.allocate((long) nameLen * 2);
                MemorySegment domainBuf = domainLen > 0 ? arena.allocate((long) domainLen * 2) : arena.allocate(2);
                cchName.set(JAVA_INT, 0, nameLen);
                cchDomain.set(JAVA_INT, 0, domainLen);
                if (!LookupAccountSid(NULL, pSid, nameBuf, cchName, domainBuf, cchDomain, peUse)) {
                    return null;
                }
                return new String[] { readWideString(nameBuf), domainLen > 0 ? readWideString(domainBuf) : "" };
            } finally {
                Kernel32FFM.LocalFree(pSid);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean registryKeyExists(MemorySegment rootKey, String keyPath) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment phkResult = arena.allocate(ADDRESS);
            int rc = RegOpenKeyEx(rootKey, toWideString(arena, keyPath), 0, KEY_READ, phkResult);
            if (rc != ERROR_SUCCESS) {
                return false;
            }
            RegCloseKey(phkResult.get(ADDRESS, 0));
            return true;
        } catch (Throwable t) {
            LOG.debug("Error checking registry key {}: {}", keyPath, t.getMessage());
            return false;
        }
    }

    private static long queryKeyWriteTime(MemorySegment rootKey, String keyPath) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment phkResult = arena.allocate(ADDRESS);
            int rc = RegOpenKeyEx(rootKey, toWideString(arena, keyPath), 0, KEY_READ, phkResult);
            if (rc != ERROR_SUCCESS) {
                return 0;
            }
            MemorySegment hKey = phkResult.get(ADDRESS, 0);
            try {
                MemorySegment ftLastWriteTime = arena.allocate(JAVA_LONG);
                rc = RegQueryInfoKey(hKey, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ftLastWriteTime);
                if (rc != ERROR_SUCCESS) {
                    return 0;
                }
                long filetime = ftLastWriteTime.get(JAVA_LONG, 0);
                return ParseUtil.filetimeToUtcMs(filetime, false);
            } finally {
                RegCloseKey(hKey);
            }
        } catch (Throwable t) {
            LOG.debug("Error querying write time for {}: {}", keyPath, t.getMessage());
            return 0;
        }
    }

    private static String[] getSubKeys(MemorySegment rootKey, String keyPath) {
        try {
            return Advapi32UtilFFM.registryGetKeys(rootKey, keyPath, 0);
        } catch (Throwable t) {
            LOG.debug("Error getting subkeys for {}: {}", keyPath, t.getMessage());
            return new String[0];
        }
    }
}
