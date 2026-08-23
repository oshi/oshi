/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
        AbstractUsbDevice parent = new AbstractUsbDevice("Hub", "Generic", "", "", "", "", List.of(child)) {
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
    void testCompareToIsConsistentWithEquals() {
        // Two distinct devices sharing a name: compareTo must not call them equal, or a sorted set would drop one
        AbstractUsbDevice first = new AbstractUsbDevice("Mouse", "Logitech", "046d", "c077", "SN1", "USB1",
                Collections.emptyList()) {
        };
        AbstractUsbDevice second = new AbstractUsbDevice("Mouse", "Logitech", "046d", "c077", "SN2", "USB2",
                Collections.emptyList()) {
        };
        assertThat(first.compareTo(second), is(not(0)));
        assertThat(first.equals(second), is(false));
        assertThat(new TreeSet<>(List.of(first, second)), hasSize(2));

        // And a device equal on every ordered field compares equal and hashes alike
        AbstractUsbDevice copy = new AbstractUsbDevice("Mouse", "Logitech", "046d", "c077", "SN1", "USB1",
                Collections.emptyList()) {
        };
        assertThat(first.compareTo(copy), is(0));
        assertThat(first, is(copy));
        assertThat(first.hashCode(), is(copy.hashCode()));
    }

    @Test
    void testEqualsIsSymmetricAgainstAForeignImplementation() {
        AbstractUsbDevice device = new AbstractUsbDevice("Mouse", "Logitech", "046d", "c077", "SN1", "USB1",
                Collections.emptyList()) {
        };
        // A UsbDevice that is not an AbstractUsbDevice carries the same values but keeps Object.equals. Equality must
        // fail in both directions, or a HashSet holding the two would behave differently depending on insertion order.
        UsbDevice foreign = new ForeignUsbDevice();
        assertThat(device.equals(foreign), is(false));
        assertThat(foreign.equals(device), is(false));
        // Ordering, unlike equality, is shared: the foreign device inherits UsbDevice's default compareTo
        assertThat(device.compareTo(foreign), is(0));
        assertThat(foreign.compareTo(device), is(0));
    }

    @Test
    void testOrderingIsSignSymmetricAcrossImplementations() {
        // Same name, different identity fields: the tie-breakers must agree on a direction from both sides
        AbstractUsbDevice ours = new AbstractUsbDevice("Mouse", "Logitech", "046d", "c077", "SN9", "USB9",
                Collections.emptyList()) {
        };
        UsbDevice foreign = new ForeignUsbDevice(); // name "Mouse", uniqueDeviceId "USB1"
        assertThat(Integer.signum(ours.compareTo(foreign)), is(-Integer.signum(foreign.compareTo(ours))));
        assertThat(ours.compareTo(foreign), is(greaterThan(0)));

        // And a SortedSet keeps the same elements whichever order they go in
        assertThat(new TreeSet<>(List.of(ours, foreign)), hasSize(2));
        assertThat(new TreeSet<>(List.of(foreign, ours)), hasSize(2));
    }

    @Test
    void testToStringWithChildren() {
        AbstractUsbDevice child = new AbstractUsbDevice("Mouse", "Logitech", "", "", "SN1", "",
                Collections.emptyList()) {
        };
        AbstractUsbDevice parent = new AbstractUsbDevice("Hub", "", "", "", "", "", List.of(child)) {
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
        hubMap.put("usb1", List.of("usb1/1-1", "usb1/1-2"));

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
        hubMap.put("usb1", List.of("usb1/1-1"));
        hubMap.put("usb1/1-1", List.of("usb1/1-1/1-1.1"));

        UsbDevice root = AbstractUsbDevice.buildDeviceTree("usb1", "0000", "0000", nameMap, Collections.emptyMap(),
                vendorIdMap, productIdMap, Collections.emptyMap(), hubMap, FACTORY);

        assertThat(root.getConnectedDevices(), hasSize(1));
        UsbDevice hub = root.getConnectedDevices().get(0);
        assertThat(hub.getName(), is("Hub"));
        assertThat(hub.getConnectedDevices(), hasSize(1));
        assertThat(hub.getConnectedDevices().get(0).getName(), is("Keyboard"));
    }

    /** A UsbDevice that does not extend AbstractUsbDevice, so it keeps Object's identity-based equals. */
    private static final class ForeignUsbDevice implements UsbDevice {
        @Override
        public String getName() {
            return "Mouse";
        }

        @Override
        public String getVendor() {
            return "Logitech";
        }

        @Override
        public String getVendorId() {
            return "046d";
        }

        @Override
        public String getProductId() {
            return "c077";
        }

        @Override
        public String getSerialNumber() {
            return "SN1";
        }

        @Override
        public String getUniqueDeviceId() {
            return "USB1";
        }

        @Override
        public List<UsbDevice> getConnectedDevices() {
            return Collections.emptyList();
        }

        // No compareTo override: the ordering comes from UsbDevice's default method, which is the point
    }
}
