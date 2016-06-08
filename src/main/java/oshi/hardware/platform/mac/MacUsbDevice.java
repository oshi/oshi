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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.jna.platform.mac.CoreFoundation.CFStringRef;
import oshi.jna.platform.mac.CoreFoundation.CFTypeRef;
import oshi.jna.platform.mac.IOKit;
import oshi.util.platform.mac.CfUtil;
import oshi.util.platform.mac.IOKitUtil;

public class MacUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 1L;

    public MacUsbDevice(String name, String vendor, String serialNumber, UsbDevice[] connectedDevices) {
        super(name, vendor, serialNumber, connectedDevices);
    }

    /*
     * Maps to store information using RegistryEntryID as the key
     */
    private static Map<Long, String> nameMap = new HashMap<>();
    private static Map<Long, String> vendorMap = new HashMap<>();
    private static Map<Long, String> serialMap = new HashMap<>();
    private static Map<Long, List<Long>> hubMap = new HashMap<>();

    /*
     * Strings for querying device information from registry
     */
    private static final CFStringRef cfVendor = CFStringRef.toCFString("USB Vendor Name");
    private static final CFStringRef cfSerial = CFStringRef.toCFString("USB Serial Number");

    /**
     * {@inheritDoc}
     */
    public static UsbDevice[] getUsbDevices() {
        // Reusable buffer for getting IO name strings
        Pointer buffer = new Memory(128); // io_name_t is char[128]
        // Build a list of devices with no parent; these will be the roots
        List<Long> usbControllers = new ArrayList<>();
        // Empty out maps
        nameMap.clear();
        vendorMap.clear();
        serialMap.clear();
        hubMap.clear();

        // Iterate over USB Controllers. All devices are children of one of
        // these controllers in the "IOService" plane
        IntByReference iter = new IntByReference();
        IOKitUtil.getMatchingServices("IOUSBController", iter);
        int device = IOKit.INSTANCE.IOIteratorNext(iter.getValue());
        while (device != 0) {
            // Unique global identifier for this device
            LongByReference id = new LongByReference();
            IOKit.INSTANCE.IORegistryEntryGetRegistryEntryID(device, id);
            usbControllers.add(id.getValue());

            // Get device name and store in map
            IOKit.INSTANCE.IORegistryEntryGetName(device, buffer);
            nameMap.put(id.getValue(), buffer.getString(0));
            // Controllers don't have vendor and serial so ignore at this level

            // Now iterate the children of this device in the "IOService" plane.
            // If devices have a parent, link to that parent, otherwise link to
            // the controller as parent
            IntByReference childIter = new IntByReference();
            IOKit.INSTANCE.IORegistryEntryGetChildIterator(device, "IOService", childIter);
            int childDevice = IOKit.INSTANCE.IOIteratorNext(childIter.getValue());
            while (childDevice != 0) {
                // Unique global identifier for this device
                LongByReference childId = new LongByReference();
                IOKit.INSTANCE.IORegistryEntryGetRegistryEntryID(childDevice, childId);

                // Get this device's parent in the "IOUSB" plane
                IntByReference parent = new IntByReference();
                IOKit.INSTANCE.IORegistryEntryGetParentEntry(childDevice, "IOUSB", parent);

                // If parent is named "Root" ignore that id and use the
                // controller's id
                LongByReference parentId = id;
                IOKit.INSTANCE.IORegistryEntryGetName(parent.getValue(), buffer);
                if (!buffer.getString(0).equals("Root")) {
                    // Unique global identifier for the parent
                    parentId = new LongByReference();
                    IOKit.INSTANCE.IORegistryEntryGetRegistryEntryID(parent.getValue(), parentId);
                }
                // Store parent in map
                if (!hubMap.containsKey(parentId.getValue())) {
                    hubMap.put(parentId.getValue(), new ArrayList<Long>());
                }
                hubMap.get(parentId.getValue()).add(childId.getValue());

                // Get device name and store in map
                IOKit.INSTANCE.IORegistryEntryGetName(childDevice, buffer);
                nameMap.put(childId.getValue(), buffer.getString(0));
                // Get vendor and store in map
                CFTypeRef vendorRef = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(childDevice, cfVendor,
                        CfUtil.ALLOCATOR, 0);
                if (vendorRef != null && vendorRef.getPointer() != null) {
                    vendorMap.put(childId.getValue(), CfUtil.cfPointerToString(vendorRef.getPointer()));
                }
                CfUtil.release(vendorRef);
                // Get serial and store in map
                CFTypeRef serialRef = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(childDevice, cfSerial,
                        CfUtil.ALLOCATOR, 0);
                if (serialRef != null && serialRef.getPointer() != null) {
                    serialMap.put(childId.getValue(), CfUtil.cfPointerToString(serialRef.getPointer()));
                }
                CfUtil.release(serialRef);

                IOKit.INSTANCE.IOObjectRelease(childDevice);
                childDevice = IOKit.INSTANCE.IOIteratorNext(childIter.getValue());
            }
            IOKit.INSTANCE.IOObjectRelease(childIter.getValue());

            IOKit.INSTANCE.IOObjectRelease(device);
            device = IOKit.INSTANCE.IOIteratorNext(iter.getValue());
        }
        IOKit.INSTANCE.IOObjectRelease(iter.getValue());

        // Build tree and return
        List<UsbDevice> controllerDevices = new ArrayList<UsbDevice>();
        for (Long controller : usbControllers) {
            controllerDevices.add(getDeviceAndChildren(controller));
        }
        return controllerDevices.toArray(new UsbDevice[controllerDevices.size()]);
    }

    /**
     * Recursively creates MacUsbDevices by fetching information from maps to
     * populate fields
     * 
     * @param registryEntryId
     *            The device unique registry id.
     * @return A MacUsbDevice corresponding to this device
     */
    private static MacUsbDevice getDeviceAndChildren(Long registryEntryId) {
        List<Long> childIds = hubMap.getOrDefault(registryEntryId, new ArrayList<>());
        List<MacUsbDevice> usbDevices = new ArrayList<>();
        for (Long id : childIds) {
            usbDevices.add(getDeviceAndChildren(id));
        }
        Collections.sort(usbDevices);
        return new MacUsbDevice(nameMap.getOrDefault(registryEntryId, ""), vendorMap.getOrDefault(registryEntryId, ""),
                serialMap.getOrDefault(registryEntryId, ""), usbDevices.toArray(new UsbDevice[usbDevices.size()]));
    }
}
