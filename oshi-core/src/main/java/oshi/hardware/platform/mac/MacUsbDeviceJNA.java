/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.List;
import java.util.Locale;

import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.platform.mac.MacUsbDevice;

/**
 * Enumerates Mac USB devices via JNA/IOKit, adapting the JNA registry-entry wrapper to {@link MacUsbDevice}'s
 * backend-neutral view so the tree-building logic is shared.
 */
public final class MacUsbDeviceJNA {

    private MacUsbDeviceJNA() {
    }

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

    private static final String IOSERVICE = "IOService";

    /**
     * Instantiates the USB controller device tree. The flat form is derived by the caller (the HAL).
     *
     * @return a list of USB controllers, each with its connected-device tree.
     */
    public static List<UsbDevice> getUsbDevices() {
        IORegistryEntry root = IOKitUtil.getRoot();
        return MacUsbDevice.getUsbDevices(root == null ? null : new JnaRegistryEntry(root));
    }

    /**
     * Adapts a JNA {@link IORegistryEntry} to {@link MacUsbDevice.RegistryEntry}.
     */
    private static final class JnaRegistryEntry implements MacUsbDevice.RegistryEntry {
        private final IORegistryEntry entry;

        JnaRegistryEntry(IORegistryEntry entry) {
            this.entry = entry;
        }

        @Override
        public long getRegistryEntryID() {
            return entry.getRegistryEntryID();
        }

        @Override
        public String getName() {
            return entry.getName();
        }

        @Override
        public String getStringProperty(String key) {
            return entry.getStringProperty(key);
        }

        @Override
        public Long getLongProperty(String key) {
            return entry.getLongProperty(key);
        }

        @Override
        public MacUsbDevice.RegistryEntry getParentEntry(String plane) {
            IORegistryEntry parent = entry.getParentEntry(plane);
            return parent == null ? null : new JnaRegistryEntry(parent);
        }

        @Override
        public MacUsbDevice.RegistryIterator getChildIterator(String plane) {
            IOIterator iter = entry.getChildIterator(plane);
            return iter == null ? null : new JnaRegistryIterator(iter);
        }

        @Override
        public String[] lookupControllerVidPid() {
            String[] result = new String[2];
            CFStringRef locationIDKey = CFStringRef.createCFString("locationID");
            CFStringRef ioPropertyMatchKey = CFStringRef.createCFString("IOPropertyMatch");
            try {
                CFTypeRef locationId = entry.createCFProperty(locationIDKey);
                if (locationId == null) {
                    return result;
                }
                try {
                    // Create a matching property dictionary from the locationId
                    CFMutableDictionaryRef propertyDict = CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(),
                            new CFIndex(0), null, null);
                    propertyDict.setValue(locationIDKey, locationId);
                    CFMutableDictionaryRef matchingDict = CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(),
                            new CFIndex(0), null, null);
                    matchingDict.setValue(ioPropertyMatchKey, propertyDict);

                    // search for all IOServices that match the locationID; getMatchingServices releases matchingDict
                    IOIterator serviceIterator = IOKitUtil.getMatchingServices(matchingDict);
                    propertyDict.release();
                    readVidPid(serviceIterator, result);
                } finally {
                    locationId.release();
                }
            } finally {
                locationIDKey.release();
                ioPropertyMatchKey.release();
            }
            return result;
        }

        /**
         * Iterates matching services, reading vendor-id/device-id from the first parent that carries them.
         *
         * @param serviceIterator the iterator of matching services, or {@code null}
         * @param result          the two-element {@code {vendorId, productId}} array to populate
         */
        private static void readVidPid(IOIterator serviceIterator, String[] result) {
            if (serviceIterator == null) {
                return;
            }
            boolean found = false;
            IORegistryEntry matchingService = serviceIterator.next();
            while (matchingService != null && !found) {
                // The parent holds the vendor-id/device-id keys, each a 4-byte array
                IORegistryEntry parent = matchingService.getParentEntry(IOSERVICE);
                if (parent != null) {
                    byte[] vid = parent.getByteArrayProperty("vendor-id");
                    if (vid != null && vid.length >= 2) {
                        result[0] = String.format(Locale.ROOT, "%02x%02x", vid[1], vid[0]);
                        found = true;
                    }
                    byte[] pid = parent.getByteArrayProperty("device-id");
                    if (pid != null && pid.length >= 2) {
                        result[1] = String.format(Locale.ROOT, "%02x%02x", pid[1], pid[0]);
                        found = true;
                    }
                    parent.release();
                }
                matchingService.release();
                matchingService = serviceIterator.next();
            }
            serviceIterator.release();
        }

        @Override
        public void release() {
            entry.release();
        }
    }

    /**
     * Adapts a JNA {@link IOIterator} to {@link MacUsbDevice.RegistryIterator}.
     */
    private static final class JnaRegistryIterator implements MacUsbDevice.RegistryIterator {
        private final IOIterator iter;

        JnaRegistryIterator(IOIterator iter) {
            this.iter = iter;
        }

        @Override
        public MacUsbDevice.RegistryEntry next() {
            IORegistryEntry next = iter.next();
            return next == null ? null : new JnaRegistryEntry(next);
        }

        @Override
        public void release() {
            iter.release();
        }
    }
}
