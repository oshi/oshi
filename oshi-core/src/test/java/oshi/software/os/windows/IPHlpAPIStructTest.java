/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.Memory;

import oshi.jna.platform.windows.IPHlpAPI;
import oshi.jna.platform.windows.IPHlpAPI.IP_ADDRESS_PREFIX;
import oshi.jna.platform.windows.IPHlpAPI.MIB_IPFORWARD_ROW2;
import oshi.jna.platform.windows.IPHlpAPI.SOCKADDR_INET;

/**
 * Asserts the routing table structure layout, which is hand-mapped from {@code netioapi.h} rather than generated.
 * <p>
 * JNA derives the size and field placement independently of that hand mapping, which makes this test the real check on
 * the equivalent constants the FFM twin hardcodes in {@code IPHlpAPIFFM}: {@code MIB_IPFORWARD_ROW2_SIZE} and the
 * {@code OFFSET_ROUTE_*} values. If anything here changes, the twin must change with it.
 * <p>
 * The structures use only fixed-width members, so the layout depends on the address size rather than the operating
 * system. This still has to run on Windows, because JNA reads a structure's alignment and type-mapper options from its
 * enclosing class, which loads {@code IPHlpAPI} and fails everywhere else.
 */
@EnabledOnOs(OS.WINDOWS)
@DisabledIfSystemProperty(named = "os.name", matches = "(?i).*netbsd.*")
class IPHlpAPIStructTest {

    @Test
    void testStructureSizes() {
        // SOCKADDR_INET is a union of SOCKADDR_IN (16) and SOCKADDR_IN6 (28), mapped as the larger arm
        assertThat(new SOCKADDR_INET().size(), is(28));
        // IP_ADDRESS_PREFIX adds a UINT8 PrefixLength, padded to the structure's 4-byte alignment
        assertThat(new IP_ADDRESS_PREFIX().size(), is(32));
        // The FFM twin hardcodes this value as MIB_IPFORWARD_ROW2_SIZE
        assertThat("MIB_IPFORWARD_ROW2 is 104 bytes on 64-bit Windows", new MIB_IPFORWARD_ROW2().size(), is(104));
    }

    @Test
    void testFieldOffsetsMatchTheFfmConstants() {
        // Write a distinct value at each offset the FFM twin declares, then confirm the JNA mapping reads it from
        // that same place. This checks the offsets themselves, not merely that the total size happens to agree.
        try (Memory buf = new Memory(104)) {
            buf.clear();
            buf.setLong(0, 0x1122334455667788L); // InterfaceLuid
            buf.setInt(8, 42); // OFFSET_ROUTE_INTERFACE_INDEX
            buf.setShort(12, (short) IPHlpAPI.AF_INET); // OFFSET_ROUTE_DEST_FAMILY
            buf.setInt(16, 0x0100007F); // OFFSET_ROUTE_DEST_IPV4
            buf.setByte(20, (byte) 0x20); // OFFSET_ROUTE_DEST_IPV6
            buf.setByte(40, (byte) 24); // OFFSET_ROUTE_PREFIX_LENGTH
            buf.setShort(44, (short) IPHlpAPI.AF_INET6); // OFFSET_ROUTE_NEXTHOP_FAMILY
            buf.setInt(48, 0x0201A8C0); // OFFSET_ROUTE_NEXTHOP_IPV4
            buf.setByte(52, (byte) 0xfe); // OFFSET_ROUTE_NEXTHOP_IPV6
            buf.setInt(84, 256); // OFFSET_ROUTE_METRIC

            MIB_IPFORWARD_ROW2 row = new MIB_IPFORWARD_ROW2(buf);
            row.read();

            assertThat(row.InterfaceIndex, is(42));
            assertThat(row.DestinationPrefix.Prefix.si_family, is((short) IPHlpAPI.AF_INET));
            assertThat(row.DestinationPrefix.Prefix.ipv4AddrOrFlowInfo, is(0x0100007F));
            assertThat("PrefixLength sits after the 28-byte union", row.DestinationPrefix.PrefixLength, is((byte) 24));
            assertThat("NextHop begins after the 32-byte IP_ADDRESS_PREFIX", row.NextHop.si_family,
                    is((short) IPHlpAPI.AF_INET6));
            assertThat("The IPv6 arm's address begins 8 bytes into the union", row.NextHop.ipv6Addr[0],
                    is((byte) 0xfe));
            assertThat(row.Metric, is(256));

            // Both unions carry both arms whatever the family says, so the two offsets the family selection does not
            // exercise above are checked here. Reading them directly is what pins OFFSET_ROUTE_DEST_IPV6 and
            // OFFSET_ROUTE_NEXTHOP_IPV4, which the backends use for the opposite family.
            assertThat("The destination union's IPv6 arm sits at offset 20", row.DestinationPrefix.Prefix.ipv6Addr[0],
                    is((byte) 0x20));
            assertThat("The next hop union's IPv4 arm sits at offset 48", row.NextHop.ipv4AddrOrFlowInfo,
                    is(0x0201A8C0));
        }
    }
}
