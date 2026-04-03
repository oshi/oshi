/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.mac.CoreFoundation.CFDataRef;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.util.platform.mac.IOKitUtilFFM;

/**
 * A Display
 */
@Immutable
final class MacDisplayFFM extends AbstractDisplay {

    private static final Logger LOG = LoggerFactory.getLogger(MacDisplayFFM.class);

    MacDisplayFFM(byte[] edid) {
        super(edid);
        LOG.debug("Initialized MacDisplayFFM");
    }

    public static List<Display> getDisplays() {
        List<Display> displays = new ArrayList<>();
        displays.addAll(getDisplaysFromService("IODisplayConnect", "IODisplayEDID", "IOService"));
        displays.addAll(getDisplaysFromService("IOPortTransportStateDisplayPort", "EDID", null));
        return displays;
    }

    private static List<Display> getDisplaysFromService(String serviceName, String edidKeyName, String childEntryName) {
        List<Display> displays = new ArrayList<>();
        IOIterator serviceIterator = IOKitUtilFFM.getMatchingServices(serviceName);
        if (serviceIterator == null) {
            return displays;
        }
        CFStringRef cfEdid = CFStringRef.createCFString(edidKeyName);
        try {
            IORegistryEntry sdService = serviceIterator.next();
            while (sdService != null) {
                try {
                    IORegistryEntry propertySource = childEntryName == null ? sdService
                            : sdService.getChildEntry(childEntryName);
                    if (propertySource != null) {
                        try {
                            java.lang.foreign.MemorySegment edidRaw = propertySource.createCFProperty(cfEdid.segment());
                            if (edidRaw != null && !edidRaw.equals(java.lang.foreign.MemorySegment.NULL)) {
                                CFDataRef edid = new CFDataRef(edidRaw);
                                try {
                                    byte[] bytes = edid.getBytes();
                                    if (bytes.length > 0) {
                                        displays.add(new MacDisplayFFM(bytes));
                                    }
                                } finally {
                                    edid.release();
                                }
                            }
                        } finally {
                            if (childEntryName != null) {
                                propertySource.release();
                            }
                        }
                    }
                } finally {
                    sdService.release();
                    sdService = serviceIterator.next();
                }
            }
        } finally {
            serviceIterator.release();
            cfEdid.release();
        }
        return displays;
    }
}
