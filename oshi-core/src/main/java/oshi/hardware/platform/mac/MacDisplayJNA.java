/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFBooleanRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDataRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.platform.mac.CoreGraphics;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.mac.MacDisplayPort;
import oshi.hardware.Display;
import oshi.hardware.DisplayInfo;
import oshi.hardware.common.AbstractDisplay;
import oshi.jna.platform.mac.CoreGraphicsExt;
import oshi.jna.platform.mac.ObjCRuntime;
import oshi.util.Constants;
import oshi.util.EdidUtil;
import oshi.util.ExceptionUtil;
import oshi.util.platform.mac.CFUtil;

/**
 * A Display
 */
@Immutable
final class MacDisplayJNA extends AbstractDisplay {

    private static final Logger LOG = LoggerFactory.getLogger(MacDisplayJNA.class);

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

    /** kCFNumberSInt64Type, as the CFIndex expected by CFNumberGetValue. */
    private static final CFIndex K_CF_NUMBER_SINT64 = new CFIndex(4);

    private final String devicePort;

    /**
     * Constructor for MacDisplayJNA from a real EDID byte array.
     *
     * @param edid a byte array representing a display EDID
     */
    MacDisplayJNA(byte[] edid) {
        this(edid, Constants.UNKNOWN);
    }

    /**
     * Constructor for MacDisplayJNA from a real EDID byte array with a device port.
     *
     * @param edid       a byte array representing a display EDID
     * @param devicePort the device port this display is attached to
     */
    MacDisplayJNA(byte[] edid, String devicePort) {
        super(edid);
        this.devicePort = devicePort;
        LOG.debug("Initialized MacDisplayJNA");
    }

    /**
     * Constructor for MacDisplayJNA from a synthetic {@link DisplayInfo}, used for the Apple Silicon built-in panel
     * which has no EDID EPROM.
     *
     * @param displayInfo the synthesized display info
     * @param devicePort  the device port this display is attached to
     */
    MacDisplayJNA(DisplayInfo displayInfo, String devicePort) {
        super(displayInfo);
        this.devicePort = devicePort;
        LOG.debug("Initialized MacDisplayJNA (synthetic)");
    }

    @Override
    public String getDevicePort() {
        return this.devicePort;
    }

    /**
     * Gets Display Information
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        List<Display> displays = new ArrayList<>();
        // Intel: real EDID exposed under IODisplayConnect (returns nothing on Apple Silicon). No port name available.
        displays.addAll(getDisplaysFromService("IODisplayConnect", "IODisplayEDID", "IOService", null));
        // Apple Silicon external monitors: same stripped EDID as Intel path, plus the port from TransportDescription.
        displays.addAll(
                getDisplaysFromService("IOPortTransportStateDisplayPort", "EDID", null, "TransportDescription"));
        // Apple Silicon built-in panel: no real EDID exposed, synthesize from DisplayAttributes.
        displays.addAll(getAppleSiliconBuiltInDisplay());
        return displays;
    }

    /**
     * Helper method to get displays from a specific IOKit service
     *
     * @param serviceName    The IOKit service name to search for
     * @param edidKeyName    The key name for the EDID property
     * @param childEntryName The name of the child entry to search in, or null to search directly in the service
     * @param portKeyName    The key name for the port property (e.g. {@code TransportDescription}), or null if the
     *                       service does not expose one
     * @return List of Display objects found using this service
     */
    private static List<Display> getDisplaysFromService(String serviceName, String edidKeyName,
            @Nullable String childEntryName, @Nullable String portKeyName) {
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
                        CFTypeRef edidRaw = propertySource.createCFProperty(cfEdid);
                        if (edidRaw != null) {
                            CFDataRef edid = new CFDataRef(edidRaw.getPointer());
                            try {
                                // EDID is a byte array of 128 bytes (or more)
                                int length = edid.getLength();
                                Pointer p = edid.getBytePtr();
                                if (length > 0) {
                                    String devicePort = portKeyName == null ? Constants.UNKNOWN
                                            : MacDisplayPort.fromTransportDescription(
                                                    propertySource.getStringProperty(portKeyName));
                                    displays.add(new MacDisplayJNA(p.getByteArray(0, length), devicePort));
                                }
                            } finally {
                                edid.release();
                            }
                        }
                        if (childEntryName != null) {
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

    /**
     * Discovers the Apple Silicon built-in display by matching the stable {@code IOMobileFramebuffer} base class. The
     * leaf class name varies by macOS version ({@code IOMobileFramebufferShim} on current releases, {@code AppleCLCD2}
     * on older ones), so matching the base class covers all generations. External monitors are skipped here (they are
     * already enumerated via {@code IOPortTransportStateDisplayPort} with their real EDID); only the built-in panel,
     * which has no physical EDID EPROM, is synthesized from {@code DisplayAttributes}.
     *
     * @return A list containing the built-in display, or empty if not found
     */
    private static List<Display> getAppleSiliconBuiltInDisplay() {
        List<Display> displays = new ArrayList<>();
        IOIterator iter = IOKitUtil.getMatchingServices("IOMobileFramebuffer");
        if (iter == null) {
            return displays;
        }
        CFStringRef cfExternal = CFStringRef.createCFString("external");
        CFStringRef cfAttrs = CFStringRef.createCFString("DisplayAttributes");
        try {
            IORegistryEntry fb = iter.next();
            while (fb != null) {
                try {
                    addBuiltInDisplay(fb, cfExternal, cfAttrs, displays);
                } finally {
                    fb.release();
                }
                fb = iter.next();
            }
        } finally {
            iter.release();
            cfExternal.release();
            cfAttrs.release();
        }
        return displays;
    }

    // Synthesizes a display for the built-in panel from its DisplayAttributes dictionary. External framebuffer nodes
    // (marked with "external" = true) are skipped, as are idle pipes with no DisplayAttributes.
    private static void addBuiltInDisplay(IORegistryEntry fb, CFStringRef cfExternal, CFStringRef cfAttrs,
            List<Display> displays) {
        // Skip external monitors — they are already enumerated via IOPortTransportStateDisplayPort.
        CFTypeRef externalRef = fb.createCFProperty(cfExternal);
        if (externalRef != null) {
            try {
                if (new CFBooleanRef(externalRef.getPointer()).booleanValue()) {
                    return;
                }
            } finally {
                externalRef.release();
            }
        }
        // Synthesize from DisplayAttributes, read from the node or (fallback) its IODeviceTree parent.
        CFTypeRef attrsRaw = fb.createCFProperty(cfAttrs);
        if (attrsRaw == null) {
            IORegistryEntry parent = fb.getParentEntry("IODeviceTree");
            if (parent != null) {
                try {
                    attrsRaw = parent.createCFProperty(cfAttrs);
                } finally {
                    parent.release();
                }
            }
        }
        if (attrsRaw == null) {
            return;
        }
        // The device tree name (e.g. "disp0,t6030") gives both the port and the fallback model name.
        String devicePort = MacDisplayPort.fromDeviceTreeName(fb.getStringProperty("IONameMatched"));
        try {
            DisplayInfo info = synthesize(fb, new CFDictionaryRef(attrsRaw.getPointer()), devicePort);
            if (info != null) {
                displays.add(new MacDisplayJNA(info, devicePort));
            }
        } finally {
            attrsRaw.release();
        }
    }

    // Maps an Apple Silicon DisplayAttributes dictionary onto a synthetic DisplayInfo via EdidUtil, enriched with
    // native resolution and device name from the framebuffer node and CoreGraphics.
    private static @Nullable DisplayInfo synthesize(IORegistryEntry fb, CFDictionaryRef attrs, String devicePort) {
        CFDictionaryRef product = CFUtil.getDictionary(attrs, "ProductAttributes");
        if (product == null) {
            return null;
        }
        Long legacyMfg = CFUtil.getLong(product, "LegacyManufacturerID");
        Long week = CFUtil.getLong(product, "WeekOfManufacture");
        Long year = CFUtil.getLong(product, "YearOfManufacture");
        String model = CFUtil.getString(product, "ProductName");
        String serial = CFUtil.getString(product, "AlphanumericSerialNumber");
        // Native pixel resolution from the framebuffer node.
        Long displayWidth = fb.getLongProperty("DisplayWidth");
        Long displayHeight = fb.getLongProperty("DisplayHeight");
        // Device tree name (the port) doubles as the fallback model name.
        String fallbackName = Constants.UNKNOWN.equals(devicePort) ? null : devicePort + " (Built-in Display)";
        // CoreGraphics properties: model number, serial number, physical size, and localized name.
        Integer cgModel = null;
        Integer cgSerial = null;
        Double widthMm = null;
        Double heightMm = null;
        String displayName = null;
        int builtInId = findBuiltInDisplayId();
        if (builtInId >= 0) {
            try {
                CoreGraphics cg = CoreGraphics.INSTANCE;
                cgModel = cg.CGDisplayModelNumber(builtInId);
                cgSerial = cg.CGDisplaySerialNumber(builtInId);
                CoreGraphicsExt.CGSizeByValue size = CoreGraphicsExt.INSTANCE.CGDisplayScreenSize(builtInId);
                widthMm = size.width;
                heightMm = size.height;
            } catch (Exception e) {
                LOG.debug("Failed to get built-in display CoreGraphics properties", e);
            }
            displayName = getLocalizedDisplayName(builtInId);
        }
        return EdidUtil.synthesizeDisplayInfo(legacyMfg, cgModel, cgSerial, week == null ? null : week.intValue(),
                year == null ? null : year.intValue(), model, serial, displayWidth, displayHeight, fallbackName,
                widthMm, heightMm, displayName);
    }

    // Returns the CGDirectDisplayID of the built-in display, or -1 if not found.
    private static int findBuiltInDisplayId() {
        CoreGraphics cg = CoreGraphics.INSTANCE;
        IntByReference count = new IntByReference();
        if (cg.CGGetActiveDisplayList(0, null, count) != 0 || count.getValue() == 0) {
            return -1;
        }
        int[] displayIds = new int[count.getValue()];
        if (cg.CGGetActiveDisplayList(displayIds.length, displayIds, count) != 0) {
            return -1;
        }
        for (int id : displayIds) {
            if (cg.CGDisplayIsBuiltin(id) != 0) {
                return id;
            }
        }
        return -1;
    }

    // Returns the NSScreen.localizedName for the given CGDirectDisplayID, or null.
    private static @Nullable String getLocalizedDisplayName(int targetDisplayId) {
        return ExceptionUtil.getOrDefault(() -> {
            ObjCRuntime objc = ObjCRuntime.INSTANCE;
            // Autorelease pool is thread-local; concurrent callers each get their own pool
            Pointer pool = objc.objc_autoreleasePoolPush();
            try {
                return queryLocalizedDisplayName(objc, targetDisplayId);
            } finally {
                objc.objc_autoreleasePoolPop(pool);
            }
        }, null, LOG, "Failed to get localized display name");
    }

    private static @Nullable String queryLocalizedDisplayName(ObjCRuntime objc, int targetDisplayId) {
        Pointer nsScreenClass = objc.objc_getClass("NSScreen");
        if (nsScreenClass == null) {
            return null;
        }
        Pointer selScreens = objc.sel_registerName("screens");
        Pointer selCount = objc.sel_registerName("count");
        Pointer selObjectAt = objc.sel_registerName("objectAtIndex:");
        Pointer selDeviceDesc = objc.sel_registerName("deviceDescription");
        Pointer selLocalizedName = objc.sel_registerName("localizedName");

        Pointer screensArray = objc.objc_msgSend(nsScreenClass, selScreens);
        if (screensArray == null) {
            return null;
        }
        long count = Pointer.nativeValue(objc.objc_msgSend(screensArray, selCount));
        CFStringRef cfKey = CFStringRef.createCFString("NSScreenNumber");
        try {
            for (long i = 0; i < count; i++) {
                Pointer screen = objc.objc_msgSend(screensArray, selObjectAt, i);
                if (screen == null) {
                    continue;
                }
                Pointer deviceDesc = objc.objc_msgSend(screen, selDeviceDesc);
                if (deviceDesc == null) {
                    continue;
                }
                Pointer cfNum = CF.CFDictionaryGetValue(new CFDictionaryRef(deviceDesc), cfKey);
                if (cfNum == null) {
                    continue;
                }
                LongByReference outId = new LongByReference();
                if (CF.CFNumberGetValue(new CFNumberRef(cfNum), K_CF_NUMBER_SINT64, outId) != 0
                        && (int) outId.getValue() == targetDisplayId) {
                    Pointer nsName = objc.objc_msgSend(screen, selLocalizedName);
                    if (nsName != null) {
                        return new CFStringRef(nsName).stringValue();
                    }
                }
            }
        } finally {
            cfKey.release();
        }
        return null;
    }
}
