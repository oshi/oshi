/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.mac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static oshi.ffm.platform.mac.MacSystem.INSI_FADDR;
import static oshi.ffm.platform.mac.MacSystem.INSI_FPORT;
import static oshi.ffm.platform.mac.MacSystem.INSI_LADDR;
import static oshi.ffm.platform.mac.MacSystem.INSI_LPORT;
import static oshi.ffm.platform.mac.MacSystem.INSI_VFLAG;
import static oshi.ffm.platform.mac.MacSystem.IN_SOCK_INFO;
import static oshi.ffm.platform.mac.MacSystem.PROC_FD;
import static oshi.ffm.platform.mac.MacSystem.PROC_FDTYPE;
import static oshi.ffm.platform.mac.MacSystem.PROC_FD_INFO;
import static oshi.ffm.platform.mac.MacSystem.PSI;
import static oshi.ffm.platform.mac.MacSystem.SOCKET_FD_INFO;
import static oshi.ffm.platform.mac.MacSystem.SOCKET_INFO;
import static oshi.ffm.platform.mac.MacSystem.SOI_FAMILY;
import static oshi.ffm.platform.mac.MacSystem.SOI_INCQLEN;
import static oshi.ffm.platform.mac.MacSystem.SOI_KIND;
import static oshi.ffm.platform.mac.MacSystem.SOI_PROTO;
import static oshi.ffm.platform.mac.MacSystem.SOI_QLEN;
import static oshi.ffm.platform.mac.MacSystem.TCPSI_INI;
import static oshi.ffm.platform.mac.MacSystem.TCPSI_STATE;
import static oshi.ffm.platform.mac.MacSystem.TCP_SOCK_INFO;

import org.junit.jupiter.api.Test;

/**
 * Asserts the socket struct layouts against sizes and offsets taken from {@code <sys/proc_info.h>} on macOS 15
 * (arm64/x86_64, which agree). These layouts are pure arithmetic over {@code MemoryLayout}, performing no native call,
 * so this test runs on every platform and guards the mappings from a host that never executes them.
 */
class MacSystemLayoutTest {

    @Test
    void testProcFdInfoLayout() {
        assertEquals(8, PROC_FD_INFO.byteSize());
        assertEquals(0, PROC_FD_INFO.byteOffset(PROC_FD));
        assertEquals(4, PROC_FD_INFO.byteOffset(PROC_FDTYPE));
    }

    @Test
    void testInSockInfoLayout() {
        // The trailing insi_v6 is a 12-byte struct aligned to 4, not a run of bytes. Modelling it as nine bytes
        // leaves in_sockinfo six short, which silently shifts every field of an enclosing struct.
        assertEquals(80, IN_SOCK_INFO.byteSize());
        assertEquals(0, IN_SOCK_INFO.byteOffset(INSI_FPORT));
        assertEquals(4, IN_SOCK_INFO.byteOffset(INSI_LPORT));
        assertEquals(24, IN_SOCK_INFO.byteOffset(INSI_VFLAG));
        assertEquals(32, IN_SOCK_INFO.byteOffset(INSI_FADDR));
        assertEquals(48, IN_SOCK_INFO.byteOffset(INSI_LADDR));
    }

    @Test
    void testTcpSockInfoLayout() {
        assertEquals(120, TCP_SOCK_INFO.byteSize());
        assertEquals(0, TCP_SOCK_INFO.byteOffset(TCPSI_INI));
        assertEquals(80, TCP_SOCK_INFO.byteOffset(TCPSI_STATE));
    }

    @Test
    void testSocketInfoLayout() {
        assertEquals(768, SOCKET_INFO.byteSize());
        assertEquals(160, SOCKET_INFO.byteOffset(SOI_FAMILY));
        assertEquals(170, SOCKET_INFO.byteOffset(SOI_QLEN));
        assertEquals(172, SOCKET_INFO.byteOffset(SOI_INCQLEN));
        assertEquals(232, SOCKET_INFO.byteOffset(SOI_KIND));
        assertEquals(240, SOCKET_INFO.byteOffset(SOI_PROTO));
    }

    @Test
    void testSocketFdInfoLayout() {
        assertEquals(792, SOCKET_FD_INFO.byteSize());
        assertEquals(24, SOCKET_FD_INFO.byteOffset(PSI));
    }
}
