/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.platform.mac.CoreFoundation.CFBooleanRef;
import oshi.ffm.platform.mac.CoreFoundation.CFDataRef;
import oshi.ffm.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.platform.mac.CoreFoundation.CFNumberRef;
import oshi.ffm.platform.mac.CoreFoundation.CFStringRef;
import oshi.ffm.platform.mac.CoreFoundationFunctions;
import oshi.ffm.platform.mac.CoreGraphicsFunctions;
import oshi.ffm.platform.mac.IOKit.IOIterator;
import oshi.ffm.platform.mac.IOKit.IORegistryEntry;
import oshi.ffm.platform.mac.ObjCFunctions;
import oshi.ffm.util.platform.mac.IOKitUtilFFM;
import oshi.hardware.Display;
import oshi.hardware.DisplayInfo;
import oshi.hardware.common.AbstractDisplay;
import oshi.util.EdidUtil;
import oshi.util.ExceptionUtil;

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

    MacDisplayFFM(DisplayInfo displayInfo) {
        super(displayInfo);
        LOG.debug("Initialized MacDisplayFFM (synthetic)");
    }

    public static List<Display> getDisplays() {
        List<Display> displays = new ArrayList<>();
        // Intel: real EDID exposed under IODisplayConnect (returns nothing on Apple Silicon).
        displays.addAll(getDisplaysFromService("IODisplayConnect", "IODisplayEDID", "IOService"));
        // Apple Silicon external monitors: same stripped EDID as Intel path, just different service.
        displays.addAll(getDisplaysFromService("IOPortTransportStateDisplayPort", "EDID", null));
        // Apple Silicon built-in panel: no real EDID exposed, synthesize from DisplayAttributes.
        displays.addAll(getAppleSiliconBuiltInDisplay());
        return displays;
    }

    private static List<Display> getDisplaysFromService(String serviceName, String edidKeyName, String childEntryName) {
        List<Display> displays = new ArrayList<>();
        IOIterator serviceIterator = IOKitUtilFFM.getMatchingServices(serviceName);
        if (serviceIterator == null) {
            return displays;
        }
        CFStringRef cfEdid = CFStringRef.createCFString(edidKeyName);
        try (serviceIterator; cfEdid) {
            IORegistryEntry sdService = serviceIterator.next();
            while (sdService != null) {
                try (IORegistryEntry current = sdService) {
                    IORegistryEntry childEntry = childEntryName == null ? null : current.getChildEntry(childEntryName);
                    IORegistryEntry propertySource = childEntry != null ? childEntry : current;
                    try {
                        MemorySegment edidRaw = propertySource.createCFProperty(cfEdid.segment());
                        if (edidRaw != null && !edidRaw.equals(MemorySegment.NULL)) {
                            try (CFDataRef edid = new CFDataRef(edidRaw)) {
                                byte[] bytes = edid.getBytes();
                                if (bytes.length > 0) {
                                    displays.add(new MacDisplayFFM(bytes));
                                }
                            }
                        }
                    } finally {
                        if (childEntry != null) {
                            childEntry.release();
                        }
                    }
                }
                sdService = serviceIterator.next();
            }
        }
        return displays;
    }

    /**
     * Discovers the Apple Silicon built-in display by matching the stable {@code IOMobileFramebuffer} base class (the
     * leaf class name varies by macOS version). External monitors are skipped here (they are already enumerated via
     * {@code IOPortTransportStateDisplayPort} with their real EDID); only the built-in panel, which has no physical
     * EDID EPROM, is synthesized from {@code DisplayAttributes}.
     *
     * @return A list containing the built-in display, or empty if not found
     */
    private static List<Display> getAppleSiliconBuiltInDisplay() {
        List<Display> displays = new ArrayList<>();
        IOIterator iter = IOKitUtilFFM.getMatchingServices("IOMobileFramebuffer");
        if (iter == null) {
            return displays;
        }
        CFStringRef cfExternal = CFStringRef.createCFString("external");
        CFStringRef cfAttrs = CFStringRef.createCFString("DisplayAttributes");
        try (iter; cfExternal; cfAttrs) {
            IORegistryEntry fb = iter.next();
            while (fb != null) {
                try (IORegistryEntry current = fb) {
                    addBuiltInDisplay(current, cfExternal, cfAttrs, displays);
                }
                fb = iter.next();
            }
        }
        return displays;
    }

    // Synthesizes a display for the built-in panel from its DisplayAttributes dictionary. External framebuffer nodes
    // (marked with "external" = true) are skipped, as are idle pipes with no DisplayAttributes.
    private static void addBuiltInDisplay(IORegistryEntry fb, CFStringRef cfExternal, CFStringRef cfAttrs,
            List<Display> displays) {
        // Skip external monitors — they are already enumerated via IOPortTransportStateDisplayPort.
        MemorySegment externalRaw = fb.createCFProperty(cfExternal.segment());
        if (externalRaw != null && !externalRaw.equals(MemorySegment.NULL)) {
            try (CFBooleanRef externalBool = new CFBooleanRef(externalRaw)) {
                if (externalBool.booleanValue()) {
                    return;
                }
            }
        }
        // Synthesize from DisplayAttributes, read from the node or (fallback) its IODeviceTree parent.
        MemorySegment attrsRaw = fb.createCFProperty(cfAttrs.segment());
        if (attrsRaw == null || attrsRaw.equals(MemorySegment.NULL)) {
            IORegistryEntry parent = fb.getParentEntry("IODeviceTree");
            if (parent != null) {
                try (parent) {
                    attrsRaw = parent.createCFProperty(cfAttrs.segment());
                }
            }
        }
        if (attrsRaw == null || attrsRaw.equals(MemorySegment.NULL)) {
            return;
        }
        try (CFDictionaryRef attrs = new CFDictionaryRef(attrsRaw)) {
            DisplayInfo info = synthesize(fb, attrs);
            if (info != null) {
                displays.add(new MacDisplayFFM(info));
            }
        }
    }

    // Maps an Apple Silicon DisplayAttributes dictionary onto a synthetic DisplayInfo via EdidUtil, enriched with
    // native resolution and device name from the framebuffer node and CoreGraphics.
    private static DisplayInfo synthesize(IORegistryEntry fb, CFDictionaryRef attrs) {
        CFDictionaryRef product = cfDictGetDictionary(attrs, "ProductAttributes");
        if (product == null) {
            return null;
        }
        Long legacyMfg = cfDictGetLong(product, "LegacyManufacturerID");
        Long week = cfDictGetLong(product, "WeekOfManufacture");
        Long year = cfDictGetLong(product, "YearOfManufacture");
        String model = cfDictGetString(product, "ProductName");
        String serial = cfDictGetString(product, "AlphanumericSerialNumber");
        // Native pixel resolution from the framebuffer node.
        Long displayWidth = fb.getLongProperty("DisplayWidth");
        Long displayHeight = fb.getLongProperty("DisplayHeight");
        // Device tree name for fallback model name.
        String fallbackName = null;
        String ioNameMatched = fb.getStringProperty("IONameMatched");
        if (ioNameMatched != null) {
            String shortName = ioNameMatched.contains(",") ? ioNameMatched.substring(0, ioNameMatched.indexOf(','))
                    : ioNameMatched;
            fallbackName = shortName + " (Built-in Display)";
        }
        // CoreGraphics properties: model number, serial number, physical size, and localized name.
        Integer cgModel = null;
        Integer cgSerial = null;
        Double widthMm = null;
        Double heightMm = null;
        String displayName = null;
        int builtInId = findBuiltInDisplayId();
        if (builtInId >= 0) {
            try (Arena sizeArena = Arena.ofConfined()) {
                cgModel = CoreGraphicsFunctions.CGDisplayModelNumber(builtInId);
                cgSerial = CoreGraphicsFunctions.CGDisplaySerialNumber(builtInId);
                MemorySegment size = CoreGraphicsFunctions.CGDisplayScreenSize(sizeArena, builtInId);
                widthMm = size.get(ValueLayout.JAVA_DOUBLE, 0);
                heightMm = size.get(ValueLayout.JAVA_DOUBLE, 8);
            } catch (Throwable t) {
                LOG.debug("Failed to get built-in display CoreGraphics properties", t);
            }
            displayName = getLocalizedDisplayName(builtInId);
        }
        return EdidUtil.synthesizeDisplayInfo(legacyMfg, cgModel, cgSerial, week == null ? null : week.intValue(),
                year == null ? null : year.intValue(), model, serial, displayWidth, displayHeight, fallbackName,
                widthMm, heightMm, displayName);
    }

    // Returns the CGDirectDisplayID of the built-in display, or -1 if not found.
    private static int findBuiltInDisplayId() {
        return ExceptionUtil.getIntOrDefault(() -> {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment countSeg = arena.allocate(ValueLayout.JAVA_INT);
                if (CoreGraphicsFunctions.CGGetActiveDisplayList(0, MemorySegment.NULL, countSeg) != 0) {
                    return -1;
                }
                int count = countSeg.get(ValueLayout.JAVA_INT, 0);
                if (count == 0) {
                    return -1;
                }
                MemorySegment idsSeg = arena.allocate(ValueLayout.JAVA_INT, count);
                if (CoreGraphicsFunctions.CGGetActiveDisplayList(count, idsSeg, countSeg) != 0) {
                    return -1;
                }
                for (int i = 0; i < count; i++) {
                    int id = idsSeg.getAtIndex(ValueLayout.JAVA_INT, i);
                    if (CoreGraphicsFunctions.CGDisplayIsBuiltin(id) != 0) {
                        return id;
                    }
                }
            }
            return -1;
        }, -1, LOG, "Failed to find built-in display ID: {}");
    }

    // Returns the NSScreen.localizedName for the given CGDirectDisplayID, or null.
    private static String getLocalizedDisplayName(int targetDisplayId) {
        return ExceptionUtil.getOrDefault(() -> {
            try (Arena arena = Arena.ofConfined()) {
                // Autorelease pool is thread-local; concurrent callers each get their own pool
                MemorySegment pool = ObjCFunctions.objc_autoreleasePoolPush();
                try {
                    return queryLocalizedDisplayName(arena, targetDisplayId);
                } finally {
                    ObjCFunctions.objc_autoreleasePoolPop(pool);
                }
            }
        }, null, LOG, "Failed to get localized display name: {}");
    }

    private static String queryLocalizedDisplayName(Arena arena, int targetDisplayId) throws Throwable {
        MemorySegment nsScreenClass = ObjCFunctions.objc_getClass(arena.allocateFrom("NSScreen"));
        if (nsScreenClass.address() == 0) {
            return null;
        }
        MemorySegment selScreens = ObjCFunctions.sel_registerName(arena.allocateFrom("screens"));
        MemorySegment selCount = ObjCFunctions.sel_registerName(arena.allocateFrom("count"));
        MemorySegment selObjectAt = ObjCFunctions.sel_registerName(arena.allocateFrom("objectAtIndex:"));
        MemorySegment selDeviceDesc = ObjCFunctions.sel_registerName(arena.allocateFrom("deviceDescription"));
        MemorySegment selLocalizedName = ObjCFunctions.sel_registerName(arena.allocateFrom("localizedName"));

        MemorySegment screensArray = ObjCFunctions.objc_msgSend(nsScreenClass, selScreens);
        if (screensArray.address() == 0) {
            return null;
        }
        long count = ObjCFunctions.objc_msgSend_long(screensArray, selCount);

        try (CFStringRef cfKey = CFStringRef.createCFString("NSScreenNumber")) {
            for (long i = 0; i < count; i++) {
                MemorySegment screen = ObjCFunctions.objc_msgSend(screensArray, selObjectAt, i);
                if (screen.address() == 0) {
                    continue;
                }
                MemorySegment deviceDesc = ObjCFunctions.objc_msgSend(screen, selDeviceDesc);
                if (deviceDesc.address() == 0) {
                    continue;
                }
                MemorySegment cfNum = CoreFoundationFunctions.CFDictionaryGetValue(deviceDesc, cfKey.segment());
                if (cfNum.address() == 0) {
                    continue;
                }
                MemorySegment outId = arena.allocate(ValueLayout.JAVA_INT);
                // kCFNumberSInt32Type = 3
                boolean ok = CoreFoundationFunctions.CFNumberGetValue(cfNum, 3, outId);
                if (ok && outId.get(ValueLayout.JAVA_INT, 0) == targetDisplayId) {
                    MemorySegment nsName = ObjCFunctions.objc_msgSend(screen, selLocalizedName);
                    if (nsName.address() != 0) {
                        // Borrowed autoreleased reference — do not close/release
                        return new CFStringRef(nsName).stringValue();
                    }
                }
            }
        }
        return null;
    }

    // CFDictionary accessors. CFDictionaryGetValue returns a borrowed reference: the returned wrappers must NOT be
    // released.

    private static CFDictionaryRef cfDictGetDictionary(CFDictionaryRef dict, String key) {
        try (CFStringRef k = CFStringRef.createCFString(key)) {
            MemorySegment v = dict.getValue(k);
            return v == null || v.equals(MemorySegment.NULL) ? null : new CFDictionaryRef(v);
        }
    }

    private static String cfDictGetString(CFDictionaryRef dict, String key) {
        try (CFStringRef k = CFStringRef.createCFString(key)) {
            MemorySegment v = dict.getValue(k);
            return v == null || v.equals(MemorySegment.NULL) ? null : new CFStringRef(v).stringValue();
        }
    }

    private static Long cfDictGetLong(CFDictionaryRef dict, String key) {
        try (CFStringRef k = CFStringRef.createCFString(key)) {
            MemorySegment v = dict.getValue(k);
            return v == null || v.equals(MemorySegment.NULL) ? null : new CFNumberRef(v).longValue();
        }
    }
}
