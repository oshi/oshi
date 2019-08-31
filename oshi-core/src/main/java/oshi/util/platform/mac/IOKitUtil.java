/*
 * Copyright (c) 2019 Daniel Widdis
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package oshi.util.platform.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFBooleanRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDataRef;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.ptr.LongByReference;

import oshi.jna.platform.mac.IOKit;

/**
 * Provides utilities for IOKit
 */
public class IOKitUtil {
    private static final Logger LOG = LoggerFactory.getLogger(IOKitUtil.class);

    private IOKitUtil() {
    }

    /**
     * Gets the masterPort value
     *
     * @return The master port. Callers should release when finished.
     */
    private static long getMasterPort() {
        LongByReference port = new LongByReference();
        IOKit.INSTANCE.IOMasterPort(0, port);
        return port.getValue();
    }

    /**
     * Gets the IO Registry root
     *
     * @return an int handle to the IORoot. Callers should release when finished.
     */
    public static long getRoot() {
        long masterPort = getMasterPort();
        long root = IOKit.INSTANCE.IORegistryGetRootEntry(masterPort);
        IOKit.INSTANCE.IOObjectRelease(masterPort);
        return root;
    }

    /**
     * Opens an IOService matching the given name
     *
     * @param serviceName
     *            The service name to match
     * @return an int handle to an IOService if successful, 0 if failed. Callers
     *         should release when finished.
     */
    public static long getMatchingService(String serviceName) {
        long masterPort = getMasterPort();
        long service = IOKit.INSTANCE.IOServiceGetMatchingService(masterPort,
                IOKit.INSTANCE.IOServiceMatching(serviceName));
        IOKit.INSTANCE.IOObjectRelease(masterPort);
        if (service == 0) {
            LOG.error("No service found: {}", serviceName);
        }
        return service;
    }

    /**
     * Convenience method to get matching IOService objects
     *
     * @param serviceName
     *            The service name to match
     * @param serviceIterator
     *            An interator over matching items, set on return. Callers should
     *            release when finished.
     * @return 0 if successful, an error code if failed.
     */
    public static int getMatchingServices(String serviceName, LongByReference serviceIterator) {
        long masterPort = getMasterPort();
        int result = IOKit.INSTANCE.IOServiceGetMatchingServices(masterPort,
                IOKit.INSTANCE.IOServiceMatching(serviceName), serviceIterator);
        IOKit.INSTANCE.IOObjectRelease(masterPort);
        return result;
    }

    /**
     * Convenience method to get matching IOService objects
     *
     * @param matchingDictionary
     *            The dictionary to match
     * @param serviceIterator
     *            An interator over matching items, set on return. Callers should
     *            release when finished.
     * @return 0 if successful, an error code if failed.
     */
    public static int getMatchingServices(CFMutableDictionaryRef matchingDictionary, LongByReference serviceIterator) {
        long masterPort = getMasterPort();
        int result = IOKit.INSTANCE.IOServiceGetMatchingServices(masterPort, matchingDictionary, serviceIterator);
        IOKit.INSTANCE.IOObjectRelease(masterPort);
        return result;
    }

    /**
     * Convenience method to get the IO dictionary matching a bsd name
     *
     * @param bsdName
     *            The bsd name of the registry entry
     * @return The dictionary ref. Callers should release when finished.
     */
    public static CFMutableDictionaryRef getBSDNameMatchingDict(String bsdName) {
        long masterPort = getMasterPort();
        CFMutableDictionaryRef result = IOKit.INSTANCE.IOBSDNameMatching(masterPort, 0, bsdName);
        IOKit.INSTANCE.IOObjectRelease(masterPort);
        return result;
    }

    /**
     * Convenience method to get a String value from an IO Registry
     *
     * @param entry
     *            A handle to the registry entry
     * @param key
     *            The string name of the key to retrieve
     * @return The value of the registry entry if it exists; null otherwise
     */
    public static String getIORegistryStringProperty(long entry, String key) {
        String value = null;
        CFStringRef keyAsCFString = CFStringRef.createCFString(key);
        CFTypeRef valueAsCFType = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(entry, keyAsCFString,
                CoreFoundation.INSTANCE.CFAllocatorGetDefault(), 0);
        if (valueAsCFType != null && valueAsCFType.getPointer() != null) {
            CFStringRef valueAsCFString = new CFStringRef(valueAsCFType.getPointer());
            value = valueAsCFString.stringValue();
        }
        keyAsCFString.release();
        if (valueAsCFType != null) {
            valueAsCFType.release();
        }
        return value;
    }

    /**
     * Convenience method to get a Long value from an IO Registry
     *
     * @param entry
     *            A handle to the registry entry
     * @param key
     *            The string name of the key to retrieve
     * @return The value of the registry entry if it exists; 0 otherwise
     */
    public static long getIORegistryLongProperty(long entry, String key) {
        long value = 0L;
        CFStringRef keyAsCFString = CFStringRef.createCFString(key);
        CFTypeRef valueAsCFType = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(entry, keyAsCFString,
                CoreFoundation.INSTANCE.CFAllocatorGetDefault(), 0);
        if (valueAsCFType != null && valueAsCFType.getPointer() != null) {
            CFNumberRef valueAsCFNumber = new CFNumberRef(valueAsCFType.getPointer());
            value = valueAsCFNumber.longValue();
        }
        keyAsCFString.release();
        if (valueAsCFType != null) {
            valueAsCFType.release();
        }
        return value;
    }

    /**
     * Convenience method to get an Int value from an IO Registry
     *
     * @param entry
     *            A handle to the registry entry
     * @param key
     *            The string name of the key to retrieve
     * @return The value of the registry entry if it exists; 0 otherwise
     */
    public static int getIORegistryIntProperty(long entry, String key) {
        int value = 0;
        CFStringRef keyAsCFString = CFStringRef.createCFString(key);
        CFTypeRef valueAsCFType = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(entry, keyAsCFString,
                CoreFoundation.INSTANCE.CFAllocatorGetDefault(), 0);
        if (valueAsCFType != null && valueAsCFType.getPointer() != null) {
            CFNumberRef valueAsCFNumber = new CFNumberRef(valueAsCFType.getPointer());
            value = valueAsCFNumber.intValue();
        }
        keyAsCFString.release();
        if (valueAsCFType != null) {
            valueAsCFType.release();
        }
        return value;
    }

    /**
     * Convenience method to get a Boolean value from an IO Registry
     *
     * @param entry
     *            A handle to the registry entry
     * @param key
     *            The string name of the key to retrieve
     * @return The value of the registry entry if it exists; false otherwise
     */
    public static boolean getIORegistryBooleanProperty(long entry, String key) {
        boolean value = false;
        CFStringRef keyAsCFString = CFStringRef.createCFString(key);
        CFTypeRef valueAsCFType = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(entry, keyAsCFString,
                CoreFoundation.INSTANCE.CFAllocatorGetDefault(), 0);
        if (valueAsCFType != null && valueAsCFType.getPointer() != null) {
            CFBooleanRef valueAsCFBoolean = new CFBooleanRef(valueAsCFType.getPointer());
            value = valueAsCFBoolean.booleanValue();
        }
        keyAsCFString.release();
        if (valueAsCFType != null) {
            valueAsCFType.release();
        }
        return value;
    }

    /**
     * Convenience method to get a byte array value from an IO Registry
     *
     * @param entry
     *            A handle to the registry entry
     * @param key
     *            The string name of the key to retrieve
     * @return The value of the registry entry if it exists; null otherwise
     */
    public static byte[] getIORegistryByteArrayProperty(long entry, String key) {
        byte[] value = null;
        CFStringRef keyAsCFString = CFStringRef.createCFString(key);
        CFTypeRef valueAsCFType = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(entry, keyAsCFString,
                CoreFoundation.INSTANCE.CFAllocatorGetDefault(), 0);
        if (valueAsCFType != null && valueAsCFType.getPointer() != null) {
            CFDataRef valueAsCFData = new CFDataRef(valueAsCFType.getPointer());
            int length = (int) CoreFoundation.INSTANCE.CFDataGetLength(valueAsCFData);
            Pointer p = CoreFoundation.INSTANCE.CFDataGetBytePtr(valueAsCFData);
            value = p.getByteArray(0, length);
        }
        keyAsCFString.release();
        if (valueAsCFType != null) {
            valueAsCFType.release();
        }
        return value;
    }
}
