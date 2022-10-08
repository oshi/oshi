/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Advapi32Util.Account;
import com.sun.jna.platform.win32.Advapi32Util.InfoKey;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinReg.HKEY;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSSession;

/**
 * Utility to read session data from HKEY_USERS
 */
@ThreadSafe
public final class HkeyUserData {

    private static final String PATH_DELIMITER = "\\";
    private static final String DEFAULT_DEVICE = "Console";
    private static final String VOLATILE_ENV_SUBKEY = "Volatile Environment";
    private static final String CLIENTNAME = "CLIENTNAME";
    private static final String SESSIONNAME = "SESSIONNAME";

    private static final Logger LOG = LoggerFactory.getLogger(HkeyUserData.class);

    private HkeyUserData() {
    }

    public static List<OSSession> queryUserSessions() {
        List<OSSession> sessions = new ArrayList<>();
        for (String sidKey : Advapi32Util.registryGetKeys(WinReg.HKEY_USERS)) {
            if (!sidKey.startsWith(".") && !sidKey.endsWith("_Classes")) {
                try {
                    Account a = Advapi32Util.getAccountBySid(sidKey);
                    String name = a.name;
                    String device = DEFAULT_DEVICE;
                    String host = a.domain; // temporary default
                    long loginTime = 0;
                    String keyPath = sidKey + PATH_DELIMITER + VOLATILE_ENV_SUBKEY;
                    if (Advapi32Util.registryKeyExists(WinReg.HKEY_USERS, keyPath)) {
                        HKEY hKey = Advapi32Util.registryGetKey(WinReg.HKEY_USERS, keyPath, WinNT.KEY_READ).getValue();
                        // InfoKey write time is user login time
                        InfoKey info = Advapi32Util.registryQueryInfoKey(hKey, 0);
                        loginTime = info.lpftLastWriteTime.toTime();
                        for (String subKey : Advapi32Util.registryGetKeys(hKey)) {
                            String subKeyPath = keyPath + PATH_DELIMITER + subKey;
                            // Check for session and client name
                            if (Advapi32Util.registryValueExists(WinReg.HKEY_USERS, subKeyPath, SESSIONNAME)) {
                                String session = Advapi32Util.registryGetStringValue(WinReg.HKEY_USERS, subKeyPath,
                                        SESSIONNAME);
                                if (!session.isEmpty()) {
                                    device = session;
                                }
                            }
                            if (Advapi32Util.registryValueExists(WinReg.HKEY_USERS, subKeyPath, CLIENTNAME)) {
                                String client = Advapi32Util.registryGetStringValue(WinReg.HKEY_USERS, subKeyPath,
                                        CLIENTNAME);
                                if (!client.isEmpty() && !DEFAULT_DEVICE.equals(client)) {
                                    host = client;
                                }
                            }
                        }
                        Advapi32Util.registryCloseKey(hKey);
                    }
                    sessions.add(new OSSession(name, device, loginTime, host));
                } catch (Win32Exception ex) {
                    LOG.warn("Error querying SID {} from registry: {}", sidKey, ex.getMessage());
                }
            }
        }
        return sessions;
    }
}
