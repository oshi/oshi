/*
 * Copyright 2016-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDataRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;
import com.sun.jna.ptr.IntByReference;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.jna.platform.mac.CoreGraphics;
import oshi.jna.platform.mac.CoreGraphics.*;
import oshi.util.EdidUtil;
import oshi.util.ParseUtil;
import static oshi.util.FormatUtil.*;

/**
 * A Display
 */
@Immutable
final class MacDisplay extends AbstractDisplay {

    private static final Logger LOG = LoggerFactory.getLogger(MacDisplay.class);

    private static final CoreGraphics CG = CoreGraphics.INSTANCE;
    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

    private Map<String, String> displayParams;

    /**
     * Constructor for MacDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    MacDisplay(byte[] edid) {
        super(edid);
        LOG.debug("Initialized MacDisplay");
    }

    /**
     * Constructor for MacDisplay.
     *
     * @param displayParams a map of the display's parameters
     */
    MacDisplay(Map<String, String> displayParams) {
        super(new byte[128]);
        this.displayParams = displayParams;
        LOG.debug("Initialized MacDisplay with a map");
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
        // CoreGraphics API
        displays.addAll(getDisplaysFromCoreGraphics());

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
            IORegistryEntry sdService = serviceIterator.next();

            while (sdService != null) {
                IORegistryEntry propertySource = null;
                try {
                    propertySource = childEntryName == null ? sdService : sdService.getChildEntry(childEntryName);
                    if (propertySource != null) {
                        try {
                            CFTypeRef edidRaw = propertySource.createCFProperty(cfEdid);
                            if (edidRaw != null) {
                                CFDataRef edid = new CFDataRef(edidRaw.getPointer());
                                try {
                                    // EDID is a byte array of 128 bytes (or more)
                                    int length = edid.getLength();
                                    Pointer p = edid.getBytePtr();
                                    displays.add(new MacDisplay(p.getByteArray(0, length)));
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
            serviceIterator.release();
            cfEdid.release();
        }
        return displays;
    }

    private static List<Display> getDisplaysFromCoreGraphics() {
        List<Display> displays = new ArrayList<>();

        IntByReference displayCount = new IntByReference();
        int result = CoreGraphics.INSTANCE.CGGetActiveDisplayList(0, null, displayCount);
        if (result != 0) {
            System.err.println("Failed to get display count, error: " + formatError(result));
            return displays;
        }
        int count = displayCount.getValue();
        int[] displayArray = new int[count];
        result = CoreGraphics.INSTANCE.CGGetActiveDisplayList(count, displayArray, displayCount);
        if (result != 0) {
            System.err.println("Failed to get displays, error: " + formatError(result));
            return displays;
        }
        count = displayCount.getValue();
        for (int i = 0; i < count; i++) {
            Map<String, String> display = new HashMap<>();
            display.put("VendorNumber", formatShort(CG.CGDisplayVendorNumber(displayArray[i])));
            display.put("ModelNumber", formatShort(CG.CGDisplayModelNumber(displayArray[i])));
            CGSize size = CG.CGDisplayScreenSize(displayArray[i]); // millimeters
            display.put("ScreenSizeH", Integer.toString(roundToInt(size.width / 10d)));
            display.put("ScreenSizeV", Integer.toString(roundToInt(size.height / 10d)));
            display.put("ScreenSizeDiag", Double.toString(roundToInt(Math.sqrt(size.width * size.width + size.height * size.height))));
            display.put("SerialNumber", formatError(CG.CGDisplaySerialNumber(displayArray[i])));
            display.put("IsMain", Boolean.toString(CG.CGDisplayIsMain(displayArray[i])));
            display.put("IsBuiltin", Boolean.toString(CG.CGDisplayIsBuiltin(displayArray[i])));
            display.put("Rotation", Double.toString(CG.CGDisplayRotation(displayArray[i])));
            display.put("ActivePixelsW", Long.toString(CG.CGDisplayPixelsWide(displayArray[i])));
            display.put("ActivePixelsH", Long.toString(CG.CGDisplayPixelsHigh(displayArray[i])));
            List<String> modes = new ArrayList<>();
            // Using "Copy" methods requires releasing them
            CFArrayRef modeArray = CG.CGDisplayCopyAllDisplayModes(displayArray[i], null);
            try {
                int modeCount = modeArray.getCount();
                for (int m = 0; m < modeCount; m++) {
                    CGDisplayModeRef mode = new CGDisplayModeRef(modeArray.getValueAtIndex(m));
                    modes.add(String.format(Locale.ROOT, "%d x %d (%.1fHz)", mode.pixelWidth(), mode.pixelHeight(), mode.refreshRate()));
                }
            } finally {
                CF.CFRelease(modeArray);
            }
            display.put("DisplayModes", String.join(", ", modes));
            displays.add(new MacDisplay(display));
        }
        return displays;
    }

    private static String formatShort(int value) {
        return String.format(Locale.ROOT, "%04X", value);
    }

    @Override
    public String toString() {
        if (displayParams == null) {
            return EdidUtil.toString(getEdid());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("  Manuf. ID=").append(displayParams.get("VendorNumber"));
        sb.append(", Product ID=").append(displayParams.get("ModelNumber"));
        sb.append(", Serial=").append(displayParams.get("SerialNumber"));
        int hSize = ParseUtil.parseIntOrDefault(displayParams.get("ScreenSizeH"), 0);
        int vSize = ParseUtil.parseIntOrDefault(displayParams.get("ScreenSizeV"), 0);
        long diag = Math.round(ParseUtil.parseDoubleOrDefault(displayParams.get("ScreenSizeDiag"), 0) / 25.4);
        sb.append("\n  ").append(hSize).append(" x ").append(vSize).append(" cm ");
        sb.append(String.format(Locale.ROOT, "(%.1f\" x %.1f\" / %d\" diagonal)", hSize / 2.54, vSize / 2.54, diag));
        sb.append("\n  Main Display: ").append(displayParams.containsKey("IsMain"));
        sb.append("\n  Built In: ").append(displayParams.get("IsBuiltin"));
        sb
            .append("\n  Active Pixels: ")
            .append(displayParams.get("ActivePixelsW"))
            .append(" x ")
            .append(displayParams.get("ActivePixelsH"));
        sb.append("\n  Rotation: ").append(displayParams.get("Rotation"));
        sb.append("\n  Display Modes: ").append(displayParams.get("DisplayModes"));

        return sb.toString();
    }
}
