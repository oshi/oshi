/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class BsdNetworkIFTest {

    @Test
    void testParseNetstatFreeBsd() {
        Map<String, Long> stats = BsdNetworkIF.parseNetstat(
                """
                        Name     Mtu Network                   Address                           Ipkts Ierrs Idrop      Ibytes     Opkts Oerrs      Obytes  Coll
                        vtnet0  9000 <Link#1>                  00:15:5d:c4:00:30              15029012     0     3  1610916560   1018784     1  1718869087     2
                        vtnet0     - 192.168.0.0/16            192.168.4.30                     336987     -     -    23723765    260196     -    46815572     -
                        """
                        .lines().toList());
        assertThat(stats, is(aMapWithSize(8)));
        assertThat(stats, hasEntry("Ipkts", 15_029_012L));
        assertThat(stats, hasEntry("Idrop", 3L));
        assertThat(stats, hasEntry("Ibytes", 1_610_916_560L));
        assertThat(stats, hasEntry("Opkts", 1_018_784L));
        assertThat(stats, hasEntry("Oerrs", 1L));
        assertThat(stats, hasEntry("Obytes", 1_718_869_087L));
        assertThat(stats, hasEntry("Coll", 2L));
    }

    @Test
    void testParseNetstatDragonFly() {
        // No Idrop column; the virtio driver counts bytes but not packets on the link-level row
        Map<String, Long> stats = BsdNetworkIF.parseNetstat("""
                Name    Mtu   Network       Address            Ipkts Ierrs     Ibytes    Opkts Oerrs     Obytes  Coll
                vtnet0  1500  <Link#1>    fa:16:3e:47:64:29        0     0 52967869005        0     0 35703541052     0
                vtnet0  1500  140.211.167.1 140-211-167-212 20457260     - 9971032506 32796892     - 35243602884     -
                """.lines().toList());
        assertThat(stats, is(aMapWithSize(7)));
        assertThat(stats, not(hasKey("Idrop")));
        assertThat(stats, hasEntry("Ibytes", 52_967_869_005L));
        assertThat(stats, hasEntry("Obytes", 35_703_541_052L));
        assertThat(stats, hasEntry("Ipkts", 0L));
    }

    @Test
    void testParseNetstatEmptyAddress() {
        // DragonFly BSD's loopback link-level row has no address
        Map<String, Long> stats = BsdNetworkIF.parseNetstat("""
                Name    Mtu   Network       Address            Ipkts Ierrs     Ibytes    Opkts Oerrs     Obytes  Coll
                lo0     16384 <Link#2>                       6650884     0  998481532  6650884     0  998481532     0
                """.lines().toList());
        assertThat(stats, hasEntry("Ipkts", 6_650_884L));
        assertThat(stats, hasEntry("Ibytes", 998_481_532L));
        assertThat(stats, hasEntry("Obytes", 998_481_532L));
        assertThat(stats, hasEntry("Coll", 0L));
    }

    @Test
    void testParseNetstatOpenBsd() {
        // OpenBSD prints only the byte counts with -b, and the packet counts under other headings without it
        Map<String, Long> bytes = BsdNetworkIF.parseNetstat("""
                Name    Mtu   Network     Address               Ibytes     Obytes
                bge2    1500  <Link>      74:86:7a:d0:13:c0  386813736  849298713
                bge2    1500  46.23.83.68 46.23.83.68        386813736  849298713
                """.lines().toList());
        assertThat(bytes, is(aMapWithSize(2)));
        assertThat(bytes, hasEntry("Ibytes", 386_813_736L));
        assertThat(bytes, hasEntry("Obytes", 849_298_713L));

        Map<String, Long> packets = BsdNetworkIF.parseNetstat("""
                Name    Mtu   Network     Address              Ipkts Ifail    Opkts Ofail Colls
                bge2    1500  <Link>      74:86:7a:d0:13:c0  2025726     0  1065503     1     0
                """.lines().toList());
        assertThat(packets, hasEntry("Ipkts", 2_025_726L));
        assertThat(packets, hasEntry("Ofail", 1L));
        assertThat(packets, hasEntry("Colls", 0L));

        Map<String, Long> loopback = BsdNetworkIF.parseNetstat("""
                Name    Mtu   Network     Address               Ibytes     Obytes
                lo0     32768 <Link>                          70038229   70038229
                """.lines().toList());
        assertThat(loopback, hasEntry("Ibytes", 70_038_229L));
    }

    @Test
    void testParseNetstatNetBsd() {
        // Like OpenBSD, NetBSD prints only the byte counts with -b; without it, the packet counts and Colls
        Map<String, Long> bytes = BsdNetworkIF.parseNetstat("""
                Name  Mtu   Network       Address               Ibytes     Obytes
                vioif 1500  <Link>        00:15:5d:c4:00:28 8419940203  684694279
                vioif 1500  fe80::/64     fe80::215:5dff:fe 8419940203  684694279
                """.lines().toList());
        assertThat(bytes, is(aMapWithSize(2)));
        assertThat(bytes, hasEntry("Ibytes", 8_419_940_203L));
        assertThat(bytes, hasEntry("Obytes", 684_694_279L));

        Map<String, Long> packets = BsdNetworkIF.parseNetstat("""
                Name  Mtu   Network       Address              Ipkts Ierrs    Opkts Oerrs Colls
                vioif 1500  <Link>        00:15:5d:c4:00:28 106586638     1  2503607     0     0
                vioif 1500  fe80::/64     fe80::215:5dff:fe 106586638     1  2503607     0     0
                """.lines().toList());
        assertThat(packets, is(aMapWithSize(5)));
        assertThat(packets, hasEntry("Ipkts", 106_586_638L));
        assertThat(packets, hasEntry("Ierrs", 1L));
        assertThat(packets, hasEntry("Opkts", 2_503_607L));
        assertThat(packets, hasEntry("Oerrs", 0L));
        assertThat(packets, hasEntry("Colls", 0L));

        // The loopback and npflog link-level rows have no address
        Map<String, Long> loopback = BsdNetworkIF.parseNetstat("""
                Name  Mtu   Network       Address               Ibytes     Obytes
                lo0   33624 <Link>                           178247481  178247481
                lo0   33624 127/8         127.0.0.1          178247481  178247481
                """.lines().toList());
        assertThat(loopback, hasEntry("Ibytes", 178_247_481L));
        assertThat(loopback, hasEntry("Obytes", 178_247_481L));
        Map<String, Long> npflog = BsdNetworkIF.parseNetstat("""
                Name  Mtu   Network       Address              Ipkts Ierrs    Opkts Oerrs Colls
                npflo 0     <Link>                                 0     0  1988557     0     0
                """.lines().toList());
        assertThat(npflog, hasEntry("Ipkts", 0L));
        assertThat(npflog, hasEntry("Opkts", 1_988_557L));
    }

    @Test
    void testParseNetstatNoLinkRow() {
        assertThat(BsdNetworkIF.parseNetstat(Collections.emptyList()), is(anEmptyMap()));
        // netstat: interface does not exist
        assertThat(
                BsdNetworkIF.parseNetstat(
                        Collections.singletonList("Name    Mtu   Network     Address               Ibytes     Obytes")),
                is(anEmptyMap()));
        assertThat(BsdNetworkIF.parseNetstat(Collections.singletonList("garbage")), is(anEmptyMap()));
    }

    private static long speed(String media) {
        return BsdNetworkIF.parseMediaSpeed(List.of("vtnet0: flags=1008843<UP,BROADCAST,RUNNING> metric 0 mtu 9000",
                "\t" + media, "\tstatus: active"));
    }

    @Test
    void testParseMediaSpeed() {
        // FreeBSD 16.0-CURRENT, a virtio NIC and a Realtek one
        assertThat(speed("media: Ethernet autoselect (10Gbase-T <full-duplex>)"), is(10_000_000_000L));
        assertThat(speed("media: Ethernet autoselect (1000baseT <full-duplex>)"), is(1_000_000_000L));
        // OpenBSD 7.9, with flow control after the duplex
        assertThat(speed("media: Ethernet autoselect (1000baseT full-duplex,rxpause,txpause)"), is(1_000_000_000L));
        // Other Ethernet spellings, and a wireless rate
        assertThat(speed("media: Ethernet autoselect (2500Base-T <full-duplex>)"), is(2_500_000_000L));
        assertThat(speed("media: Ethernet autoselect (100baseTX <full-duplex>)"), is(100_000_000L));
        assertThat(speed("media: IEEE802.11 autoselect (OFDM/54Mbps mode 11g)"), is(54_000_000L));
        // Fixed media, with no parentheses
        assertThat(speed("media: Ethernet 10Gbase-SR <full-duplex>"), is(10_000_000_000L));
        // The active type, in parentheses, rather than the configured one before it
        assertThat(speed("media: Ethernet 1000baseT <full-duplex> (100baseTX <full-duplex>)"), is(100_000_000L));
        // No link: OpenBSD prints (none), FreeBSD leaves the parentheses off
        assertThat(speed("media: Ethernet autoselect (none)"), is(0L));
        assertThat(speed("media: Ethernet autoselect"), is(0L));
        // Fixed media names its rate with nothing connected; the status says there is no link
        assertThat(BsdNetworkIF.parseMediaSpeed(List.of("em0: flags=8802<BROADCAST,SIMPLEX,MULTICAST> mtu 1500",
                "\tmedia: Ethernet 1000baseT <full-duplex>", "\tstatus: no carrier")), is(0L));
        assertThat(BsdNetworkIF.parseMediaSpeed(List.of("wlan0: flags=8843<UP,BROADCAST,RUNNING> mtu 1500",
                "\tmedia: IEEE802.11 autoselect (OFDM/54Mbps mode 11g)", "\tstatus: no network")), is(0L));
        // A wireless interface that is up is "associated", not "active"
        assertThat(
                BsdNetworkIF.parseMediaSpeed(List.of("wlan0: flags=8843<UP,BROADCAST,RUNNING> mtu 1500",
                        "\tmedia: IEEE802.11 autoselect (OFDM/54Mbps mode 11g)", "\tstatus: associated")),
                is(54_000_000L));
        // No media line at all, as for NetBSD's virtio interface
        assertThat(BsdNetworkIF.parseMediaSpeed(List.of("vioif0: flags=0x8843<UP,BROADCAST,RUNNING> mtu 1500")),
                is(0L));
        assertThat(BsdNetworkIF.parseMediaSpeed(Collections.emptyList()), is(0L));
    }
}
