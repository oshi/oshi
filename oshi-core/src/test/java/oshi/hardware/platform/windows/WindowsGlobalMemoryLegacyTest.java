/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.PhysicalMemory;

/**
 * Exercises the pre-Windows-10 physical-memory fallback path against the real system by forcing the
 * {@code isWindows10OrGreater()} version gate. Only the version flag is overridden, so the genuine WMI query runs; a
 * rotted legacy path surfaces as a divergence from the modern path rather than as a mere coverage number.
 */
@EnabledOnOs(OS.WINDOWS)
class WindowsGlobalMemoryLegacyTest {

    // Stable ordering on fields that must agree between the two schemas, so the comparison can't flake on WMI row order
    private static final Comparator<PhysicalMemory> BY_BANK = Comparator.comparing(PhysicalMemory::getBankLabel)
            .thenComparing(PhysicalMemory::getSerialNumber).thenComparingLong(PhysicalMemory::getCapacity);

    /** A real {@link WindowsGlobalMemoryJNA} with only the Windows-version gate forced. */
    private static final class ForcedVersion extends WindowsGlobalMemoryJNA {
        private final boolean windows10OrGreater;

        ForcedVersion(boolean windows10OrGreater) {
            this.windows10OrGreater = windows10OrGreater;
        }

        @Override
        protected boolean isWindows10OrGreater() {
            return windows10OrGreater;
        }
    }

    /**
     * The legacy (pre-Win10) and modern physical-memory queries hit the same {@code Win32_PhysicalMemory} class and
     * differ only in how they resolve the memory-type string, so every other field must agree bank-for-bank. A broken
     * legacy query (wrong field, wrong parse) shows up here as a mismatch. When the runner reports no banks both lists
     * are empty and the legacy branch is still exercised.
     */
    @Test
    void legacyPhysicalMemoryMatchesModernExceptType() {
        List<PhysicalMemory> legacy = new ArrayList<>(new ForcedVersion(false).getPhysicalMemory());
        List<PhysicalMemory> modern = new ArrayList<>(new ForcedVersion(true).getPhysicalMemory());
        legacy.sort(BY_BANK);
        modern.sort(BY_BANK);

        assertThat("legacy and modern must report the same bank count", legacy, hasSize(modern.size()));
        for (int i = 0; i < modern.size(); i++) {
            PhysicalMemory l = legacy.get(i);
            PhysicalMemory m = modern.get(i);
            assertThat(l.getBankLabel(), is(m.getBankLabel()));
            assertThat(l.getCapacity(), is(m.getCapacity()));
            assertThat(l.getClockSpeed(), is(m.getClockSpeed()));
            assertThat(l.getManufacturer(), is(m.getManufacturer()));
            assertThat(l.getPartNumber(), is(m.getPartNumber()));
            assertThat(l.getSerialNumber(), is(m.getSerialNumber()));
            // memoryType is intentionally not compared: legacy resolves Win32 MemoryType, modern SMBIOSMemoryType
        }
    }
}
