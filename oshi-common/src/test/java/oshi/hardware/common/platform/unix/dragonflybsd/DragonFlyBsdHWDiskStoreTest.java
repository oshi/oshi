/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.dragonflybsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import oshi.hardware.HWDiskStore;
import oshi.util.Constants;

class DragonFlyBsdHWDiskStoreTest {

    private static final int DEVSTAT_SIZE = 200;

    // Counters read from kern.devstat.all on a DragonFly BSD 6.4 virtio guest up 207 days
    private static final byte[] DEVSTAT = devstat(2, devstatRecord("vbd", 0, 5_705_682_519_552L, 41_767_741_681_664L,
            87_615_332L, 637_981_330L, 1, 2_026_184L, 62_532L),
            devstatRecord("md", 0, 20_992L, 0L, 17L, 0L, 0, 0L, 0L));

    @Test
    void testParseDevstat() {
        Map<String, DragonFlyBsdHWDiskStore> stats = DragonFlyBsdHWDiskStore.parseDevstat(DEVSTAT, () -> DEVSTAT);
        assertThat(stats.keySet(), contains("vbd0", "md0"));
        DragonFlyBsdHWDiskStore vbd = stats.get("vbd0");
        assertNotNull(vbd);
        assertThat(vbd.getReads(), is(87_615_332L));
        assertThat(vbd.getReadBytes(), is(5_705_682_519_552L));
        assertThat(vbd.getWrites(), is(637_981_330L));
        assertThat(vbd.getWriteBytes(), is(41_767_741_681_664L));
        assertThat(vbd.getCurrentQueueLength(), is(1L));
        assertThat(vbd.getTransferTime(), is(2_026_184_062L));
    }

    @Test
    void testParseDevstatFullLengthName() {
        // A name filling all 16 bytes has no terminating NUL
        byte[] buffer = devstat(1, devstatRecord("abcdefghijklmnop", 12, 0L, 0L, 0L, 0L, 0, 0L, 0L));
        Map<String, DragonFlyBsdHWDiskStore> stats = DragonFlyBsdHWDiskStore.parseDevstat(buffer, () -> buffer);
        assertThat(stats.keySet(), contains("abcdefghijklmnop12"));
    }

    @Test
    void testParseDevstatTruncated() {
        assertThat(DragonFlyBsdHWDiskStore.parseDevstat(new byte[0], () -> new byte[0]), is(anEmptyMap()));
        byte[] truncated = Arrays.copyOf(DEVSTAT, DEVSTAT.length - 1);
        assertThat(DragonFlyBsdHWDiskStore.parseDevstat(truncated, () -> truncated).keySet(), contains("vbd0"));
    }

    @Test
    void testGetDisks() {
        // kern.disks also lists unconfigured vnode disks, which have no devstat record
        List<HWDiskStore> disks = DragonFlyBsdHWDiskStore.getDisks("vbd0 vn3 vn2 vn1 vn0 md0", () -> DEVSTAT);
        assertThat(disks.stream().map(HWDiskStore::getName).toList(), contains("vbd0", "md0"));
        HWDiskStore vbd = disks.get(0);
        assertThat(vbd.getModel(), is(Constants.UNKNOWN));
        assertThat(vbd.getSize(), is(0L));
        assertThat(vbd.getReads(), is(87_615_332L));
        assertThat(vbd.getWriteBytes(), is(41_767_741_681_664L));
        assertThat(vbd.getTransferTime(), is(2_026_184_062L));
        assertThat(vbd.getPartitions(), is(empty()));
        assertThat(vbd.updateAttributes(), is(true));

        assertThat(DragonFlyBsdHWDiskStore.getDisks("vn0", () -> DEVSTAT), is(empty()));
        assertThat(DragonFlyBsdHWDiskStore.getDisks("", () -> new byte[0]), is(empty()));
    }

    @Test
    void testUpdateAttributesDiskGone() {
        AtomicReference<byte[]> sysctl = new AtomicReference<>(DEVSTAT);
        HWDiskStore md = DragonFlyBsdHWDiskStore.getDisks("md0", sysctl::get).get(0);
        sysctl.set(devstat(3, devstatRecord("vbd", 0, 0L, 0L, 0L, 0L, 0, 0L, 0L)));
        assertThat(md.updateAttributes(), is(false));
    }

    private static byte[] devstat(long generation, byte[]... records) {
        ByteBuffer bb = ByteBuffer.allocate(8 + DEVSTAT_SIZE * records.length).order(ByteOrder.nativeOrder());
        bb.putLong(generation);
        for (byte[] r : records) {
            bb.put(r);
        }
        return bb.array();
    }

    private static byte[] devstatRecord(String name, int unit, long bytesRead, long bytesWritten, long numReads,
            long numWrites, int busyCount, long busySec, long busyUsec) {
        ByteBuffer bb = ByteBuffer.allocate(DEVSTAT_SIZE).order(ByteOrder.nativeOrder());
        bb.put(12, name.getBytes(StandardCharsets.US_ASCII));
        bb.putInt(28, unit);
        bb.putLong(32, bytesRead);
        bb.putLong(40, bytesWritten);
        bb.putLong(56, numReads);
        bb.putLong(64, numWrites);
        bb.putInt(88, busyCount);
        bb.putLong(136, busySec);
        bb.putLong(144, busyUsec);
        return bb.array();
    }
}
