/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.UsbDevice;

class AbstractUsbDeviceTest {

    /** Builds a plain AbstractUsbDevice so buildDeviceTree can be exercised without a platform backend. */
    private static final AbstractUsbDevice.UsbDeviceFactory FACTORY = (name, vendor, vendorId, productId, serialNumber,
            uniqueDeviceId, connectedDevices) -> new AbstractUsbDevice(name, vendor, vendorId, productId, serialNumber,
                    uniqueDeviceId, connectedDevices) {
            };

    @Test
    void testGetters() {
        AbstractUsbDevice device = new AbstractUsbDevice("Mouse", "Logitech", "046d", "c077", "SN1", "USB1",
                Collections.emptyList()) {
        };
        assertThat(device.getName(), is("Mouse"));
        assertThat(device.getVendor(), is("Logitech"));
        assertThat(device.getVendorId(), is("046d"));
        assertThat(device.getProductId(), is("c077"));
        assertThat(device.getSerialNumber(), is("SN1"));
        assertThat(device.getUniqueDeviceId(), is("USB1"));
        assertThat(device.getConnectedDevices(), is(Collections.emptyList()));
    }

    @Test
    void testConnectedDevices() {
        AbstractUsbDevice child = new AbstractUsbDevice("Mouse", "Logitech", "", "", "", "", Collections.emptyList()) {
        };
        AbstractUsbDevice parent = new AbstractUsbDevice("Hub", "Generic", "", "", "", "",
                Collections.singletonList(child)) {
        };
        assertThat(parent.getConnectedDevices(), hasSize(1));
        assertThat(parent.getConnectedDevices().get(0).getName(), is("Mouse"));
    }

    @Test
    void testCompareTo() {
        AbstractUsbDevice a = new AbstractUsbDevice("Alpha", "", "", "", "", "", Collections.emptyList()) {
        };
        AbstractUsbDevice b = new AbstractUsbDevice("Beta", "", "", "", "", "", Collections.emptyList()) {
        };
        assertThat(a.compareTo(b), is(lessThan(0)));
    }

    @Test
    void testToStringWithChildren() {
        AbstractUsbDevice child = new AbstractUsbDevice("Mouse", "Logitech", "", "", "SN1", "",
                Collections.emptyList()) {
        };
        AbstractUsbDevice parent = new AbstractUsbDevice("Hub", "", "", "", "", "", Collections.singletonList(child)) {
        };
        assertThat(parent.toString(), is(" Hub\n |-- Mouse (Logitech) [s/n: SN1]"));
    }

    // -------------------------------------------------------------------------
    // buildDeviceTree — leaf device
    // -------------------------------------------------------------------------

    @Test
    void testBuildDeviceTreeLeaf() {
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("usb1/1-1", "USB Mouse");
        Map<String, String> vendorMap = new HashMap<>();
        vendorMap.put("usb1/1-1", "Logitech");
        Map<String, String> vendorIdMap = new HashMap<>();
        vendorIdMap.put("usb1/1-1", "046d");
        Map<String, String> productIdMap = new HashMap<>();
        productIdMap.put("usb1/1-1", "c077");
        Map<String, String> serialMap = new HashMap<>();
        serialMap.put("usb1/1-1", "SN123");
        Map<String, List<String>> hubMap = Collections.emptyMap();

        UsbDevice device = AbstractUsbDevice.buildDeviceTree("usb1/1-1", "0000", "0000", nameMap, vendorMap,
                vendorIdMap, productIdMap, serialMap, hubMap, FACTORY);

        assertThat(device.getName(), is("USB Mouse"));
        assertThat(device.getVendor(), is("Logitech"));
        assertThat(device.getVendorId(), is("046d"));
        assertThat(device.getProductId(), is("c077"));
        assertThat(device.getSerialNumber(), is("SN123"));
        assertThat(device.getUniqueDeviceId(), is("usb1/1-1"));
        assertThat(device.getConnectedDevices(), is(empty()));
    }

    // -------------------------------------------------------------------------
    // buildDeviceTree — fallback to parent vid/pid when this device reports none
    // -------------------------------------------------------------------------

    @Test
    void testBuildDeviceTreeFallbackToParentVidPid() {
        UsbDevice device = AbstractUsbDevice.buildDeviceTree("usb1/1-2", "abcd", "1234", Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), FACTORY);

        assertThat(device.getVendorId(), is("abcd"));
        assertThat(device.getProductId(), is("1234"));
        // Name falls back to "vid:pid"
        assertThat(device.getName(), is("abcd:1234"));
        assertThat(device.getVendor(), is(""));
        assertThat(device.getSerialNumber(), is(""));
    }

    // -------------------------------------------------------------------------
    // buildDeviceTree — hub with children, sorted by name
    // -------------------------------------------------------------------------

    @Test
    void testBuildDeviceTreeHubWithSortedChildren() {
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("usb1", "Root Hub");
        nameMap.put("usb1/1-1", "Zebra Device");
        nameMap.put("usb1/1-2", "Alpha Device");
        Map<String, String> vendorMap = Collections.emptyMap();
        Map<String, String> vendorIdMap = new HashMap<>();
        vendorIdMap.put("usb1", "1d6b");
        vendorIdMap.put("usb1/1-1", "aaaa");
        vendorIdMap.put("usb1/1-2", "bbbb");
        Map<String, String> productIdMap = new HashMap<>();
        productIdMap.put("usb1", "0002");
        productIdMap.put("usb1/1-1", "0001");
        productIdMap.put("usb1/1-2", "0002");
        Map<String, String> serialMap = Collections.emptyMap();
        Map<String, List<String>> hubMap = new HashMap<>();
        hubMap.put("usb1", Arrays.asList("usb1/1-1", "usb1/1-2"));

        UsbDevice hub = AbstractUsbDevice.buildDeviceTree("usb1", "0000", "0000", nameMap, vendorMap, vendorIdMap,
                productIdMap, serialMap, hubMap, FACTORY);

        assertThat(hub.getName(), is("Root Hub"));
        List<UsbDevice> children = hub.getConnectedDevices();
        assertThat(children, hasSize(2));
        // Children sorted by name: "Alpha Device" before "Zebra Device"
        assertThat(children.get(0).getName(), is("Alpha Device"));
        assertThat(children.get(1).getName(), is("Zebra Device"));
    }

    // -------------------------------------------------------------------------
    // buildDeviceTree — nested hierarchy
    // -------------------------------------------------------------------------

    @Test
    void testBuildDeviceTreeNestedHierarchy() {
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("usb1", "Root Hub");
        nameMap.put("usb1/1-1", "Hub");
        nameMap.put("usb1/1-1/1-1.1", "Keyboard");
        Map<String, String> vendorIdMap = new HashMap<>();
        vendorIdMap.put("usb1", "1d6b");
        vendorIdMap.put("usb1/1-1", "0424");
        vendorIdMap.put("usb1/1-1/1-1.1", "04f2");
        Map<String, String> productIdMap = new HashMap<>();
        productIdMap.put("usb1", "0002");
        productIdMap.put("usb1/1-1", "2514");
        productIdMap.put("usb1/1-1/1-1.1", "0112");
        Map<String, List<String>> hubMap = new HashMap<>();
        hubMap.put("usb1", Collections.singletonList("usb1/1-1"));
        hubMap.put("usb1/1-1", Collections.singletonList("usb1/1-1/1-1.1"));

        UsbDevice root = AbstractUsbDevice.buildDeviceTree("usb1", "0000", "0000", nameMap, Collections.emptyMap(),
                vendorIdMap, productIdMap, Collections.emptyMap(), hubMap, FACTORY);

        assertThat(root.getConnectedDevices(), hasSize(1));
        UsbDevice hub = root.getConnectedDevices().get(0);
        assertThat(hub.getName(), is("Hub"));
        assertThat(hub.getConnectedDevices(), hasSize(1));
        assertThat(hub.getConnectedDevices().get(0).getName(), is("Keyboard"));
    }
}
