/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Memory obtained by host_statistics (vm_stat) and sysctl.
 */
@ThreadSafe
public abstract class MacGlobalMemory extends AbstractGlobalMemory {

    private final Supplier<Long> available = memoize(this::queryVmStats, defaultExpiration());

    private final Supplier<Long> total = memoize(this::queryPhysMem);

    private final Supplier<Long> pageSize = memoize(this::queryPageSize);

    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    @Override
    public long getAvailable() {
        return available.get();
    }

    @Override
    public long getTotal() {
        return total.get();
    }

    @Override
    public long getPageSize() {
        return pageSize.get();
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
    }

    @Override
    public List<PhysicalMemory> getPhysicalMemory() {
        List<PhysicalMemory> pmList = new ArrayList<>();
        List<String> sp = ExecutingCommand.runNative("system_profiler SPMemoryDataType");
        int bank = 0;
        String bankLabel = Constants.UNKNOWN;
        long capacity = 0L;
        long speed = 0L;
        String manufacturer = Constants.UNKNOWN;
        String memoryType = Constants.UNKNOWN;
        String partNumber = Constants.UNKNOWN;
        String serialNumber = Constants.UNKNOWN;
        for (String line : sp) {
            if (line.trim().startsWith("BANK")) {
                // Save previous bank
                if (bank++ > 0) {
                    pmList.add(new PhysicalMemory(bankLabel, capacity, speed, manufacturer, memoryType,
                            Constants.UNKNOWN, serialNumber));
                }
                bankLabel = line.trim();
                int colon = bankLabel.lastIndexOf(':');
                if (colon > 0) {
                    bankLabel = bankLabel.substring(0, colon - 1);
                }
            } else if (bank > 0) {
                String[] split = line.trim().split(":");
                if (split.length == 2) {
                    switch (split[0]) {
                        case "Size":
                            capacity = ParseUtil.parseDecimalMemorySizeToBinary(split[1].trim());
                            break;
                        case "Type":
                            memoryType = split[1].trim();
                            break;
                        case "Speed":
                            speed = ParseUtil.parseHertz(split[1]);
                            break;
                        case "Manufacturer":
                            manufacturer = split[1].trim();
                            break;
                        case "Part Number":
                            partNumber = split[1].trim();
                            break;
                        case "Serial Number":
                            serialNumber = split[1].trim();
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        pmList.add(new PhysicalMemory(bankLabel, capacity, speed, manufacturer, memoryType, partNumber, serialNumber));

        return pmList;
    }

    protected abstract long queryVmStats();

    protected abstract long sysctl(String name, long defaultValue);

    private long queryPhysMem() {
        return sysctl("hw.memsize", 0L);
    }

    /**
     * Queries the host page size. Implementations should return the page size on success, or {@code 4096L} as a
     * fallback on failure.
     *
     * @return the page size in bytes
     */
    protected abstract long queryPageSize();

    protected abstract VirtualMemory createVirtualMemory();
}
