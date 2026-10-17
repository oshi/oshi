/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FreeBsdFileSystemTest {

    @Test
    void testParseGeomPartListTypical() {
        List<String> lines = """
                Geom name: ada0
                Providers:
                1. Name: ada0p1
                   rawuuid: 3e1cbe42-e2b9-11e6-85c6-0025909460ac
                2. Name: ada0p2
                   rawuuid: 3e2e5580-e2b9-11e6-85c6-0025909460ac
                3. Name: ada0p3
                   rawuuid: 3e3ed3be-e2b9-11e6-85c6-0025909460ac
                """.lines().toList();
        Map<String, String> result = FreeBsdFileSystem.parseGeomPartList(lines);
        // Each partition under its device name and its GPT ID
        assertThat(result, is(aMapWithSize(6)));
        assertThat(result.get("gptid/3e2e5580-e2b9-11e6-85c6-0025909460ac"),
                is("3e2e5580-e2b9-11e6-85c6-0025909460ac"));
        assertThat(result.get("ada0p1"), is("3e1cbe42-e2b9-11e6-85c6-0025909460ac"));
        assertThat(result.get("ada0p2"), is("3e2e5580-e2b9-11e6-85c6-0025909460ac"));
        assertThat(result.get("ada0p3"), is("3e3ed3be-e2b9-11e6-85c6-0025909460ac"));
    }

    @Test
    void testUuidForVolume() {
        // geom part list on FreeBSD 16.0-CURRENT (cfarm430), trimmed, with each disk's consumer section
        List<String> lines = """
                Geom name: da0
                Providers:
                1. Name: da0p1
                   rawuuid: d454a567-8f91-11ef-8e4d-00155dc40030
                   label:\s
                   type: efi
                2. Name: da0p2
                   rawuuid: d4553393-8f91-11ef-8e4d-00155dc40030
                   label: rootfs
                   type: freebsd-ufs
                Consumers:
                1. Name: da0
                Geom name: da1
                Providers:
                1. Name: da1p1
                   rawuuid: deb7728b-8f91-11ef-8e4d-00155dc40030
                   label: cfarm430
                   type: freebsd-ufs
                Consumers:
                1. Name: da1
                """.lines().toList();
        Map<String, String> uuids = FreeBsdFileSystem.parseGeomPartList(lines);
        // The volumes mount -p printed there: /, /boot/efi and /home. Only / was matched when the lookup went by the
        // last element of the mount point
        assertThat(FreeBsdFileSystem.uuidForVolume("/dev/da0p2", uuids), is("d4553393-8f91-11ef-8e4d-00155dc40030"));
        assertThat(FreeBsdFileSystem.uuidForVolume("/dev/da0p1", uuids), is("d454a567-8f91-11ef-8e4d-00155dc40030"));
        assertThat(FreeBsdFileSystem.uuidForVolume("/dev/da1p1", uuids), is("deb7728b-8f91-11ef-8e4d-00155dc40030"));
        // The same partitions mounted by GPT label or GPT ID
        assertThat(FreeBsdFileSystem.uuidForVolume("/dev/gpt/rootfs", uuids),
                is("d4553393-8f91-11ef-8e4d-00155dc40030"));
        assertThat(FreeBsdFileSystem.uuidForVolume("/dev/gptid/deb7728b-8f91-11ef-8e4d-00155dc40030", uuids),
                is("deb7728b-8f91-11ef-8e4d-00155dc40030"));
        // A disk is not a partition, and tmpfs and ZFS datasets are not devices
        assertThat(FreeBsdFileSystem.uuidForVolume("/dev/da0", uuids), is(""));
        assertThat(FreeBsdFileSystem.uuidForVolume("tmpfs", uuids), is(""));
        assertThat(FreeBsdFileSystem.uuidForVolume("zroot/ROOT/default", uuids), is(""));
        // The unlabeled efi partition adds no gpt/ entry
        assertThat(uuids.containsKey("gpt/"), is(false));
    }

    @Test
    void testParseGeomPartListEmpty() {
        Map<String, String> result = FreeBsdFileSystem.parseGeomPartList(Collections.emptyList());
        assertThat(result, is(anEmptyMap()));
    }

}
