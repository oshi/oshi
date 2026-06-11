/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import java.lang.foreign.MemorySegment;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.solaris.LibKstatFunctions;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.hardware.HWDiskStore;
import oshi.hardware.common.platform.unix.solaris.SolarisHWDiskStore;

/**
 * FFM-backed Solaris HWDiskStore. Uses the legacy {@code kstat} chain only; Kstat2 exists only on the JDK 17-capped
 * latest Solaris, so FFM (JDK 25) never needs it.
 */
@ThreadSafe
public final class SolarisHWDiskStoreFFM extends SolarisHWDiskStore {

    private SolarisHWDiskStoreFFM(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Gets the disks on this machine.
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        return getDisks(SolarisHWDiskStoreFFM::new);
    }

    @Override
    protected DiskStats queryStats() {
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup(null, 0, getName());
            if (ksp.address() != 0L && kc.read(ksp)) {
                MemorySegment dataPtr = LibKstatFunctions.kstatData(ksp);
                if (dataPtr.address() == 0L) {
                    return null;
                }
                MemorySegment io = dataPtr.reinterpret(LibKstatFunctions.KSTAT_IO_LAYOUT.byteSize());
                DiskStats stats = new DiskStats();
                stats.reads = LibKstatFunctions.kstatIoReads(io);
                stats.writes = LibKstatFunctions.kstatIoWrites(io);
                stats.readBytes = LibKstatFunctions.kstatIoNread(io);
                stats.writeBytes = LibKstatFunctions.kstatIoNwritten(io);
                stats.currentQueueLength = (long) LibKstatFunctions.kstatIoWcnt(io) + LibKstatFunctions.kstatIoRcnt(io);
                stats.transferTime = LibKstatFunctions.kstatIoRtime(io) / 1_000_000L;
                stats.timeStamp = LibKstatFunctions.kstatSnaptime(ksp) / 1_000_000L;
                return stats;
            }
        }
        return null;
    }
}
