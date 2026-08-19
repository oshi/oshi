/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.NetworkParams.IPRoute;
import oshi.util.ParseUtil;

/**
 * Reads the routing table out of the buffer a {@code sysctl} of {@code NET_RT_DUMP} returns on macOS and the BSDs.
 * <p>
 * The buffer is a run of variable-length messages, each a fixed header followed by the addresses named in its
 * {@code rtm_addrs} bitmask, one per set bit in ascending {@code RTAX} order. Every address is a {@code sockaddr}
 * stating its own length in its first byte and padded up to a platform-dependent unit.
 * <p>
 * Only four numbers differ between the platforms, which {@link Layout} carries; the walk itself is the same everywhere.
 * The padding unit is the one to respect: it is four bytes on macOS and eight on the BSDs, and reading a message with
 * the wrong unit yields addresses from the middle of the next field rather than an error.
 */
@ThreadSafe
public final class RouteTableDump {

    private static final int RTM_GET = 4;
    private static final int RTF_GATEWAY = 0x2;
    private static final int RTF_HOST = 0x4;

    private static final int RTAX_DST = 0;
    private static final int RTAX_GATEWAY = 1;
    private static final int RTAX_NETMASK = 2;

    private static final int AF_INET = 2;

    // Offsets shared by every platform measured
    private static final int OFF_MSGLEN = 0;
    private static final int OFF_TYPE = 3;
    private static final int OFF_ADDRS = 12;

    /**
     * The parts of a routing message whose position or size depends on the platform, and the platforms' values.
     * <p>
     * They are declared together so they can be read as a table, because reading down a column is the only way to
     * notice that macOS pads addresses to four bytes where the BSDs pad to eight. That one is the trap: the wrong unit
     * does not fail, it reads addresses from the middle of the next field.
     * <p>
     * OpenBSD is otherwise the odd one, stating its header length per message and carrying a route priority usable as a
     * metric where the others have none.
     * <p>
     * DragonFly BSD differs from FreeBSD only in the last two columns, and only because FreeBSD stopped cloning routes:
     * it defines neither {@code RTF_WASCLONED} nor {@code RTF_CLONED}, while DragonFly kept both. The flag values are
     * not interchangeable across these platforms either -- OpenBSD's {@code RTF_CLONED} is {@code 0x10000}, where
     * {@code 0x20000} is {@code RTF_CACHED}.
     */
    public enum Layout {
        // header hdrLen flags index priority padding rtaxMax AF_INET6 cloned linkInfo
        MACOS(92, -1, 8, 4, -1, 4, 8, 30, 0x20000, 0x400), //
        FREEBSD(152, -1, 8, 4, -1, 8, 8, 28, 0, 0), //
        DRAGONFLY(152, -1, 8, 4, -1, 8, 11, 28, 0x20000, 0x400), //
        NETBSD(120, -1, 8, 4, -1, 8, 9, 24, 0, 0), //
        OPENBSD(96, 4, 16, 6, 10, 8, 15, 24, 0, 0);

        private final int headerSize;
        private final int hdrLenOffset;
        private final int flagsOffset;
        private final int indexOffset;
        private final int priorityOffset;
        private final int paddingUnit;
        private final int rtaxMax;
        private final int afInet6;
        private final int clonedFlag;
        private final int linkInfoFlag;

        Layout(int headerSize, int hdrLenOffset, int flagsOffset, int indexOffset, int priorityOffset, int paddingUnit,
                int rtaxMax, int afInet6, int clonedFlag, int linkInfoFlag) {
            this.headerSize = headerSize;
            this.hdrLenOffset = hdrLenOffset;
            this.flagsOffset = flagsOffset;
            this.indexOffset = indexOffset;
            this.priorityOffset = priorityOffset;
            this.paddingUnit = paddingUnit;
            this.rtaxMax = rtaxMax;
            this.afInet6 = afInet6;
            this.clonedFlag = clonedFlag;
            this.linkInfoFlag = linkInfoFlag;
        }
    }

    private RouteTableDump() {
    }

    /**
     * Parses a routing table dump.
     *
     * @param buffer      The bytes {@code sysctl} returned
     * @param layout      The platform's message layout
     * @param ifNameByIdx Interface names by index, used to name each route's interface
     * @return The routes read, skipping any message whose destination will not parse
     */
    public static List<IPRoute> parse(byte[] buffer, Layout layout, Map<Integer, String> ifNameByIdx) {
        List<IPRoute> routes = new ArrayList<>();
        ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder());
        int offset = 0;
        while (offset + layout.headerSize <= buffer.length) {
            int msgLen = bb.getShort(offset + OFF_MSGLEN) & 0xFFFF;
            // A message shorter than the header would have its own fields read out of the next one, and one running
            // past the buffer cannot be trusted at all. Give up on the whole table rather than return part of it:
            // the caller reads it by running a command when this yields nothing, which beats a truncated answer.
            if (msgLen < layout.headerSize || offset + msgLen > buffer.length) {
                routes.clear();
                return routes;
            }
            if ((buffer[offset + OFF_TYPE] & 0xFF) == RTM_GET) {
                IPRoute route = parseMessage(bb, buffer, offset, msgLen, layout, ifNameByIdx);
                if (route != null) {
                    routes.add(route);
                }
            }
            offset += msgLen;
        }
        return routes;
    }

    private static @Nullable IPRoute parseMessage(ByteBuffer bb, byte[] buffer, int offset, int msgLen, Layout layout,
            Map<Integer, String> ifNameByIdx) {
        int addrs = bb.getInt(offset + OFF_ADDRS);
        int flags = bb.getInt(offset + layout.flagsOffset);
        // A route cloned from another is a cache entry for one host rather than a table entry, and is not listed --
        // except a link-layer one, which is how the neighbour cache appears and which the table does show
        if (layout.clonedFlag != 0 && (flags & layout.clonedFlag) != 0 && (flags & layout.linkInfoFlag) == 0) {
            return null;
        }
        int index = bb.getShort(offset + layout.indexOffset) & 0xFFFF;
        int headerSize = layout.hdrLenOffset < 0 ? layout.headerSize
                : bb.getShort(offset + layout.hdrLenOffset) & 0xFFFF;

        byte[] destination = new byte[0];
        byte[] gateway = new byte[0];
        int maskStart = -1;
        int maskLen = 0;

        int end = offset + msgLen;
        int sa = offset + headerSize;
        for (int rtax = 0; rtax < layout.rtaxMax && sa < end; rtax++) {
            if ((addrs & (1 << rtax)) == 0) {
                continue;
            }
            int saLen = buffer[sa] & 0xFF;
            int saFamily = buffer[sa + 1] & 0xFF;
            if (rtax == RTAX_DST) {
                destination = readAddress(buffer, sa, saLen, saFamily, layout);
            } else if (rtax == RTAX_GATEWAY) {
                gateway = readAddress(buffer, sa, saLen, saFamily, layout);
            } else if (rtax == RTAX_NETMASK) {
                // The netmask is a truncated sockaddr carrying only the bytes it needs, and its family is not the
                // address family, so where the mask starts follows from the destination rather than from this header
                maskStart = sa;
                maskLen = saLen;
            }
            sa += saLen == 0 ? layout.paddingUnit : roundUp(saLen, layout.paddingUnit);
        }

        if (destination.length == 0) {
            return null;
        }
        boolean isGateway = (flags & RTF_GATEWAY) != 0;
        int prefixLength = prefixFromMask(buffer, maskStart, maskLen, destination.length);
        boolean isHost = (flags & RTF_HOST) != 0 || prefixLength == destination.length * 8;
        long metric = layout.priorityOffset < 0 ? -1L : buffer[offset + layout.priorityOffset] & 0xFFL;
        String name = ifNameByIdx.get(index);
        return new IPRoute(destination, prefixLength, isGateway ? gateway : new byte[0], name == null ? "" : name,
                index, metric, isGateway, isHost);
    }

    /**
     * Reads an address out of a sockaddr, yielding nothing for a link-layer or truncated one.
     */
    private static byte[] readAddress(byte[] buffer, int sa, int saLen, int saFamily, Layout layout) {
        if (saFamily == AF_INET && saLen >= 8) {
            return Arrays.copyOfRange(buffer, sa + 4, sa + 8);
        }
        if (saFamily == layout.afInet6 && saLen >= 24) {
            byte[] address = Arrays.copyOfRange(buffer, sa + 8, sa + 24);
            clearEmbeddedScope(address);
            return address;
        }
        return new byte[0];
    }

    /**
     * Clears the interface index a routing socket embeds in the third and fourth bytes of an address whose scope is
     * narrower than the whole site. The address is carried that way inside the kernel; the index belongs in the scope,
     * which this API does not report, and leaving it in place renders {@code fe80::1} as {@code fe80:17::1} and turns
     * one multicast route into one per interface.
     * <p>
     * It applies to link-local unicast, and to multicast scoped to an interface or a link.
     */
    private static void clearEmbeddedScope(byte[] address) {
        if (address.length != 16) {
            return;
        }
        boolean linkLocalUnicast = (address[0] & 0xFF) == 0xFE && (address[1] & 0xC0) == 0x80;
        boolean scopedMulticast = (address[0] & 0xFF) == 0xFF && (address[1] & 0x0F) <= 2;
        if (linkLocalUnicast || scopedMulticast) {
            address[2] = 0;
            address[3] = 0;
        }
    }

    /**
     * Derives a prefix length from a truncated netmask sockaddr. The bytes present start where the address would in a
     * sockaddr of the destination's family, and any byte the kernel left off is zero.
     */
    private static int prefixFromMask(byte[] buffer, int maskStart, int maskLen, int addressBytes) {
        if (maskStart < 0) {
            // No netmask supplied at all: a host route unless its flags say otherwise
            return addressBytes * 8;
        }
        int addrOffset = addressBytes == 4 ? 4 : 8;
        byte[] mask = new byte[addressBytes];
        for (int i = 0; i < addressBytes && addrOffset + i < maskLen; i++) {
            mask[i] = buffer[maskStart + addrOffset + i];
        }
        return ParseUtil.netmaskToPrefixLength(mask);
    }

    private static int roundUp(int len, int unit) {
        return 1 + ((len - 1) | (unit - 1));
    }
}
