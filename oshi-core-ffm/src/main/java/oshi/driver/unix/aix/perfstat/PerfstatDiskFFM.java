/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import static oshi.ffm.platform.unix.aix.PerfstatFunctions.PERFSTAT_DISK_T_SIZE;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.PERFSTAT_ID_T_SIZE;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.diskBsize;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.diskDescription;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.diskName;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.diskQdepth;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.diskRblks;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.diskSize;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.diskTime;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.diskWblks;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.diskXfers;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.perfstat_disk;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;

/**
 * FFM-backed driver for {@code perfstat_disk}, mirroring {@code oshi.driver.unix.aix.perfstat.PerfstatDiskJNA}.
 */
@ThreadSafe
public final class PerfstatDiskFFM {

    private static final Logger LOG = LoggerFactory.getLogger(PerfstatDiskFFM.class);

    private PerfstatDiskFFM() {
    }

    /** POJO mirror of the {@code perfstat_disk_t} fields OSHI consumes. */
    public static final class Disk {
        public String name = "";
        public String description = "";
        public long size;
        public long bsize;
        public long xfers;
        public long wblks;
        public long rblks;
        public long qdepth;
        public long time;
    }

    /**
     * Queries {@code perfstat_disk} for all disks.
     *
     * @return one {@link Disk} per disk, or an empty array on error
     */
    public static Disk[] queryDiskStats() {
        return ForeignFunctions.callInArenaOrDefault(arena -> {
            int count = perfstat_disk(MemorySegment.NULL, MemorySegment.NULL, PERFSTAT_DISK_T_SIZE, 0);
            if (count <= 0) {
                return new Disk[0];
            }
            MemorySegment buf = arena.allocate((long) PERFSTAT_DISK_T_SIZE * count);
            MemorySegment firstName = arena.allocate(PERFSTAT_ID_T_SIZE);
            int ret = perfstat_disk(firstName, buf, PERFSTAT_DISK_T_SIZE, count);
            if (ret <= 0) {
                return new Disk[0];
            }
            Disk[] result = new Disk[ret];
            for (int i = 0; i < ret; i++) {
                long off = (long) i * PERFSTAT_DISK_T_SIZE;
                Disk d = new Disk();
                d.name = diskName(buf, off);
                d.description = diskDescription(buf, off);
                d.size = diskSize(buf, off);
                d.bsize = diskBsize(buf, off);
                d.xfers = diskXfers(buf, off);
                d.wblks = diskWblks(buf, off);
                d.rblks = diskRblks(buf, off);
                d.qdepth = diskQdepth(buf, off);
                d.time = diskTime(buf, off);
                result[i] = d;
            }
            return result;
        }, LOG, Level.TRACE, "Failed to query disk statistics", new Disk[0]);
    }
}
