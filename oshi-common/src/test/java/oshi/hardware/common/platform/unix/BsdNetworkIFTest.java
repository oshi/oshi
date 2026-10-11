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
    void testParseNetstatNoLinkRow() {
        assertThat(BsdNetworkIF.parseNetstat(Collections.emptyList()), is(anEmptyMap()));
        // netstat: interface does not exist
        assertThat(
                BsdNetworkIF.parseNetstat(
                        Collections.singletonList("Name    Mtu   Network     Address               Ibytes     Obytes")),
                is(anEmptyMap()));
        assertThat(BsdNetworkIF.parseNetstat(Collections.singletonList("garbage")), is(anEmptyMap()));
    }
}
