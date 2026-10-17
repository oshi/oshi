/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.HWPartition;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

class NetBsdHWDiskStoreTest {

    /** Captured from {@code iostat -x -I} on NetBSD 10.1. */
    private static final List<String> FIRST = """
            device  read KB/t    xfr   time     MB   write KB/t    xfr   time     MB
            ld0         26.36   2087   8.23    53.72      25.42  74725   8.23  1855.30
            fd0          3.34    179  92.38     0.58       0.00      0  92.38     0.00
            dk0         27.45   1995  14.32    53.47      25.42  74725  14.32  1855.30
            cd0          0.00      0   0.00     0.00       0.00      0   0.00     0.00
            """.lines().toList();

    /** The same drives two seconds later. The write KB/t figure has fallen while the transfer count has risen. */
    private static final List<String> SECOND = """
            device  read KB/t    xfr   time     MB   write KB/t    xfr   time     MB
            ld0         26.36   2087   8.23    53.72      25.40  74833   8.23  1856.52
            fd0          3.34    179  92.38     0.58       0.00      0  92.38     0.00
            dk0         27.45   1995  14.32    53.47      25.40  74833  14.32  1856.52
            cd0          0.00      0   0.00     0.00       0.00      0   0.00     0.00
            """.lines().toList();

    private static NetBsdHWDiskStore store(String name, List<String> iostat) {
        return new NetBsdHWDiskStore(name, "model", "serial", 1024L, () -> iostat);
    }

    @Test
    void testUpdateAttributes() {
        NetBsdHWDiskStore ld0 = store("ld0", FIRST);
        assertTrue(ld0.updateAttributes());
        assertThat(ld0.getReads(), is(2087L));
        assertThat(ld0.getWrites(), is(74725L));
        // The MB columns, not KB-per-transfer times transfers
        assertThat(ld0.getReadBytes(), is((long) (53.72 * 1024 * 1024)));
        assertThat(ld0.getWriteBytes(), is((long) (1855.30 * 1024 * 1024)));
        // One drive-busy total, not the two identical columns added together
        assertThat(ld0.getTransferTime(), is(8230L));
        assertThat(ld0.getTimeStamp(), greaterThanOrEqualTo(1L));
    }

    @Test
    void testBytesNeverGoBackwards() {
        // iostat's write KB/t for ld0 rounds down from 25.42 to 25.40 between these two samples while the transfer
        // count rises, so a KB/t times transfers reading would report fewer bytes written than a moment earlier
        NetBsdHWDiskStore before = store("ld0", FIRST);
        NetBsdHWDiskStore after = store("ld0", SECOND);
        assertTrue(before.updateAttributes());
        assertTrue(after.updateAttributes());
        assertThat(after.getWrites(), greaterThanOrEqualTo(before.getWrites()));
        assertThat(after.getWriteBytes(), greaterThanOrEqualTo(before.getWriteBytes()));
        assertThat(after.getReadBytes(), greaterThanOrEqualTo(before.getReadBytes()));
    }

    @Test
    void testIdleAndMissingDrives() {
        NetBsdHWDiskStore cd0 = store("cd0", FIRST);
        assertTrue(cd0.updateAttributes());
        assertThat(cd0.getReadBytes(), is(0L));
        assertThat(cd0.getWriteBytes(), is(0L));
        assertThat(cd0.getTransferTime(), is(0L));

        NetBsdHWDiskStore absent = store("sd9", FIRST);
        assertFalse(absent.updateAttributes());
    }

    /**
     * The live buffer from NetBSD 10.1 on cfarm428, followed by its boot copy, which repeats it without timestamps. The
     * last two wedges are invented: one on a 4096-byte-sector disk, and one named by a GPT label rather than its GUID.
     */
    private static final List<String> DMESG = """
            [     1.000004] ld4: 65536 MB, 16383 cyl, 16 head, 63 sec, 512 bytes/sect x 134217728 sectors
            [     1.000004] ld6: 4096 MB, 8322 cyl, 16 head, 63 sec, 512 bytes/sect x 8388608 sectors
            [     1.000004] sd0: 1863 GB, 243201 cyl, 255 head, 63 sec, 4096 bytes/sect x 488378646 sectors
            [     1.373709] ld4: GPT GUID: 13c767b2-2c2a-46d9-8095-22064f48ade5
            [     1.373709] dk0 at ld4: "7c94c5a0-89a4-42c2-8088-0f88dd11e8fa", 409600 blocks at 64, type: ntfs
            [     1.373709] dk1 at ld4: "bce3cb3f-b5ff-4aac-8049-5f748b710db0", 133807967 blocks at 409728, type: ffs
            [     1.390720] dk3 at ld6: "ece9f3bb-9a88-4229-845c-4b23a4a00bc6", 8388528 blocks at 40, type: swap
            [     1.390720] dk4 at sd0: "data, old", 1000 blocks at 6, type: ffs
            ld4: 65536 MB, 16383 cyl, 16 head, 63 sec, 512 bytes/sect x 134217728 sectors
            dk0 at ld4: "7c94c5a0-89a4-42c2-8088-0f88dd11e8fa", 409600 blocks at 64, type: ntfs
            dk1 at ld4: "bce3cb3f-b5ff-4aac-8049-5f748b710db0", 133807967 blocks at 409728, type: ffs
            boot device: dk1
            """.lines().toList();

    /** {@code df} on the same machine. */
    private static final List<String> DF = """
            Filesystem     512-blocks         Used        Avail %Cap Mounted on
            /dev/dk1        129729712     36578376     86664852  29% /
            /dev/dk0           409160        20904       388256   5% /boot
            tmpfs             8375792        37496      8338296   0% /tmp
            /dev/dk4             8000         1000         7000  13% /mnt/old disk
            """.lines().toList();

    @Test
    void testParseMounts() {
        Map<String, String> mounts = NetBsdHWDiskStore.parseMounts(DF);
        assertThat(mounts.size(), is(3));
        assertThat(mounts.get("dk1"), is("/"));
        assertThat(mounts.get("dk0"), is("/boot"));
        assertThat(mounts.get("dk4"), is("/mnt/old disk"));
    }

    @Test
    void testParseWedges() {
        Map<String, List<HWPartition>> wedges = NetBsdHWDiskStore.parseWedges(DMESG, NetBsdHWDiskStore.parseMounts(DF),
                (disk, part) -> new Pair<>(168, ParseUtil.getFirstIntValue(disk)));
        assertThat(wedges.keySet(), contains("ld4", "ld6", "sd0"));

        // Each wedge once, though the boot copy repeats two of them
        List<HWPartition> ld4 = wedges.get("ld4");
        assertNotNull(ld4);
        assertThat(ld4.size(), is(2));
        HWPartition dk1 = ld4.get(1);
        assertThat(dk1.getIdentification(), is("dk1"));
        assertThat(dk1.getName(), is("dk1"));
        assertThat(dk1.getType(), is("ffs"));
        assertThat(dk1.getUuid(), is("bce3cb3f-b5ff-4aac-8049-5f748b710db0"));
        assertThat(dk1.getLabel(), is(""));
        assertThat(dk1.getSize(), is(133807967L * 512));
        assertThat(dk1.getMajor(), is(168));
        assertThat(dk1.getMinor(), is(1));
        assertThat(dk1.getMountPoint(), is("/"));
        assertThat(ld4.get(0).getMountPoint(), is("/boot"));

        // Swap is not mounted
        List<HWPartition> ld6 = wedges.get("ld6");
        assertNotNull(ld6);
        assertThat(ld6.get(0).getType(), is("swap"));
        assertThat(ld6.get(0).getMountPoint(), is(""));

        // Sized in the parent's sectors, and named by a label that holds a comma
        List<HWPartition> sd0 = wedges.get("sd0");
        assertNotNull(sd0);
        HWPartition dk4 = sd0.get(0);
        assertThat(dk4.getSize(), is(1000L * 4096));
        assertThat(dk4.getUuid(), is(""));
        assertThat(dk4.getLabel(), is("data, old"));
        assertThat(dk4.getMountPoint(), is("/mnt/old disk"));
    }

    @Test
    void testParseWedgesWithoutParentGeometry() {
        // A parent whose geometry line is gone from both buffers is assumed to have 512-byte sectors
        Map<String, List<HWPartition>> wedges = NetBsdHWDiskStore.parseWedges(
                List.of("dk2 at ld5: \"c4040fb3-6246-4f8a-9718-9d613c827f82\", 2048 blocks at 2048, type: ffs"),
                Map.of(), (disk, part) -> new Pair<>(0, 0));
        List<HWPartition> ld5 = wedges.get("ld5");
        assertNotNull(ld5);
        assertThat(ld5.get(0).getSize(), is(2048L * 512));
        assertThat(NetBsdHWDiskStore
                .parseWedges(List.of("boot device: dk1"), Map.of(), (disk, part) -> new Pair<>(0, 0)).isEmpty(),
                is(true));
    }
}
