/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * FreeBsd Usb Device
 */
@Immutable
public class FreeBsdUsbDevice extends AbstractUsbDevice {

    // A devinfo -v line: its indent, driver instance, optional description, and then its plug-and-play and location
    // fields, after "pnpinfo" and "at" respectively
    private static final Pattern DEVINFO_LINE = Pattern.compile("( *)(\\S+)(?: <(.*?)>)?(.*)");
    // A plug-and-play or location field, its value quoted where it may hold spaces: sernum="1234BBBK5678" ugen=ugen0.2
    // The lookbehind stops find() from retrying inside a name, so the scan stays linear
    private static final String PNP_NAME = "(?<!\\w)(\\w++)=";
    private static final String PNP_VALUE = "(\"[^\"]*+\"|\\S++)";
    private static final Pattern PNP_FIELD = Pattern.compile(PNP_NAME + PNP_VALUE); // NOSONAR java:S8786 - linear

    /**
     * Constructs a new {@code FreeBsdUsbDevice}.
     *
     * @param name             the device name
     * @param vendor           the vendor
     * @param vendorId         the vendor ID
     * @param productId        the product ID
     * @param serialNumber     the serial number
     * @param uniqueDeviceId   the unique device ID
     * @param connectedDevices the devices connected to this one
     */
    public FreeBsdUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            @Nullable String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * Instantiates the USB controller device tree. The flat form is derived by the caller (the HAL).
     *
     * @return a list of USB controllers, each with its connected-device tree.
     */
    public static List<UsbDevice> getUsbDevices() {
        return parseUsbDevices(ExecutingCommand.runNative("devinfo -v"));
    }

    /**
     * Parses {@code devinfo -v} output into the USB controller device tree. Package-private for testing.
     * <p>
     * {@code devinfo} prints the device tree by indentation, each line a driver instance with its description in angle
     * brackets and its plug-and-play fields after {@code pnpinfo}. A controller is the parent of a {@code usbus} node,
     * whose children, the root hub and what hangs from it, become the controller's own. A USB device is listed once per
     * interface driver attached to it, each with the same {@code ugen} name among its location fields, so that name
     * identifies the device. A driver attached beneath a USB device without a {@code ugen} name of its own, such as a
     * HID bus, is not a USB device and is left out. Only devices with a driver attached appear. DragonFly, which shares
     * this class, prints no descriptions, so there a device is named by its driver instance.
     *
     * @param devinfo the lines emitted by {@code devinfo -v}
     * @return a list of USB controllers, each with its connected-device tree.
     */
    static List<UsbDevice> parseUsbDevices(List<String> devinfo) {
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> vendorIdMap = new HashMap<>();
        Map<String, String> productIdMap = new HashMap<>();
        Map<String, String> serialMap = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        Map<String, List<String>> hubMap = new HashMap<>();
        List<String> usBuses = new ArrayList<>();
        // The open ancestors of the current line, innermost first, as indent and key
        Deque<Pair<Integer, String>> ancestors = new ArrayDeque<>();
        // The root hubs and USB devices seen, and the drivers beneath them that are neither, such as a HID bus
        Set<String> usbNodes = new HashSet<>();
        Set<String> notUsb = new HashSet<>();
        for (String line : devinfo) {
            Matcher m = DEVINFO_LINE.matcher(line);
            if (!m.matches()) {
                continue;
            }
            int indent = m.group(1).length();
            Pair<Integer, String> ancestor = ancestors.peek();
            while (ancestor != null && ancestor.getA() >= indent) {
                ancestors.pop();
                ancestor = ancestors.peek();
            }
            String parent = ancestor == null ? null : ancestor.getB();
            // The ugen name is among the location fields, the IDs among the plug-and-play ones
            Map<String, String> pnp = parseFields(m.group(4));
            String ugen = pnp.get("ugen");
            String key = ugen == null ? m.group(2) : ugen;
            if (ugen == null && nameMap.containsKey(key)) {
                // A name devinfo repeats, such as "unknown" for each device without a driver
                key = key + "#" + nameMap.size();
            }
            ancestors.push(new Pair<>(indent, key));
            if (parent != null && ugen == null && (usbNodes.contains(parent) || notUsb.contains(parent))) {
                // A child driver of a USB device that is not itself one, such as the HID bus beneath a tablet
                notUsb.add(key);
                continue;
            }
            if (ugen != null || (parent != null && parent.startsWith("usbus"))) {
                usbNodes.add(key);
            }
            if (nameMap.containsKey(key)) {
                // Another interface driver of a device already listed
                continue;
            }
            if (key.startsWith("usbus")) {
                usBuses.add(key);
            }
            String description = ParseUtil.getStringValueOrEmpty(m.group(3));
            int usbClass = description.indexOf(", class ");
            if (description.isEmpty()) {
                // DragonFly's devinfo prints no descriptions, so the driver instance (uhci0, uhub0) names the device
                nameMap.put(key, m.group(2));
            } else {
                nameMap.put(key, usbClass < 0 ? description : description.substring(0, usbClass));
            }
            putHexId(vendorIdMap, key, pnp.get("vendor"));
            putHexId(productIdMap, key, pnp.get(pnp.containsKey("product") ? "product" : "device"));
            String serial = pnp.get("sernum");
            if (serial != null && !serial.isEmpty()) {
                serialMap.put(key, serial);
            }
            if (parent != null) {
                parentMap.put(key, parent);
                hubMap.computeIfAbsent(parent, x -> new ArrayList<>()).add(key);
            }
        }

        // Skip the usbuses: make their parents the controllers and replace parents' children with the buses' children
        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String usbus : usBuses) {
            String parent = parentMap.get(usbus);
            if (parent == null) {
                continue;
            }
            hubMap.put(parent, new ArrayList<>(hubMap.getOrDefault(usbus, Collections.emptyList())));
            controllerDevices.add(buildDeviceTree(parent, "0000", "0000", nameMap, Collections.emptyMap(), vendorIdMap,
                    productIdMap, serialMap, hubMap, FreeBsdUsbDevice::new));
        }
        return Collections.unmodifiableList(controllerDevices);
    }

    private static Map<String, String> parseFields(String fields) {
        Map<String, String> pnp = new HashMap<>();
        Matcher m = PNP_FIELD.matcher(fields);
        while (m.find()) {
            String value = m.group(2);
            if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                value = value.substring(1, value.length() - 1);
            }
            pnp.put(m.group(1), value);
        }
        return pnp;
    }

    private static void putHexId(Map<String, String> map, String key, @Nullable String hex) {
        if (hex != null && hex.startsWith("0x")) {
            map.put(key, String.format(Locale.ROOT, "%04x", ParseUtil.hexStringToInt(hex.substring(2), 0)));
        }
    }
}
