/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.dragonflybsd;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * DragonFly BSD hard disk implementation. DragonFly has neither {@code geom} nor FreeBSD's {@code iostat -x}, so the
 * statistics come from the raw {@code struct devstat} records in the {@code kern.devstat.all} sysctl. The model, serial
 * number, size and partitions are only readable through the disk devices themselves, which need root, so they are left
 * unknown.
 */
@ThreadSafe
public final class DragonFlyBsdHWDiskStore extends AbstractHWDiskStore {

    /** The sysctl holding a generation counter followed by one {@code struct devstat} per device. */
    public static final String DEVSTAT_SYSCTL = "kern.devstat.all";

    // Offsets in struct devstat from sys/devicestat.h on x86_64, the only architecture DragonFly supports
    private static final int HEADER_SIZE = 8; // long generation
    private static final int DEVSTAT_SIZE = 200;
    private static final int NAME_OFFSET = 12;
    private static final int NAME_LEN = 16;
    private static final int UNIT_OFFSET = 28;
    private static final int BYTES_READ_OFFSET = 32;
    private static final int BYTES_WRITTEN_OFFSET = 40;
    private static final int NUM_READS_OFFSET = 56;
    private static final int NUM_WRITES_OFFSET = 64;
    private static final int BUSY_COUNT_OFFSET = 88;
    private static final int BUSY_TIME_OFFSET = 136; // struct timeval: long tv_sec, long tv_usec

    private final Supplier<byte[]> devstat;

    private DragonFlyBsdHWDiskStore(String name, Supplier<byte[]> devstat) {
        super(name, Constants.UNKNOWN, Constants.UNKNOWN, 0L);
        this.devstat = devstat;
    }

    @Override
    public synchronized boolean updateAttributes() {
        DragonFlyBsdHWDiskStore current = parseDevstat(devstat.get(), devstat).get(getName());
        if (current == null) {
            return false;
        }
        setDiskStats(current.getReads(), current.getReadBytes(), current.getWrites(), current.getWriteBytes(),
                current.getCurrentQueueLength(), current.getTransferTime(), current.getTimeStamp());
        return true;
    }

    /**
     * Gets the disks on this machine.
     *
     * @param kernDisks the value of the {@code kern.disks} sysctl, a space-separated list of disk names
     * @param devstat   reads the {@code kern.devstat.all} sysctl, returning an empty array on failure
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks(String kernDisks, Supplier<byte[]> devstat) {
        List<String> devices = Arrays.asList(ParseUtil.whitespaces.split(kernDisks.trim(), -1));
        List<HWDiskStore> diskList = new ArrayList<>();
        for (DragonFlyBsdHWDiskStore store : parseDevstat(devstat.get(), devstat).values()) {
            if (devices.contains(store.getName())) {
                diskList.add(store);
            }
        }
        return diskList;
    }

    /**
     * Parses the {@code kern.devstat.all} sysctl.
     *
     * @param buffer  the sysctl's bytes, in native byte order
     * @param devstat reads the sysctl again, for the stores' {@link #updateAttributes()}
     * @return a store for each device, keyed by its name and unit number, in the kernel's order
     */
    static Map<String, DragonFlyBsdHWDiskStore> parseDevstat(byte[] buffer, Supplier<byte[]> devstat) {
        Map<String, DragonFlyBsdHWDiskStore> map = new LinkedHashMap<>();
        ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder());
        long now = System.currentTimeMillis();
        for (int offset = HEADER_SIZE; offset + DEVSTAT_SIZE <= buffer.length; offset += DEVSTAT_SIZE) {
            int len = 0;
            while (len < NAME_LEN && buffer[offset + NAME_OFFSET + len] != 0) {
                len++;
            }
            String name = new String(buffer, offset + NAME_OFFSET, len, StandardCharsets.US_ASCII)
                    + bb.getInt(offset + UNIT_OFFSET);
            long busyMillis = bb.getLong(offset + BUSY_TIME_OFFSET) * 1000L
                    + bb.getLong(offset + BUSY_TIME_OFFSET + 8) / 1000L;
            DragonFlyBsdHWDiskStore store = new DragonFlyBsdHWDiskStore(name, devstat);
            store.setDiskStats(bb.getLong(offset + NUM_READS_OFFSET), bb.getLong(offset + BYTES_READ_OFFSET),
                    bb.getLong(offset + NUM_WRITES_OFFSET), bb.getLong(offset + BYTES_WRITTEN_OFFSET),
                    bb.getInt(offset + BUSY_COUNT_OFFSET), busyMillis, now);
            map.put(name, store);
        }
        return map;
    }
}
