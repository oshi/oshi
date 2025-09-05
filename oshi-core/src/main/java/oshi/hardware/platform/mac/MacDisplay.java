/*
 * Copyright 2016-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFDataRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;

/**
 * A Display
 */
@Immutable
final class MacDisplay extends AbstractDisplay {

    private static final Logger LOG = LoggerFactory.getLogger(MacDisplay.class);

    /**
     * Constructor for MacDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    MacDisplay(byte[] edid, String connectionPort) {
        super(edid, connectionPort);
        LOG.debug("Initialized MacDisplay");
    }

    /**
     * Gets Display Information
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        List<Display> displays = new ArrayList<>();
        // Intel
        displays.addAll(getDisplaysFromService("IODisplayConnect", "IODisplayEDID", "IOService"));
        // Apple Silicon
        displays.addAll(getDisplaysFromService("IOPortTransportStateDisplayPort", "EDID", null));

        return displays;
    }

    /**
     * Helper method to get displays from a specific IOKit service
     *
     * @param serviceName    The IOKit service name to search for
     * @param edidKeyName    The key name for the EDID property
     * @param childEntryName The name of the child entry to search in, or null to search directly in the service
     * @return List of Display objects found using this service
     */
    private static List<Display> getDisplaysFromService(String serviceName, String edidKeyName, String childEntryName) {
        List<Display> displays = new ArrayList<>();

        IOIterator serviceIterator = IOKitUtil.getMatchingServices(serviceName);
        if (serviceIterator != null) {
            CFStringRef cfEdid = CFStringRef.createCFString(edidKeyName);
            CFStringRef cfConnectionPort = CFStringRef.createCFString("ParentBuiltInPortTypeDescription");
            IORegistryEntry sdService = serviceIterator.next();

            byte[] edid = null;
            String connectionPort = null;

            while (sdService != null) {
                IORegistryEntry propertySource = null;

                try {
                    propertySource = childEntryName == null ? sdService : sdService.getChildEntry(childEntryName);
                    if (propertySource != null) {

                        edid = getEdidFromPropertySource(propertySource, cfEdid);
                        // Apple silicon displays connection port in the same registry entry as EDID
                        if (childEntryName == null)
                            connectionPort = getConnectionPortFromPropertySource(propertySource, cfConnectionPort);

                        if (Objects.nonNull(edid))
                            displays.add(new MacDisplay(edid, connectionPort));

                        if (childEntryName != null && propertySource != null) {
                            propertySource.release();
                        }
                    }
                } finally {
                    sdService.release();
                    sdService = serviceIterator.next();
                }
            }
            serviceIterator.release();
            cfEdid.release();
        }
        return displays;
    }

    private static byte[] getEdidFromPropertySource(IORegistryEntry propertySource, CFStringRef cfEdid) {

        byte[] edid = null;
        CFTypeRef edidRaw = propertySource.createCFProperty(cfEdid);
        if (edidRaw != null) {
            CFDataRef edidRef = new CFDataRef(edidRaw.getPointer());
            try {
                // EDID is a byte array of 128 bytes (or more)
                int length = edidRef.getLength();
                Pointer p = edidRef.getBytePtr();
                edid = p.getByteArray(0, length);
            } finally {
                edidRef.release();
            }
        }

        return edid;
    }

    private static String getConnectionPortFromPropertySource(IORegistryEntry propertySource,
            CFStringRef cfConnectionPort) {

        String connectionPort = null;
        CFTypeRef connectionPortRaw = propertySource.createCFProperty(cfConnectionPort);

        if (connectionPortRaw != null) {
            CFStringRef connectionPortRef = new CFStringRef(connectionPortRaw.getPointer());
            try {
                connectionPort = connectionPortRef.stringValue();
            } finally {
                connectionPortRef.release();
            }
        }

        return connectionPort;
    }

}
