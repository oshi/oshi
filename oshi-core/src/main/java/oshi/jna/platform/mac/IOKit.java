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
 *
 * @author widdis[at]gmail[dot]com
 */
public interface IOKit extends Library {
    IOKit INSTANCE = Native.load("IOKit", IOKit.class);

    CFStringRef IOPS_NAME_KEY = CFStringRef.toCFString("Name");

    CFStringRef IOPS_IS_PRESENT_KEY = CFStringRef.toCFString("Is Present");

    CFStringRef IOPS_CURRENT_CAPACITY_KEY = CFStringRef.toCFString("Current Capacity");

    CFStringRef IOPS_MAX_CAPACITY_KEY = CFStringRef.toCFString("Max Capacity");

    String SMC_KEY_FAN_NUM = "FNum";

    String SMC_KEY_FAN_SPEED = "F%dAc";

    String SMC_KEY_CPU_TEMP = "TC0P";

    String SMC_KEY_CPU_VOLTAGE = "VC0C";

    byte SMC_CMD_READ_BYTES = 5;

    byte SMC_CMD_READ_KEYINFO = 9;

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

    CFTypeRef IOPSCopyPowerSourcesInfo();

    CFArrayRef IOPSCopyPowerSourcesList(CFTypeRef blob);

    CFDictionaryRef IOPSGetPowerSourceDescription(CFTypeRef blob, CFTypeRef ps);

    double IOPSGetTimeRemainingEstimate();

    int IOMasterPort(int unused, IntByReference masterPort);

    CFMutableDictionaryRef IOServiceMatching(String name);

    CFMutableDictionaryRef IOBSDNameMatching(int masterPort, int options, String bsdName);

    int IOServiceGetMatchingService(int port, CFMutableDictionaryRef matchingDictionary);

    int IOServiceGetMatchingServices(int port, CFMutableDictionaryRef matchingDictionary, IntByReference iterator);

    int IOServiceOpen(int service, int owningTask, int type, IntByReference connect);

    int IOServiceClose(int connect);

    void IOObjectRelease(int object);

    int IOIteratorNext(int iterator);

    boolean IOObjectConformsTo(int object, String className);

    // Requires OS X 10.5+
    int IOConnectCallStructMethod(int connection, int selector, Structure inputStructure, int structureInputSize,
            Structure outputStructure, IntByReference structureOutputSize);

    CFTypeRef IORegistryEntryCreateCFProperty(int entry, CFStringRef key, CFAllocatorRef allocator, int options);

    CFTypeRef IORegistryEntrySearchCFProperty(int entry, String plane, CFStringRef key, CFAllocatorRef allocator,
            int options);

    int IORegistryEntryCreateCFProperties(int entry, PointerByReference propsPtr, CFAllocatorRef allocator,
            int options);

    CFStringRef CFCopyDescription(CFTypeRef type);

    int IORegistryEntryGetName(int entry, Pointer name);

    int IORegistryEntryGetRegistryEntryID(int entry, LongByReference id);

    int IORegistryEntryGetParentEntry(int entry, String plane, IntByReference parent);

    int IORegistryEntryGetChildEntry(int entry, String plane, IntByReference child);

    int IORegistryEntryGetChildIterator(int entry, String plane, IntByReference iter);

    int IORegistryGetRootEntry(int masterPort);
}
