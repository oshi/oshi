/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static oshi.ffm.windows.WindowsForeignFunctions.readWideString;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.Wtsapi32FFM;
import oshi.software.os.OSSession;
import oshi.util.ParseUtil;

/**
 * Utility to read WTS session data via FFM.
 */
@ThreadSafe
public final class SessionWtsDataFFM {

    private static final int WTS_ACTIVE = 0;
    private static final int WTS_CLIENTADDRESS = 14;
    private static final int WTS_SESSIONINFO = 24;
    private static final int WTS_CLIENTPROTOCOLTYPE = 16;

    private static final int AF_INET = 2;
    private static final int AF_INET6 = 23;

    // WTS_SESSION_INFOW on 64-bit: DWORD(4) + pad(4) + LPWSTR(8) + DWORD(4) + pad(4) = 24
    private static final long SESSION_INFO_SIZE = 24;
    private static final long SESSION_INFO_SESSIONID_OFFSET = 0;
    private static final long SESSION_INFO_WINSTATION_OFFSET = 8;
    private static final long SESSION_INFO_STATE_OFFSET = 16;

    // WTSINFOW offsets (64-bit):
    // State(4)+SessionId(4)+counters(6*4=24)+WinStationName(32*2)+Domain(17*2)+UserName(21*2)
    // then padding to 8-byte align, then 5 LARGE_INTEGERs
    // State: 0, SessionId: 4, IncomingBytes: 8, ..., OutgoingCompressedBytes: 28
    // WinStationName: 32, Domain: 32+64=96, UserName: 96+34=130
    // After UserName (130+42=172), pad to 176, then ConnectTime(8), DisconnectTime(8), LastInputTime(8), LogonTime(8)
    // LogonTime offset = 176 + 24 = 200
    private static final int WINSTATIONNAME_LENGTH = 32;
    private static final int DOMAIN_LENGTH = 17;
    private static final int USERNAME_LENGTH = 20;
    private static final long WTSINFO_USERNAME_OFFSET = 32 + WINSTATIONNAME_LENGTH * 2L + DOMAIN_LENGTH * 2L;
    // After fixed-size char arrays, align to 8 for LARGE_INTEGERs
    private static final long WTSINFO_AFTER_STRINGS = 32 + WINSTATIONNAME_LENGTH * 2L + DOMAIN_LENGTH * 2L
            + (USERNAME_LENGTH + 1) * 2L;
    private static final long WTSINFO_LOGONTIME_OFFSET;

    static {
        // Align to 8 bytes, then skip ConnectTime(8) + DisconnectTime(8) + LastInputTime(8)
        long aligned = (WTSINFO_AFTER_STRINGS + 7) & ~7L;
        WTSINFO_LOGONTIME_OFFSET = aligned + 24; // 4th LARGE_INTEGER
    }

    // WTS_CLIENT_ADDRESS: DWORD AddressFamily(4) + byte[20] Address
    private static final long CLIENT_ADDR_FAMILY_OFFSET = 0;
    private static final long CLIENT_ADDR_ADDRESS_OFFSET = 4;

    private SessionWtsDataFFM() {
    }

    public static List<OSSession> queryUserSessions() {
        List<OSSession> sessions = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ppSessionInfo = arena.allocate(ADDRESS);
            MemorySegment pCount = arena.allocate(JAVA_INT);

            if (!Wtsapi32FFM.WTSEnumerateSessions(Wtsapi32FFM.WTS_CURRENT_SERVER_HANDLE, 0, 1, ppSessionInfo, pCount)) {
                return sessions;
            }

            MemorySegment pSessionInfo = ppSessionInfo.get(ADDRESS, 0);
            try {
                int count = pCount.get(JAVA_INT, 0);

                if (count > 0) {
                    // Reinterpret to cover all session entries
                    pSessionInfo = pSessionInfo.reinterpret(SESSION_INFO_SIZE * count);

                    MemorySegment ppBuffer = arena.allocate(ADDRESS);
                    MemorySegment pBytes = arena.allocate(JAVA_INT);

                    for (int i = 0; i < count; i++) {
                        long base = i * SESSION_INFO_SIZE;
                        int state = pSessionInfo.get(JAVA_INT, base + SESSION_INFO_STATE_OFFSET);
                        if (state != WTS_ACTIVE) {
                            continue;
                        }
                        int sessionId = pSessionInfo.get(JAVA_INT, base + SESSION_INFO_SESSIONID_OFFSET);
                        MemorySegment pWinStation = pSessionInfo.get(ADDRESS, base + SESSION_INFO_WINSTATION_OFFSET);

                        // Query protocol type
                        if (!Wtsapi32FFM.WTSQuerySessionInformation(Wtsapi32FFM.WTS_CURRENT_SERVER_HANDLE, sessionId,
                                WTS_CLIENTPROTOCOLTYPE, ppBuffer, pBytes)) {
                            continue;
                        }
                        MemorySegment pBuffer = ppBuffer.get(ADDRESS, 0).reinterpret(2);
                        short protocolType;
                        try {
                            protocolType = pBuffer.get(JAVA_SHORT, 0);
                        } finally {
                            Wtsapi32FFM.WTSFreeMemory(pBuffer);
                        }

                        // Only non-console sessions; console sessions come from registry
                        if (protocolType <= 0) {
                            continue;
                        }
                        String device = readWideString(pWinStation.reinterpret(256));

                        // Query WTSINFO for username and logon time
                        if (!Wtsapi32FFM.WTSQuerySessionInformation(Wtsapi32FFM.WTS_CURRENT_SERVER_HANDLE, sessionId,
                                WTS_SESSIONINFO, ppBuffer, pBytes)) {
                            continue;
                        }
                        pBuffer = ppBuffer.get(ADDRESS, 0).reinterpret(WTSINFO_LOGONTIME_OFFSET + 8);
                        String userName;
                        long logonTime;
                        try {
                            userName = readWideString(
                                    pBuffer.asSlice(WTSINFO_USERNAME_OFFSET, (USERNAME_LENGTH + 1) * 2L));
                            logonTime = ParseUtil.filetimeToUtcMs(pBuffer.get(JAVA_LONG, WTSINFO_LOGONTIME_OFFSET),
                                    false);
                        } finally {
                            Wtsapi32FFM.WTSFreeMemory(pBuffer);
                        }

                        // Query client address
                        if (!Wtsapi32FFM.WTSQuerySessionInformation(Wtsapi32FFM.WTS_CURRENT_SERVER_HANDLE, sessionId,
                                WTS_CLIENTADDRESS, ppBuffer, pBytes)) {
                            sessions.add(new OSSession(userName, device, logonTime, "::"));
                            continue;
                        }
                        pBuffer = ppBuffer.get(ADDRESS, 0).reinterpret(24);
                        int addrFamily;
                        byte[] address;
                        try {
                            addrFamily = pBuffer.get(JAVA_INT, CLIENT_ADDR_FAMILY_OFFSET);
                            address = pBuffer.asSlice(CLIENT_ADDR_ADDRESS_OFFSET, 20)
                                    .toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
                        } finally {
                            Wtsapi32FFM.WTSFreeMemory(pBuffer);
                        }

                        String host = "::";
                        if (addrFamily == AF_INET) {
                            try {
                                host = InetAddress.getByAddress(Arrays.copyOfRange(address, 2, 6)).getHostAddress();
                            } catch (UnknownHostException e) {
                                host = "Illegal length IP Array";
                            }
                        } else if (addrFamily == AF_INET6) {
                            int[] ipArray = convertBytesToInts(address);
                            host = ParseUtil.parseUtAddrV6toIP(ipArray);
                        }
                        sessions.add(new OSSession(userName, device, logonTime, host));
                    }
                }
            } finally {
                Wtsapi32FFM.WTSFreeMemory(pSessionInfo);
            }
        } catch (Throwable t) {
            // Silently return what we have
        }
        return sessions;
    }

    private static int[] convertBytesToInts(byte[] address) {
        IntBuffer intBuf = ByteBuffer.wrap(Arrays.copyOfRange(address, 2, 18)).order(ByteOrder.BIG_ENDIAN)
                .asIntBuffer();
        int[] array = new int[intBuf.remaining()];
        intBuf.get(array);
        return array;
    }
}
