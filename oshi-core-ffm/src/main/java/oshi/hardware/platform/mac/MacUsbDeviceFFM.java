/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.util.ExceptionUtil.runOrLog;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.platform.mac.CoreFoundation.CFAllocatorRef;
import oshi.ffm.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.ffm.platform.mac.CoreFoundation.CFStringRef;
import oshi.ffm.platform.mac.CoreFoundation.CFTypeRef;
import oshi.ffm.platform.mac.CoreFoundationFunctions;
import oshi.ffm.platform.mac.IOKit.IOIterator;
import oshi.ffm.platform.mac.IOKit.IORegistryEntry;
import oshi.ffm.util.platform.mac.IOKitUtilFFM;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.platform.mac.MacUsbDevice;

/**
 * Enumerates Mac USB devices via FFM/IOKit, adapting the FFM registry-entry wrapper to {@link MacUsbDevice}'s
 * backend-neutral view so the tree-building logic is shared.
 */
public final class MacUsbDeviceFFM {

    private MacUsbDeviceFFM() {
    }

    private static final String IOSERVICE = "IOService";
    private static final Logger LOG = LoggerFactory.getLogger(MacUsbDeviceFFM.class);

    /**
     * Instantiates the USB controller device tree. The flat form is derived by the caller (the HAL).
     *
     * @return a list of USB controllers, each with its connected-device tree.
     */
    public static List<UsbDevice> getUsbDevices() {
        IORegistryEntry root = IOKitUtilFFM.getRoot();
        return MacUsbDevice.getUsbDevices(root == null ? null : new FfmRegistryEntry(root));
    }

    /**
     * Adapts an FFM {@link IORegistryEntry} to {@link MacUsbDevice.RegistryEntry}.
     */
    private static final class FfmRegistryEntry implements MacUsbDevice.RegistryEntry {
        private final IORegistryEntry entry;

        FfmRegistryEntry(IORegistryEntry entry) {
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
            return parent == null ? null : new FfmRegistryEntry(parent);
        }

        @Override
        public MacUsbDevice.RegistryIterator getChildIterator(String plane) {
            IOIterator iter = entry.getChildIterator(plane);
            return iter == null ? null : new FfmRegistryIterator(iter);
        }

        @Override
        public String[] lookupControllerVidPid() {
            String[] result = new String[2];
            CFStringRef locationIDKey = CFStringRef.createCFString("locationID");
            CFStringRef ioPropertyMatchKey = CFStringRef.createCFString("IOPropertyMatch");
            try (locationIDKey; ioPropertyMatchKey) {
                MemorySegment ref = entry.createCFProperty(locationIDKey.segment());
                if (ref == null || ref.equals(MemorySegment.NULL)) {
                    return result;
                }
                CFTypeRef locationId = new CFTypeRef(ref);
                try (locationId) {
                    // Build matching dict: { IOPropertyMatch: { locationID: <locationId> } }
                    runOrLog(() -> {
                        CFAllocatorRef alloc = new CFAllocatorRef(CoreFoundationFunctions.CFAllocatorGetDefault());
                        CFMutableDictionaryRef propertyDict = new CFMutableDictionaryRef(CoreFoundationFunctions
                                .CFDictionaryCreateMutable(alloc.segment(), 0, MemorySegment.NULL, MemorySegment.NULL));
                        propertyDict.setValue(locationIDKey, locationId);
                        CFMutableDictionaryRef matchingDict = new CFMutableDictionaryRef(CoreFoundationFunctions
                                .CFDictionaryCreateMutable(alloc.segment(), 0, MemorySegment.NULL, MemorySegment.NULL));
                        matchingDict.setValue(ioPropertyMatchKey, propertyDict);

                        IOIterator serviceIterator = IOKitUtilFFM.getMatchingServices(matchingDict.segment());
                        propertyDict.release();
                        readVidPid(serviceIterator, result);
                    }, LOG, "Failed to retrieve controller vendor/product IDs");
                }
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
            try (serviceIterator) {
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
            }
        }

        @Override
        public void release() {
            entry.release();
        }
    }

    /**
     * Adapts an FFM {@link IOIterator} to {@link MacUsbDevice.RegistryIterator}.
     */
    private static final class FfmRegistryIterator implements MacUsbDevice.RegistryIterator {
        private final IOIterator iter;

        FfmRegistryIterator(IOIterator iter) {
            this.iter = iter;
        }

        @Override
        public MacUsbDevice.RegistryEntry next() {
            IORegistryEntry next = iter.next();
            return next == null ? null : new FfmRegistryEntry(next);
        }

        @Override
        public void release() {
            iter.release();
        }
    }
}
