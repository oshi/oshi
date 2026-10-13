/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.freebsd.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.HWPartition;
import oshi.util.Constants;

class GeomPartListTest {

    // geom part list on a FreeBSD 16 VM with three GPT disks, trimmed of fields OSHI does not read. Each geom ends with
    // a Consumers section naming its own disk, which is not a partition.
    private static final String GEOM_PART_LIST = """
            Geom name: da1
            modified: false
            state: OK
            scheme: GPT
            Providers:
            1. Name: da1p1
               Mediasize: 1099511586816 (1.0T)
               Sectorsize: 512
               Mode: r1w1e1
               rawuuid: deb7728b-8f91-11ef-8e4d-00155dc40030
               rawtype: 516e7cb6-6ecf-11d6-8ff8-00022d09712b
               label: cfarm430
               length: 1099511586816
               offset: 20480
               type: freebsd-ufs
               index: 1
            Consumers:
            1. Name: da1
               Mediasize: 1099511627776 (1.0T)
               Sectorsize: 512
               Mode: r1w1e2

            Geom name: da0
            modified: false
            state: OK
            scheme: GPT
            Providers:
            1. Name: da0p1
               Mediasize: 272629760 (260M)
               Sectorsize: 512
               Mode: r1w1e1
               rawuuid: d454a567-8f91-11ef-8e4d-00155dc40030
               rawtype: c12a7328-f81f-11d2-ba4b-00a0c93ec93b
               label:\s
               length: 272629760
               offset: 20480
               type: efi
               index: 1
            2. Name: da0p2
               Mediasize: 85626675200 (80G)
               Sectorsize: 512
               Mode: r1w1e1
               rawuuid: d4553393-8f91-11ef-8e4d-00155dc40030
               rawtype: 516e7cb6-6ecf-11d6-8ff8-00022d09712b
               label: rootfs
               length: 85626675200
               offset: 272650240
               type: freebsd-ufs
               index: 2
            Consumers:
            1. Name: da0
               Mediasize: 85899345920 (80G)
               Sectorsize: 512
               Mode: r2w2e4

            Geom name: da2
            modified: false
            state: OK
            scheme: GPT
            Providers:
            1. Name: da2p1
               Mediasize: 8589893632 (8.0G)
               Sectorsize: 512
               Mode: r1w1e0
               rawuuid: 43188d88-22ef-11f1-b400-00155dc40030
               rawtype: 516e7cb5-6ecf-11d6-8ff8-00022d09712b
               label:\s
               length: 8589893632
               offset: 20480
               type: freebsd-swap
               index: 1
            Consumers:
            1. Name: da2
               Mediasize: 8589934592 (8.0G)
               Sectorsize: 512
               Mode: r1w1e1
            """;

    private static final Map<String, String> MOUNTS = Map.of("da0p2", "/", "da0p1", "/boot/efi", "da1p1", "/home");

    private static List<String> names(List<HWPartition> partitions) {
        return partitions.stream().map(HWPartition::getName).toList();
    }

    @Test
    void testParsePartitions() {
        Map<String, List<HWPartition>> map = GeomPartList.parsePartitions(GEOM_PART_LIST.lines().toList(), MOUNTS,
                part -> part.equals("da0p2") ? 119 : 0);
        assertThat(map.keySet(), containsInAnyOrder("da0", "da1", "da2"));

        // Only providers, each under its own disk: no consumer becomes a partition of the next geom
        List<HWPartition> da0 = map.get("da0");
        List<HWPartition> da1 = map.get("da1");
        List<HWPartition> da2 = map.get("da2");
        assertNotNull(da0);
        assertNotNull(da1);
        assertNotNull(da2);
        assertThat(names(da0), contains("da0p1", "da0p2"));
        assertThat(names(da1), contains("da1p1"));
        assertThat(names(da2), contains("da2p1"));

        HWPartition root = da0.get(1);
        assertThat(root.getIdentification(), is("da0p2"));
        assertThat(root.getType(), is("freebsd-ufs"));
        assertThat(root.getUuid(), is("d4553393-8f91-11ef-8e4d-00155dc40030"));
        assertThat(root.getLabel(), is("rootfs"));
        assertThat(root.getSize(), is(85626675200L));
        assertThat(root.getMajor(), is(0));
        assertThat(root.getMinor(), is(119));
        assertThat(root.getMountPoint(), is("/"));

        HWPartition efi = da0.get(0);
        assertThat(efi.getLabel(), is(""));
        assertThat(efi.getMountPoint(), is("/boot/efi"));
        HWPartition swap = da2.get(0);
        assertThat(swap.getType(), is("freebsd-swap"));
        assertThat(swap.getMountPoint(), is(""));
        assertThat(da1.get(0).getLabel(), is("cfarm430"));
    }

    @Test
    void testParsePartitionsEmpty() {
        assertThat(GeomPartList.parsePartitions(Collections.emptyList(), MOUNTS, part -> 0), is(anEmptyMap()));
    }

    @Test
    void testPartitionWithoutDetails() {
        Map<String, List<HWPartition>> map = GeomPartList.parsePartitions(
                List.of("Geom name: ada0", "Providers:", "1. Name: ada0p1", "Consumers:", "1. Name: ada0"),
                Collections.emptyMap(), part -> 0);
        List<HWPartition> ada0 = map.get("ada0");
        assertNotNull(ada0);
        assertThat(ada0.size(), is(1));
        assertThat(ada0.get(0).getType(), is(Constants.UNKNOWN));
        assertThat(ada0.get(0).getSize(), is(0L));
    }
}
