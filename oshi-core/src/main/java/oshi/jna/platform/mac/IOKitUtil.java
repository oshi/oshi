/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.jna.platform.mac;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.jna.platform.mac.IOKit.IOIterator;
import oshi.jna.platform.mac.IOKit.IORegistryEntry;
import oshi.jna.platform.mac.IOKit.IOService;

/**
 * Provides utilities for IOKit.
 */
public class IOKitUtil {
    private static final IOKit IO = IOKit.INSTANCE;
    private static final oshi.jna.platform.mac.SystemB SYS = oshi.jna.platform.mac.SystemB.INSTANCE;

    private IOKitUtil() {
    }

    /**
     * Gets a pointer to the Mach Master Port.
     *
     * @return The master port.
     *         <p>
     *         Multiple calls to {@link #getMasterPort} will not result in leaking
     *         ports (each call to {@link IOKit#IOMasterPort} adds another send
     *         right to the port) but it is considered good programming practice to
     *         deallocate the port when you are finished with it, using
     *         {@link oshi.jna.platform.mac.SystemB#mach_port_deallocate}.
     */
    public static int getMasterPort() {
        IntByReference port = new IntByReference();
        IO.IOMasterPort(0, port);
        return port.getValue();
    }

    /**
     * Gets the IO Registry root.
     *
     * @return a handle to the IORoot. Callers should release when finished, using
     *         {@link IOKit#IOObjectRelease}.
     */
    public static IORegistryEntry getRoot() {
        int masterPort = getMasterPort();
        IORegistryEntry root = IO.IORegistryGetRootEntry(masterPort);
        SYS.mach_port_deallocate(SYS.mach_task_self(), masterPort);
        return root;
    }

    /**
     * Opens a the first IOService matching a service name.
     *
     * @param serviceName
     *            The service name to match
     * @return a handle to an IOService if successful, {@code null} if failed.
     *         Callers should release when finished, using
     *         {@link IOKit#IOObjectRelease}.
     */
    public static IOService getMatchingService(String serviceName) {
        CFMutableDictionaryRef dict = IO.IOServiceMatching(serviceName);
        if (dict != null) {
            return getMatchingService(dict);
        }
        return null;
    }

    /**
     * Opens a the first IOService matching a dictionary.
     *
     * @param matchingDictionary
     *            The dictionary to match. This method will consume a reference to
     *            the dictionary.
     * @return a handle to an IOService if successful, {@code null} if failed.
     *         Callers should release when finished, using
     *         {@link IOKit#IOObjectRelease}.
     */
    public static IOService getMatchingService(CFDictionaryRef matchingDictionary) {
        int masterPort = getMasterPort();
        IOService service = IO.IOServiceGetMatchingService(masterPort, matchingDictionary);
        SYS.mach_port_deallocate(SYS.mach_task_self(), masterPort);
        return service;
    }

    /**
     * Convenience method to get IOService objects matching a service name.
     *
     * @param serviceName
     *            The service name to match
     * @return a handle to an IOIterator if successful, {@code null} if failed.
     *         Callers should release when finished, using
     *         {@link IOKit#IOObjectRelease}.
     */
    public static IOIterator getMatchingServices(String serviceName) {
        CFMutableDictionaryRef dict = IO.IOServiceMatching(serviceName);
        if (dict != null) {
            return getMatchingServices(dict);
        }
        return null;
    }

    /**
     * Convenience method to get IOService objects matching a dictionary.
     *
     * @param matchingDictionary
     *            The dictionary to match. This method will consume a reference to
     *            the dictionary.
     * @return a handle to an IOIterator if successful, {@code null} if failed.
     *         Callers should release when finished, using
     *         {@link IOKit#IOObjectRelease}.
     */
    public static IOIterator getMatchingServices(CFDictionaryRef matchingDictionary) {
        int masterPort = getMasterPort();
        PointerByReference serviceIterator = new PointerByReference();
        int result = IO.IOServiceGetMatchingServices(masterPort, matchingDictionary, serviceIterator);
        SYS.mach_port_deallocate(SYS.mach_task_self(), masterPort);
        if (result == 0 && serviceIterator.getValue() != null) {
            return new IOIterator(serviceIterator.getValue());
        }
        return null;
    }

    /**
     * Convenience method to get the IO dictionary matching a bsd name.
     *
     * @param bsdName
     *            The bsd name of the registry entry
     * @return The dictionary ref if successful, {@code null} if failed. Callers
     *         should release when finished, using {@link IOKit#IOObjectRelease}.
     */
    public static CFMutableDictionaryRef getBSDNameMatchingDict(String bsdName) {
        int masterPort = getMasterPort();
        CFMutableDictionaryRef result = IO.IOBSDNameMatching(masterPort, 0, bsdName);
        SYS.mach_port_deallocate(SYS.mach_task_self(), masterPort);
        return result;
    }
}
