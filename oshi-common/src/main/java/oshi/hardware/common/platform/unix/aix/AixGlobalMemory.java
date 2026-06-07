/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * Abstract base for AIX GlobalMemory. The public {@link AbstractGlobalMemory} API translates
 * {@code perfstat_memory_total_t} fields into bytes; {@link #getPhysicalMemory()} parses the shared {@code lscfg}
 * supplier passed in from the HAL. Subclasses provide the perfstat field accessors and the matching
 * {@link AixVirtualMemory}.
 */
@ThreadSafe
public abstract class AixGlobalMemory extends AbstractGlobalMemory {

    // AIX uses 4 KB pages for "pages" reported by perfstat (per the libperfstat docs).
    protected static final long PAGESIZE = 4096L;

    /** Memoized hardware listing supplier from the owning HAL. */
    protected final Supplier<List<String>> lscfg;

    protected AixGlobalMemory(Supplier<List<String>> lscfg) {
        this.lscfg = lscfg;
    }

    @Override
    public long getAvailable() {
        return queryRealAvail() * PAGESIZE;
    }

    @Override
    public long getTotal() {
        return queryRealTotal() * PAGESIZE;
    }

    @Override
    public long getPageSize() {
        return PAGESIZE;
    }

    @Override
    public List<PhysicalMemory> getPhysicalMemory() {
        List<PhysicalMemory> pmList = new ArrayList<>();
        boolean isMemModule = false;
        boolean isMemoryDIMM = false;
        String bankLabel = Constants.UNKNOWN;
        String locator = "";
        String partNumber = Constants.UNKNOWN;
        long capacity = 0L;
        for (String line : lscfg.get()) {
            String s = line.trim();
            if (s.endsWith("memory-module")) {
                isMemModule = true;
            } else if (s.startsWith("Memory DIMM")) {
                isMemoryDIMM = true;
            } else if (isMemModule) {
                if (s.startsWith("Node:")) {
                    bankLabel = s.substring(5).trim();
                    if (bankLabel.startsWith("IBM,")) {
                        bankLabel = bankLabel.substring(4);
                    }
                } else if (s.startsWith("Physical Location:")) {
                    locator = "/" + s.substring(18).trim();
                } else if (s.startsWith("Size")) {
                    capacity = ParseUtil.parseLongOrDefault(ParseUtil.removeLeadingDots(s.substring(4).trim()),
                            0L) << 20;
                } else if (s.startsWith("Hardware Location Code")) {
                    if (capacity > 0) {
                        pmList.add(new PhysicalMemory(bankLabel + locator, capacity, 0L, "IBM", Constants.UNKNOWN,
                                Constants.UNKNOWN, Constants.UNKNOWN));
                    }
                    bankLabel = Constants.UNKNOWN;
                    locator = "";
                    capacity = 0L;
                    isMemModule = false;
                }
            } else if (isMemoryDIMM) {
                if (s.startsWith("Hardware Location Code")) {
                    locator = ParseUtil.removeLeadingDots(s.substring(23).trim());
                } else if (s.startsWith("Size")) {
                    capacity = ParseUtil.parseLongOrDefault(ParseUtil.removeLeadingDots(s.substring(4).trim()),
                            0L) << 20;
                } else if (s.startsWith("Part Number") || s.startsWith("FRU Number")) {
                    partNumber = ParseUtil.removeLeadingDots(s.substring(11).trim());
                } else if (s.startsWith("Physical Location:")) {
                    if (capacity > 0) {
                        pmList.add(new PhysicalMemory(locator, capacity, 0L, "IBM", Constants.UNKNOWN, partNumber,
                                Constants.UNKNOWN));
                    }
                    partNumber = Constants.UNKNOWN;
                    locator = "";
                    capacity = 0L;
                    isMemoryDIMM = false;
                }
            }
        }
        return pmList;
    }

    /**
     * Reads {@code perfstat_memory_total_t.real_avail} (4 KB pages) via the subclass.
     *
     * @return real_avail in pages
     */
    protected abstract long queryRealAvail();

    /**
     * Reads {@code perfstat_memory_total_t.real_total} (4 KB pages) via the subclass.
     *
     * @return real_total in pages
     */
    protected abstract long queryRealTotal();

    /**
     * Constructs the matching {@link AixVirtualMemory} (JNA or FFM) for this instance.
     *
     * @return the VirtualMemory paired with this GlobalMemory implementation
     */
    @Override
    public abstract VirtualMemory getVirtualMemory();
}
