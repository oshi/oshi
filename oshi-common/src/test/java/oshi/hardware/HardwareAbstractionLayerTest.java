/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests default methods on {@link HardwareAbstractionLayer} using anonymous implementations.
 */
class HardwareAbstractionLayerTest {

    // Minimal stub implementing only the abstract methods
    private static final HardwareAbstractionLayer MINIMAL_HAL = new HardwareAbstractionLayer() {
        @Override
        public ComputerSystem getComputerSystem() {
            return null;
        }

        @Override
        public CentralProcessor getProcessor() {
            return null;
        }

        @Override
        public GlobalMemory getMemory() {
            return null;
        }

        @Override
        public List<PowerSource> getPowerSources() {
            return Collections.emptyList();
        }

        @Override
        public List<HWDiskStore> getDiskStores() {
            return Collections.emptyList();
        }

        @Override
        public List<NetworkIF> getNetworkIFs() {
            return Collections.emptyList();
        }

        @Override
        public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
            return Collections.emptyList();
        }

        @Override
        public List<Display> getDisplays() {
            return Collections.emptyList();
        }

        @Override
        public Sensors getSensors() {
            return null;
        }

        @Override
        public List<UsbDevice> getUsbDevices(boolean tree) {
            return Collections.emptyList();
        }

        @Override
        public List<SoundCard> getSoundCards() {
            return Collections.emptyList();
        }

        @Override
        public List<GraphicsCard> getGraphicsCards() {
            return Collections.emptyList();
        }
    };

    // Stub that overrides both default methods
    private static final HardwareAbstractionLayer OVERRIDING_HAL = new HardwareAbstractionLayer() {
        @Override
        public ComputerSystem getComputerSystem() {
            return null;
        }

        @Override
        public CentralProcessor getProcessor() {
            return null;
        }

        @Override
        public GlobalMemory getMemory() {
            return null;
        }

        @Override
        public List<PowerSource> getPowerSources() {
            return Collections.emptyList();
        }

        @Override
        public List<HWDiskStore> getDiskStores() {
            return Collections.emptyList();
        }

        @Override
        public List<NetworkIF> getNetworkIFs() {
            return Collections.emptyList();
        }

        @Override
        public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
            return Collections.emptyList();
        }

        @Override
        public List<Display> getDisplays() {
            return Collections.emptyList();
        }

        @Override
        public Sensors getSensors() {
            return null;
        }

        @Override
        public List<UsbDevice> getUsbDevices(boolean tree) {
            return Collections.emptyList();
        }

        @Override
        public List<SoundCard> getSoundCards() {
            return Collections.emptyList();
        }

        @Override
        public List<GraphicsCard> getGraphicsCards() {
            return Collections.emptyList();
        }

        @Override
        public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
            return Collections.singletonList(null);
        }

        @Override
        public List<Printer> getPrinters() {
            return Collections.singletonList(null);
        }
    };

    @Test
    void testDefaultGetLogicalVolumeGroupsReturnsEmptyList() {
        List<LogicalVolumeGroup> lvgs = MINIMAL_HAL.getLogicalVolumeGroups();
        assertThat("Default getLogicalVolumeGroups() should not be null", lvgs, is(notNullValue()));
        assertThat("Default getLogicalVolumeGroups() should return empty list", lvgs, is(empty()));
    }

    @Test
    void testDefaultGetPrintersReturnsEmptyList() {
        List<Printer> printers = MINIMAL_HAL.getPrinters();
        assertThat("Default getPrinters() should not be null", printers, is(notNullValue()));
        assertThat("Default getPrinters() should return empty list", printers, is(empty()));
    }

    @Test
    void testOverriddenGetLogicalVolumeGroups() {
        List<LogicalVolumeGroup> lvgs = OVERRIDING_HAL.getLogicalVolumeGroups();
        assertThat("Overridden getLogicalVolumeGroups() should not be null", lvgs, is(notNullValue()));
        assertThat("Overridden getLogicalVolumeGroups() should return one entry", lvgs, hasSize(1));
    }

    @Test
    void testOverriddenGetPrinters() {
        List<Printer> printers = OVERRIDING_HAL.getPrinters();
        assertThat("Overridden getPrinters() should not be null", printers, is(notNullValue()));
        assertThat("Overridden getPrinters() should return one entry", printers, hasSize(1));
    }
}
