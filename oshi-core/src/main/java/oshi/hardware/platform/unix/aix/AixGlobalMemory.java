/*
 * Copyright 2020-2024 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.sun.jna.platform.unix.aix.Perfstat.perfstat_memory_total_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatMemory;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * Memory obtained by perfstat_memory_total_t
 */
@ThreadSafe
final class AixGlobalMemory extends AbstractGlobalMemory {

    private final Supplier<perfstat_memory_total_t> perfstatMem = memoize(AixGlobalMemory::queryPerfstat,
            defaultExpiration());
    private final Supplier<List<String>> lscfg;

    // AIX has multiple page size units, but for purposes of "pages" in perfstat,
    // the docs specify 4KB pages so we hardcode this
    private static final long PAGESIZE = 4096L;

    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    AixGlobalMemory(Supplier<List<String>> lscfg) {
        this.lscfg = lscfg;
    }

    @Override
    public long getAvailable() {
        return perfstatMem.get().real_avail * PAGESIZE;
    }

    @Override
    public long getTotal() {
        return perfstatMem.get().real_total * PAGESIZE;
    }

    @Override
    public long getPageSize() {
        return PAGESIZE;
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
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
                    // Save previous bank
                    if (capacity > 0) {
                        pmList.add(new PhysicalMemory(bankLabel + locator, capacity, 0L, "IBM", Constants.UNKNOWN,
                                Constants.UNKNOWN));
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
                    // Save previous bank
                    if (capacity > 0) {
                        pmList.add(new PhysicalMemory(locator, capacity, 0L, "IBM", Constants.UNKNOWN, partNumber));
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

    private static perfstat_memory_total_t queryPerfstat() {
        return PerfstatMemory.queryMemoryTotal();
    }

    private VirtualMemory createVirtualMemory() {
        return new AixVirtualMemory(perfstatMem);
    }
}
