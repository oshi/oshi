/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.software.os.NetworkParams.IPRoute;
import oshi.util.driver.unix.RouteTableDump.Layout;

/**
 * Exercises the routing message walk against buffers laid out by hand.
 * <p>
 * The walk reads a kernel buffer, so the interesting cases are the ones a live table will not produce: a message
 * claiming to be shorter than its own header, a sockaddr claiming more bytes than its message holds, and a buffer
 * ending mid-message. It makes no native call, so this runs on every platform rather than only the BSDs.
 */
class RouteTableDumpTest {

    private static final int RTM_GET = 4;
    private static final int RTF_UP = 0x1;
    private static final int RTF_GATEWAY = 0x2;
    /** macOS RTF_WASCLONED and RTF_LLINFO, the pair Layout.MACOS names. */
    private static final int RTF_WASCLONED = 0x20000;
    private static final int RTF_LLINFO = 0x400;

    private static final int RTAX_DST = 1 << 0;
    private static final int RTAX_GATEWAY = 1 << 1;
    private static final int RTAX_NETMASK = 1 << 2;

    /** macOS: 92-byte header, addresses padded to 4. */
    private static final int HEADER = 92;
    private static final int PAD = 4;

    /** FreeBSD's header, for the one case that reads with its layout. */
    private static final int FREEBSD_HEADER = 152;

    private static final Map<Integer, String> IF_NAMES = Collections.singletonMap(7, "en0");

    /**
     * One message: a default route via a gateway, then a /24 with a truncated netmask.
     *
     * @param truncateBy      bytes to cut off the end of the buffer, to model a short read
     * @param maskLenOverride a length to write into the netmask sockaddr instead of its real one, or -1
     */
    private static byte[] buildDump(int truncateBy, int maskLenOverride) {
        // message 1: destination, gateway (sockaddr_in, 16 bytes each), then a netmask of no length, which is how a
        // default route states a prefix of zero
        int msg1 = HEADER + 16 + 16 + PAD;
        // message 2: destination, gateway, then a 7-byte netmask padded to 8
        int msg2 = HEADER + 16 + 16 + 8;
        byte[] bytes = new byte[msg1 + msg2];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());

        writeHeader(bb, 0, msg1, RTF_UP | RTF_GATEWAY, RTAX_DST | RTAX_GATEWAY | RTAX_NETMASK);
        writeInet(bb, HEADER, new byte[] { 0, 0, 0, 0 });
        writeInet(bb, HEADER + 16, new byte[] { 10, 0, 0, 1 });
        // sa_len 0: the mask has no bytes at all, so every bit of it is zero

        int m2 = msg1;
        writeHeader(bb, m2, msg2, RTF_UP, RTAX_DST | RTAX_GATEWAY | RTAX_NETMASK);
        writeInet(bb, m2 + HEADER, new byte[] { (byte) 192, (byte) 168, 1, 0 });
        writeInet(bb, m2 + HEADER + 16, new byte[] { 10, 0, 0, 1 });
        // a netmask carries only the bytes it needs: 4 of header plus 3 of mask for a /24
        int mask = m2 + HEADER + 32;
        bytes[mask] = (byte) (maskLenOverride < 0 ? 7 : maskLenOverride);
        bytes[mask + 1] = (byte) 255;
        bytes[mask + 4] = (byte) 255;
        bytes[mask + 5] = (byte) 255;
        bytes[mask + 6] = (byte) 255;

        return truncateBy == 0 ? bytes : Arrays.copyOf(bytes, bytes.length - truncateBy);
    }

    private static void writeHeader(ByteBuffer bb, int at, int msgLen, int flags, int addrs) {
        bb.putShort(at, (short) msgLen);
        bb.put(at + 3, (byte) RTM_GET);
        bb.putShort(at + 4, (short) 7); // interface index
        bb.putInt(at + 8, flags);
        bb.putInt(at + 12, addrs);
    }

    private static void writeInet(ByteBuffer bb, int at, byte[] address) {
        bb.put(at, (byte) 16); // sa_len
        bb.put(at + 1, (byte) 2); // AF_INET
        for (int i = 0; i < 4; i++) {
            bb.put(at + 4 + i, address[i]);
        }
    }

    @Test
    void testWellFormedDump() {
        List<IPRoute> routes = RouteTableDump.parse(buildDump(0, -1), Layout.MACOS, IF_NAMES);
        assertEquals(2, routes.size());

        IPRoute def = routes.get(0);
        assertEquals(0, def.getPrefixLength());
        assertTrue(def.isGateway());
        assertEquals("en0", def.getInterfaceName());
        assertEquals(7, def.getInterfaceIndex());
        assertTrue(Arrays.equals(new byte[] { 10, 0, 0, 1 }, def.getGateway()));

        IPRoute subnet = routes.get(1);
        // the mask carries three bytes, and the fourth is implied zero
        assertEquals(24, subnet.getPrefixLength());
        assertTrue(Arrays.equals(new byte[] { (byte) 192, (byte) 168, 1, 0 }, subnet.getDestination()));
        // a route without the gateway flag reports no gateway, whatever the message carries
        assertEquals(0, subnet.getGateway().length);
    }

    @Test
    void testTrailingPartialHeaderIsRejected() {
        // the last message loses all but a few bytes, leaving too little for a header
        byte[] dump = buildDump(HEADER + 32, -1);
        assertTrue(RouteTableDump.parse(dump, Layout.MACOS, IF_NAMES).isEmpty(),
                "a buffer ending mid-message should yield nothing, so the caller falls back to the command");
    }

    @Test
    void testOversizedSockaddrIsRejected() {
        // the netmask claims far more bytes than its message holds
        assertTrue(RouteTableDump.parse(buildDump(0, 200), Layout.MACOS, IF_NAMES).isEmpty(),
                "a sockaddr longer than its message should yield nothing");
    }

    @Test
    void testMessageShorterThanItsHeaderIsRejected() {
        byte[] dump = buildDump(0, -1);
        ByteBuffer.wrap(dump).order(ByteOrder.nativeOrder()).putShort(0, (short) (HEADER - 4));
        assertTrue(RouteTableDump.parse(dump, Layout.MACOS, IF_NAMES).isEmpty(),
                "a message shorter than its own header should yield nothing");
    }

    @Test
    void testEmptyBufferYieldsNoRoutes() {
        assertTrue(RouteTableDump.parse(new byte[0], Layout.MACOS, IF_NAMES).isEmpty());
    }

    @Test
    void testFlaggedButAbsentSockaddrIsRejected() {
        // The header names a netmask, but the message ends after the gateway. Every message on every platform
        // measured ends exactly on its last address, so this is truncation rather than a shorthand for "no mask"
        int msgLen = HEADER + 16 + 16;
        byte[] bytes = new byte[msgLen];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        writeHeader(bb, 0, msgLen, RTF_UP, RTAX_DST | RTAX_GATEWAY | RTAX_NETMASK);
        writeInet(bb, HEADER, new byte[] { (byte) 192, (byte) 168, 1, 0 });
        writeInet(bb, HEADER + 16, new byte[] { 10, 0, 0, 1 });

        assertTrue(RouteTableDump.parse(bytes, Layout.MACOS, IF_NAMES).isEmpty(),
                "a message naming an address it does not carry should yield nothing");
    }

    /**
     * Builds one message with the given flags, carrying a destination and a gateway.
     */
    private static byte[] oneRoute(int flags, byte[] destination, int headerSize) {
        int msgLen = headerSize + 16 + 16;
        byte[] bytes = new byte[msgLen];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        writeHeader(bb, 0, msgLen, flags, RTAX_DST | RTAX_GATEWAY);
        writeInet(bb, headerSize, destination);
        writeInet(bb, headerSize + 16, new byte[] { 10, 0, 0, 1 });
        return bytes;
    }

    @Test
    void testClonedRouteIsExcluded() {
        // A route cloned from another is a cache entry for one host, which the routing table does not list
        byte[] dump = oneRoute(RTF_UP | RTF_GATEWAY | RTF_WASCLONED, new byte[] { 93, (byte) 184, (byte) 216, 1 },
                HEADER);
        assertTrue(RouteTableDump.parse(dump, Layout.MACOS, IF_NAMES).isEmpty(), "a cloned route should be dropped");
    }

    @Test
    void testClonedLinkLayerRouteIsKept() {
        // Except a link-layer one: that is how the neighbour cache appears, and the table does show those
        byte[] dump = oneRoute(RTF_UP | RTF_WASCLONED | RTF_LLINFO, new byte[] { 10, 0, 0, 42 }, HEADER);
        List<IPRoute> routes = RouteTableDump.parse(dump, Layout.MACOS, IF_NAMES);
        assertEquals(1, routes.size(), "a cloned link-layer route is the neighbour cache and should be kept");
        assertTrue(Arrays.equals(new byte[] { 10, 0, 0, 42 }, routes.get(0).getDestination()));
    }

    @Test
    void testClonedRouteIsKeptWhereThePlatformDoesNotClone() {
        // FreeBSD stopped cloning routes and names no flag for it, so the same bits mean nothing there. Its header
        // is longer than the macOS one the other cases use
        byte[] dump = oneRoute(RTF_UP | RTF_GATEWAY | RTF_WASCLONED, new byte[] { 93, (byte) 184, (byte) 216, 1 },
                FREEBSD_HEADER);
        assertEquals(1, RouteTableDump.parse(dump, Layout.FREEBSD, IF_NAMES).size(),
                "a platform that does not clone should not have routes filtered out from under it");
    }
}
