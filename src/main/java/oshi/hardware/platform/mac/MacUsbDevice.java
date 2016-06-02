/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ExecutingCommand;

public class MacUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 1L;

    private static final Pattern XML_STRING = Pattern.compile("<string>(.*?)</string>");

    public MacUsbDevice(String name, String vendor, String serialNumber, UsbDevice[] connectedDevices) {
        super(name, vendor, serialNumber, connectedDevices);
    }

    /**
     * {@inheritDoc}
     */
    public static UsbDevice[] getUsbDevices() {
        // Get heirarchical list of USB devices
        List<String> xml = ExecutingCommand.runNative("system_profiler SPUSBDataType -xml");
        // Look for <key>_items</key> which prcedes <array> ... </array>
        // Each pair of <dict> ... </dict> following is a USB device/hub
        List<String> items = new ArrayList<>();
        boolean copy = false;
        int indent = 0;
        for (String s : xml) {
            s = s.trim();
            // Read until <key>_items</key>
            if (!copy && s.equals("<key>_items</key>")) {
                copy = true;
                continue;
            }
            // If we've fond items indent with each <array> tag and copy over
            // everything with indent > 0.
            if (copy) {
                if (s.equals("</array>")) {
                    if (--indent == 0) {
                        copy = false;
                        continue;
                    }
                }
                if (indent > 0) {
                    items.add(s);
                }
                if (s.equals("<array>")) {
                    indent++;
                }
            }
        }
        // Items now contains 0 or more sets of <dict>...</dict>
        return getUsbDevices(items);
    }

    /**
     * Parses a list of xml into USB devices
     * 
     * @param items
     *            A list of XML beginning containing 0 or more <dict>...</dict>
     *            entries corresponding to USB buses, hubs, or devices
     * @return An array of usb devices corresponding to the <dict> entries
     */
    private static UsbDevice[] getUsbDevices(List<String> items) {
        List<UsbDevice> usbDevices = new ArrayList<UsbDevice>();
        List<String> item = new ArrayList<>();
        // Separate out item between each pair of <dict>...</dict> tags
        int indent = 0;
        for (String s : items) {
            if (s.equals("</dict>")) {
                // If this is the final closing tag, add the singular device
                // we've been accumulating in the item list
                if (--indent == 0) {
                    usbDevices.add(getUsbDevice(item));
                    item.clear();
                    continue;
                }
            }
            if (indent > 0) {
                item.add(s);
            }
            if (s.equals("<dict>")) {
                indent++;
            }
        }
        return usbDevices.toArray(new UsbDevice[usbDevices.size()]);
    }

    /**
     * Parses a list of xml (selected from inside <dict>...</dict> tags) into a
     * USB device
     * 
     * @param data
     *            A list of XML beginning containing an XML entry corresponding
     *            to a USB bus, hub, or device
     * @return A usb device corresponding to the entry
     */
    private static UsbDevice getUsbDevice(List<String> data) {
        String name = "";
        boolean readName = false;
        String vendor = "";
        boolean readVendor = false;
        String serialNumber = "";
        boolean readSerialNumber = false;
        List<String> nested = new ArrayList<>();
        boolean readNested = false;
        int indent = 0;
        for (String s : data) {
            if (readName) {
                name = parseXmlString(s);
                readName = false;
                continue;
            } else if (readVendor) {
                vendor = parseXmlString(s);
                readVendor = false;
                continue;
            } else if (readSerialNumber) {
                serialNumber = parseXmlString(s);
                readSerialNumber = false;
                continue;
            }
            if (s.equals("</array>") || s.equals("</dict>")) {
                if (--indent == 0) {
                    readNested = false;
                    continue;
                }
            }
            if (readNested && indent > 0) {
                nested.add(s);
            }
            if (s.equals("<array>") || s.equals("<dict>")) {
                indent++;
            }
            if (indent == 0) {
                switch (s) {
                case "<key>_name</key>":
                    readName = true;
                    break;
                case "<key>manufacturer</key>":
                    readVendor = true;
                    break;
                case "<key>serial_num</key>":
                    readSerialNumber = true;
                    break;
                case "<key>_items</key>":
                    readNested = true;
                    break;
                default:
                }
            }
        }
        return new MacUsbDevice(name, vendor, serialNumber, getUsbDevices(nested));
    }

    /**
     * Get the string between tags
     * 
     * @param s
     *            A string between <string> ... </string> tags
     * @return The string
     */
    private static String parseXmlString(String s) {
        Matcher matcher = XML_STRING.matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
