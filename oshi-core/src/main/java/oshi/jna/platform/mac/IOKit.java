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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.mac.CoreFoundation.CFAllocatorRef;
import oshi.jna.platform.mac.CoreFoundation.CFArrayRef;
import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFStringRef;
import oshi.jna.platform.mac.CoreFoundation.CFTypeRef;

/**
 * Power Supply stats. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 */
public interface IOKit extends Library {
    /** Constant <code>INSTANCE</code> */
    IOKit INSTANCE = Native.load("IOKit", IOKit.class);

    /** Constant <code>IOPS_NAME_KEY</code> */
    CFStringRef IOPS_NAME_KEY = CFStringRef.toCFString("Name");

    /** Constant <code>IOPS_IS_PRESENT_KEY</code> */
    CFStringRef IOPS_IS_PRESENT_KEY = CFStringRef.toCFString("Is Present");

    /** Constant <code>IOPS_CURRENT_CAPACITY_KEY</code> */
    CFStringRef IOPS_CURRENT_CAPACITY_KEY = CFStringRef.toCFString("Current Capacity");

    /** Constant <code>IOPS_MAX_CAPACITY_KEY</code> */
    CFStringRef IOPS_MAX_CAPACITY_KEY = CFStringRef.toCFString("Max Capacity");

    /** Constant <code>SMC_KEY_FAN_NUM="FNum"</code> */
    String SMC_KEY_FAN_NUM = "FNum";

    /** Constant <code>SMC_KEY_FAN_SPEED="F%dAc"</code> */
    String SMC_KEY_FAN_SPEED = "F%dAc";

    /** Constant <code>SMC_KEY_CPU_TEMP="TC0P"</code> */
    String SMC_KEY_CPU_TEMP = "TC0P";

    /** Constant <code>SMC_KEY_CPU_VOLTAGE="VC0C"</code> */
    String SMC_KEY_CPU_VOLTAGE = "VC0C";

    /** Constant <code>SMC_CMD_READ_BYTES=5</code> */
    byte SMC_CMD_READ_BYTES = 5;

    /** Constant <code>SMC_CMD_READ_KEYINFO=9</code> */
    byte SMC_CMD_READ_KEYINFO = 9;

    /** Constant <code>KERNEL_INDEX_SMC=2</code> */
    int KERNEL_INDEX_SMC = 2;

    /**
     * Holds the return value of SMC version query.
     */
    @FieldOrder({ "major", "minor", "build", "reserved", "release" })
    class SMCKeyDataVers extends Structure {
        public byte major;
        public byte minor;
        public byte build;
        public byte[] reserved = new byte[1];
        public short release;

    }

    /**
     * Holds the return value of SMC pLimit query.
     */
    @FieldOrder({ "version", "length", "cpuPLimit", "gpuPLimit", "memPLimit" })
    class SMCKeyDataPLimitData extends Structure {
        public short version;
        public short length;
        public int cpuPLimit;
        public int gpuPLimit;
        public int memPLimit;
    }

    /**
     * Holds the return value of SMC KeyInfo query.
     */
    @FieldOrder({ "dataSize", "dataType", "dataAttributes" })
    class SMCKeyDataKeyInfo extends Structure {
        public int dataSize;
        public int dataType;
        public byte dataAttributes;
    }

    /**
     * Holds the return value of SMC query.
     */
    @FieldOrder({ "key", "vers", "pLimitData", "keyInfo", "result", "status", "data8", "data32", "bytes" })
    class SMCKeyData extends Structure {
        public int key;
        public SMCKeyDataVers vers;
        public SMCKeyDataPLimitData pLimitData;
        public SMCKeyDataKeyInfo keyInfo;
        public byte result;
        public byte status;
        public byte data8;
        public int data32;
        public byte[] bytes = new byte[32];
    }

    /**
     * Holds an SMC value
     */
    @FieldOrder({ "key", "dataSize", "dataType", "bytes" })
    class SMCVal extends Structure {
        public byte[] key = new byte[5];
        public int dataSize;
        public byte[] dataType = new byte[5];
        public byte[] bytes = new byte[32];
    }

    class IOConnect extends IntByReference {
    }

    class MachPort extends IntByReference {
    }

    /**
     * <p>
     * IOPSCopyPowerSourcesInfo.
     * </p>
     *
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFTypeRef} object.
     */
    CFTypeRef IOPSCopyPowerSourcesInfo();

    /**
     * <p>
     * IOPSCopyPowerSourcesList.
     * </p>
     *
     * @param blob
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFTypeRef} object.
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFArrayRef} object.
     */
    CFArrayRef IOPSCopyPowerSourcesList(CFTypeRef blob);

    /**
     * <p>
     * IOPSGetPowerSourceDescription.
     * </p>
     *
     * @param blob
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFTypeRef} object.
     * @param ps
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFTypeRef} object.
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef}
     *         object.
     */
    CFDictionaryRef IOPSGetPowerSourceDescription(CFTypeRef blob, CFTypeRef ps);

    /**
     * <p>
     * IOPSGetTimeRemainingEstimate.
     * </p>
     *
     * @return a double.
     */
    double IOPSGetTimeRemainingEstimate();

    /**
     * <p>
     * IOMasterPort.
     * </p>
     *
     * @param unused
     *            a int.
     * @param masterPort
     *            a {@link com.sun.jna.ptr.IntByReference} object.
     * @return a int.
     */
    int IOMasterPort(int unused, IntByReference masterPort);

    /**
     * <p>
     * IOServiceMatching.
     * </p>
     *
     * @param name
     *            a {@link java.lang.String} object.
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef}
     *         object.
     */
    CFMutableDictionaryRef IOServiceMatching(String name);

    /**
     * <p>
     * IOBSDNameMatching.
     * </p>
     *
     * @param masterPort
     *            a int.
     * @param options
     *            a int.
     * @param bsdName
     *            a {@link java.lang.String} object.
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef}
     *         object.
     */
    CFMutableDictionaryRef IOBSDNameMatching(int masterPort, int options, String bsdName);

    /**
     * <p>
     * IOServiceGetMatchingService.
     * </p>
     *
     * @param port
     *            a int.
     * @param matchingDictionary
     *            a
     *            {@link oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef}
     *            object.
     * @return a int.
     */
    int IOServiceGetMatchingService(int port, CFMutableDictionaryRef matchingDictionary);

    /**
     * <p>
     * IOServiceGetMatchingServices.
     * </p>
     *
     * @param port
     *            a int.
     * @param matchingDictionary
     *            a
     *            {@link oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef}
     *            object.
     * @param iterator
     *            a {@link com.sun.jna.ptr.IntByReference} object.
     * @return a int.
     */
    int IOServiceGetMatchingServices(int port, CFMutableDictionaryRef matchingDictionary, IntByReference iterator);

    /**
     * <p>
     * IOServiceOpen.
     * </p>
     *
     * @param service
     *            a int.
     * @param owningTask
     *            a int.
     * @param type
     *            a int.
     * @param connect
     *            a {@link com.sun.jna.ptr.IntByReference} object.
     * @return a int.
     */
    int IOServiceOpen(int service, int owningTask, int type, IntByReference connect);

    /**
     * <p>
     * IOServiceClose.
     * </p>
     *
     * @param connect
     *            a int.
     * @return a int.
     */
    int IOServiceClose(int connect);

    /**
     * <p>
     * IOObjectRelease.
     * </p>
     *
     * @param object
     *            a int.
     */
    void IOObjectRelease(int object);

    /**
     * <p>
     * IOIteratorNext.
     * </p>
     *
     * @param iterator
     *            a int.
     * @return a int.
     */
    int IOIteratorNext(int iterator);

    /**
     * <p>
     * IOObjectConformsTo.
     * </p>
     *
     * @param object
     *            a int.
     * @param className
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    boolean IOObjectConformsTo(int object, String className);

    // Requires OS X 10.5+
    /**
     * <p>
     * IOConnectCallStructMethod.
     * </p>
     *
     * @param connection
     *            a int.
     * @param selector
     *            a int.
     * @param inputStructure
     *            a {@link com.sun.jna.Structure} object.
     * @param structureInputSize
     *            a int.
     * @param outputStructure
     *            a {@link com.sun.jna.Structure} object.
     * @param structureOutputSize
     *            a {@link com.sun.jna.ptr.IntByReference} object.
     * @return a int.
     */
    int IOConnectCallStructMethod(int connection, int selector, Structure inputStructure, int structureInputSize,
            Structure outputStructure, IntByReference structureOutputSize);

    /**
     * <p>
     * IORegistryEntryCreateCFProperty.
     * </p>
     *
     * @param entry
     *            a int.
     * @param key
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFStringRef} object.
     * @param allocator
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFAllocatorRef}
     *            object.
     * @param options
     *            a int.
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFTypeRef} object.
     */
    CFTypeRef IORegistryEntryCreateCFProperty(int entry, CFStringRef key, CFAllocatorRef allocator, int options);

    /**
     * <p>
     * IORegistryEntrySearchCFProperty.
     * </p>
     *
     * @param entry
     *            a int.
     * @param plane
     *            a {@link java.lang.String} object.
     * @param key
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFStringRef} object.
     * @param allocator
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFAllocatorRef}
     *            object.
     * @param options
     *            a int.
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFTypeRef} object.
     */
    CFTypeRef IORegistryEntrySearchCFProperty(int entry, String plane, CFStringRef key, CFAllocatorRef allocator,
            int options);

    /**
     * <p>
     * IORegistryEntryCreateCFProperties.
     * </p>
     *
     * @param entry
     *            a int.
     * @param propsPtr
     *            a {@link com.sun.jna.ptr.PointerByReference} object.
     * @param allocator
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFAllocatorRef}
     *            object.
     * @param options
     *            a int.
     * @return a int.
     */
    int IORegistryEntryCreateCFProperties(int entry, PointerByReference propsPtr, CFAllocatorRef allocator,
            int options);

    /**
     * <p>
     * CFCopyDescription.
     * </p>
     *
     * @param type
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFTypeRef} object.
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFStringRef} object.
     */
    CFStringRef CFCopyDescription(CFTypeRef type);

    /**
     * <p>
     * IORegistryEntryGetName.
     * </p>
     *
     * @param entry
     *            a int.
     * @param name
     *            a {@link com.sun.jna.Pointer} object.
     * @return a int.
     */
    int IORegistryEntryGetName(int entry, Pointer name);

    /**
     * <p>
     * IORegistryEntryGetRegistryEntryID.
     * </p>
     *
     * @param entry
     *            a int.
     * @param id
     *            a {@link com.sun.jna.ptr.LongByReference} object.
     * @return a int.
     */
    int IORegistryEntryGetRegistryEntryID(int entry, LongByReference id);

    /**
     * <p>
     * IORegistryEntryGetParentEntry.
     * </p>
     *
     * @param entry
     *            a int.
     * @param plane
     *            a {@link java.lang.String} object.
     * @param parent
     *            a {@link com.sun.jna.ptr.IntByReference} object.
     * @return a int.
     */
    int IORegistryEntryGetParentEntry(int entry, String plane, IntByReference parent);

    /**
     * <p>
     * IORegistryEntryGetChildEntry.
     * </p>
     *
     * @param entry
     *            a int.
     * @param plane
     *            a {@link java.lang.String} object.
     * @param child
     *            a {@link com.sun.jna.ptr.IntByReference} object.
     * @return a int.
     */
    int IORegistryEntryGetChildEntry(int entry, String plane, IntByReference child);

    /**
     * <p>
     * IORegistryEntryGetChildIterator.
     * </p>
     *
     * @param entry
     *            a int.
     * @param plane
     *            a {@link java.lang.String} object.
     * @param iter
     *            a {@link com.sun.jna.ptr.IntByReference} object.
     * @return a int.
     */
    int IORegistryEntryGetChildIterator(int entry, String plane, IntByReference iter);

    /**
     * <p>
     * IORegistryGetRootEntry.
     * </p>
     *
     * @param masterPort
     *            a int.
     * @return a int.
     */
    int IORegistryGetRootEntry(int masterPort);
}
