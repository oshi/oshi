/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;

class AbstractHardwareAbstractionLayerTest {

    private static AbstractHardwareAbstractionLayer createHal(List<UsbDevice> usbTree) {
        return new AbstractHardwareAbstractionLayer() {
            @Override
            protected ComputerSystem createComputerSystem() {
                return null;
            }

            @Override
            protected CentralProcessor createProcessor() {
                return null;
            }

            @Override
            protected GlobalMemory createMemory() {
                return null;
            }

            @Override
            protected Sensors createSensors() {
                return null;
            }

            @Override
            protected List<Display> createDisplays() {
                return Collections.emptyList();
            }

            @Override
            protected List<SoundCard> createSoundCards() {
                return Collections.emptyList();
            }

            @Override
            protected List<GraphicsCard> createGraphicsCards() {
                return Collections.emptyList();
            }

            @Override
            protected List<UsbDevice> createUsbDevices() {
                return usbTree;
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
            public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
                return Collections.emptyList();
            }
        };
    }

    private static UsbDevice createUsb(String name, List<UsbDevice> children) {
        return new AbstractUsbDevice(name, "", "", "", "", "", children) {
        };
    }

    @Test
    void testGetUsbDevicesTreeReturnsCachedTree() {
        UsbDevice child = createUsb("child", Collections.emptyList());
        UsbDevice root = createUsb("root", Collections.singletonList(child));
        AbstractHardwareAbstractionLayer hal = createHal(Collections.singletonList(root));

        List<UsbDevice> tree = hal.getUsbDevices(true);
        assertThat(tree, hasSize(1));
        assertThat(tree.get(0).getName(), is("root"));
        assertThat(tree.get(0).getConnectedDevices(), hasSize(1));
    }

    @Test
    void testGetUsbDevicesFlatFlattensTree() {
        UsbDevice grandchild = createUsb("grandchild", Collections.emptyList());
        UsbDevice child = createUsb("child", Collections.singletonList(grandchild));
        UsbDevice root = createUsb("root", Collections.singletonList(child));
        AbstractHardwareAbstractionLayer hal = createHal(Collections.singletonList(root));

        List<UsbDevice> flat = hal.getUsbDevices(false);
        assertThat(flat, hasSize(2));
        assertThat(flat.get(0).getName(), is("child"));
        assertThat(flat.get(1).getName(), is("grandchild"));
    }

    @Test
    void testGetUsbDevicesFlatEmptyTree() {
        AbstractHardwareAbstractionLayer hal = createHal(Collections.emptyList());

        List<UsbDevice> flat = hal.getUsbDevices(false);
        assertThat(flat, is(empty()));
    }
}
