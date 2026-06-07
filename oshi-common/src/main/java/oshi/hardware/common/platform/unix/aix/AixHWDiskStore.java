/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractHWDiskStore;

/**
 * Abstract base for AIX HWDiskStore. The {@link #updateAttributes()} math (rough read/write split from total transfers
 * and block counts, monotonic-increase enforcement) is shared; subclasses fetch the per-disk stats from their
 * respective perfstat driver.
 */
@ThreadSafe
public abstract class AixHWDiskStore extends AbstractHWDiskStore {

    /** Per-disk stats fields the AIX HAL reads. Both JNA and FFM concrete subclasses populate this. */
    public static final class DiskStats {
        public long xfers;
        public long rblks;
        public long wblks;
        public long bsize;
        public long qdepth;
        public long time;
    }

    protected AixHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    @Override
    public synchronized boolean updateAttributes() {
        DiskStats stats = queryStats();
        if (stats == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long blks = stats.rblks + stats.wblks;
        if (blks == 0L) {
            // No block-split info: attribute all transfers to reads, but stay monotonic with the
            // non-zero branch below — never decrease a previously-reported counter.
            if (stats.xfers > getReads()) {
                setReads(stats.xfers);
            }
        } else {
            long approximateReads = Math.round(stats.xfers * stats.rblks / (double) blks);
            long approximateWrites = stats.xfers - approximateReads;
            if (approximateReads > getReads()) {
                setReads(approximateReads);
            }
            if (approximateWrites > getWrites()) {
                setWrites(approximateWrites);
            }
        }
        setReadBytes(stats.rblks * stats.bsize);
        setWriteBytes(stats.wblks * stats.bsize);
        setCurrentQueueLength(stats.qdepth);
        setTransferTime(stats.time);
        setTimeStamp(now);
        return true;
    }

    /**
     * Looks up this disk's per-disk stats from the subclass's perfstat data source.
     *
     * @return stats POJO, or {@code null} if no entry was found for this disk name
     */
    protected abstract DiskStats queryStats();
}
