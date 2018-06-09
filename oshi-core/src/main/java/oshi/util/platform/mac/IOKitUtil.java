/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.util.platform.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.ptr.IntByReference; //NOSONAR
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFStringRef;
import oshi.jna.platform.mac.CoreFoundation.CFTypeRef;
import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.IOKit.MachPort;

/**
 * Provides utilities for IOKit
 *
 * @author widdis[at]gmail[dot]com
 */
public class IOKitUtil {
    private static final Logger LOG = LoggerFactory.getLogger(IOKitUtil.class);

    private static MachPort masterPort = new MachPort();

    private IOKitUtil() {
    }

    /**
     * Sets the masterPort value
     *
     * @return 0 if the value was successfully set, error value otherwise
     */
    private static int setMasterPort() {
        if (masterPort.getValue() == 0) {
            int result = IOKit.INSTANCE.IOMasterPort(0, masterPort);
            if (result != 0) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(String.format("Error: IOMasterPort() = %08x", result));
                }
                return result;
            }
        }
        return 0;
    }

    /**
     * Gets the IO Registry root
     *
     * @return an int handle to the IORoot
     */
    public static int getRoot() {
        if (setMasterPort() == 0) {
            int root = IOKit.INSTANCE.IORegistryGetRootEntry(masterPort.getValue());
            if (root == 0) {
                LOG.error("No IO Root found.");
            }
            return root;
        }
        return 0;
    }

    /**
     * Opens an IOService matching the given name
     *
     * @param serviceName
     *            The service name to match
     * @return an int handle to an IOService if successful, 0 if failed
     */
    public static int getMatchingService(String serviceName) {
        if (setMasterPort() == 0) {
            int service = IOKit.INSTANCE.IOServiceGetMatchingService(masterPort.getValue(),
                    IOKit.INSTANCE.IOServiceMatching(serviceName));
            if (service == 0) {
                LOG.error("No service found: {}", serviceName);
            }
            return service;
        }
        return 0;
    }

    /**
     * Convenience method to get matching IOService objects
     *
     * @param serviceName
     *            The service name to match
     * @param serviceIterator
     *            An interator over matching items, set on return
     * @return 0 if successful, an error code if failed.
     */
    public static int getMatchingServices(String serviceName, IntByReference serviceIterator) {
        int setMasterPort = setMasterPort();
        if (setMasterPort == 0) {
            return IOKit.INSTANCE.IOServiceGetMatchingServices(masterPort.getValue(),
                    IOKit.INSTANCE.IOServiceMatching(serviceName), serviceIterator);
        }
        return setMasterPort;
    }

    /**
     * Convenience method to get matching IOService objects
     *
     * @param matchingDictionary
     *            The dictionary to match
     * @param serviceIterator
     *            An interator over matching items, set on return
     * @return 0 if successful, an error code if failed.
     */
    public static int getMatchingServices(CFMutableDictionaryRef matchingDictionary, IntByReference serviceIterator) {
        int setMasterPort = setMasterPort();
        if (setMasterPort == 0) {
            return IOKit.INSTANCE.IOServiceGetMatchingServices(masterPort.getValue(), matchingDictionary,
                    serviceIterator);
        }
        return setMasterPort;
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
    public static String getIORegistryStringProperty(int entry, String key) {
        String value = null;
        CFStringRef keyAsCFString = CfUtil.getCFString(key);
        CFTypeRef valueAsCFString = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(entry, keyAsCFString,
                CoreFoundation.INSTANCE.CFAllocatorGetDefault(), 0);
        if (valueAsCFString != null && valueAsCFString.getPointer() != null) {
            value = CfUtil.cfPointerToString(valueAsCFString.getPointer());
        }
        CfUtil.release(valueAsCFString);
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
    public static long getIORegistryLongProperty(int entry, String key) {
        long value = 0L;
        CFStringRef keyAsCFString = CfUtil.getCFString(key);
        CFTypeRef valueAsCFNumber = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(entry, keyAsCFString,
                CoreFoundation.INSTANCE.CFAllocatorGetDefault(), 0);
        if (valueAsCFNumber != null && valueAsCFNumber.getPointer() != null) {
            value = CfUtil.cfPointerToLong(valueAsCFNumber.getPointer());
        }
        CfUtil.release(valueAsCFNumber);
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
    public static int getIORegistryIntProperty(int entry, String key) {
        int value = 0;
        CFStringRef keyAsCFString = CfUtil.getCFString(key);
        CFTypeRef valueAsCFNumber = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(entry, keyAsCFString,
                CoreFoundation.INSTANCE.CFAllocatorGetDefault(), 0);
        if (valueAsCFNumber != null && valueAsCFNumber.getPointer() != null) {
            value = CfUtil.cfPointerToInt(valueAsCFNumber.getPointer());
        }
        CfUtil.release(valueAsCFNumber);
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
    public static boolean getIORegistryBooleanProperty(int entry, String key) {
        boolean value = false;
        CFStringRef keyAsCFString = CfUtil.getCFString(key);
        CFTypeRef valueAsCFBoolean = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(entry, keyAsCFString,
                CoreFoundation.INSTANCE.CFAllocatorGetDefault(), 0);
        if (valueAsCFBoolean != null && valueAsCFBoolean.getPointer() != null) {
            value = CfUtil.cfPointerToBoolean(valueAsCFBoolean.getPointer());
        }
        CfUtil.release(valueAsCFBoolean);
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
    public static byte[] getIORegistryByteArrayProperty(int entry, String key) {
        byte[] value = null;
        CFStringRef keyAsCFString = CfUtil.getCFString(key);
        CFTypeRef valueAsCFData = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(entry, keyAsCFString,
                CoreFoundation.INSTANCE.CFAllocatorGetDefault(), 0);
        if (valueAsCFData != null && valueAsCFData.getPointer() != null) {
            int length = CoreFoundation.INSTANCE.CFDataGetLength(valueAsCFData);
            PointerByReference p = CoreFoundation.INSTANCE.CFDataGetBytePtr(valueAsCFData);
            value = p.getPointer().getByteArray(0, length);
        }
        CfUtil.release(valueAsCFData);
        return value;
    }

    /**
     * Convenience method to get the IO dictionary matching a bsd name
     *
     * @param bsdName
     *            The bsd name of the registry entry
     * @return The dictionary ref
     */
    public static CFMutableDictionaryRef getBSDNameMatchingDict(String bsdName) {
        if (setMasterPort() == 0) {
            return IOKit.INSTANCE.IOBSDNameMatching(masterPort.getValue(), 0, bsdName);
        }
        return null;
    }
}