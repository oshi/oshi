/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static oshi.ffm.ForeignFunctions.getByteArrayFromNativePointer;
import static oshi.ffm.mac.CoreFoundation.kCFNumberFloat64Type;
import static oshi.ffm.mac.CoreFoundation.kCFNumberSInt32Type;
import static oshi.ffm.mac.CoreFoundation.kCFNumberSInt64Type;
import static oshi.ffm.mac.CoreFoundation.kCFStringEncodingUTF8;
import static oshi.ffm.mac.CoreFoundationFunctions.CFAllocatorGetDefault;
import static oshi.ffm.mac.CoreFoundationFunctions.CFDataGetBytePtr;
import static oshi.ffm.mac.CoreFoundationFunctions.CFDataGetLength;
import static oshi.ffm.mac.CoreFoundationFunctions.CFNumberGetValue;
import static oshi.ffm.mac.CoreFoundationFunctions.CFStringCreateWithCharacters;
import static oshi.ffm.mac.CoreFoundationFunctions.CFStringGetCString;
import static oshi.ffm.mac.CoreFoundationFunctions.CFStringGetLength;
import static oshi.ffm.mac.IOKitFunctions.IOIteratorNext;
import static oshi.ffm.mac.IOKitFunctions.IOObjectConformsTo;
import static oshi.ffm.mac.IOKitFunctions.IOObjectRelease;
import static oshi.ffm.mac.IOKitFunctions.IORegistryEntryCreateCFProperties;
import static oshi.ffm.mac.IOKitFunctions.IORegistryEntryCreateCFProperty;
import static oshi.ffm.mac.IOKitFunctions.IORegistryEntryGetChildEntry;
import static oshi.ffm.mac.IOKitFunctions.IORegistryEntryGetChildIterator;
import static oshi.ffm.mac.IOKitFunctions.IORegistryEntryGetName;
import static oshi.ffm.mac.IOKitFunctions.IORegistryEntryGetParentEntry;
import static oshi.ffm.mac.IOKitFunctions.IORegistryEntryGetRegistryEntryID;
import static oshi.ffm.mac.IOKitFunctions.IORegistryEntrySearchCFProperty;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * The I/O Kit framework implements non-kernel access to I/O Kit objects (drivers and nubs) through the device-interface
 * mechanism.
 */
public interface IOKit {

    int kIORegistryIterateRecursively = 0x00000001;
    int kIORegistryIterateParents = 0x00000002;
    int kIOReturnNoDevice = 0xe00002c0;
    double kIOPSTimeRemainingUnlimited = -2.0;
    double kIOPSTimeRemainingUnknown = -1.0;

    class IOObject {
        private final MemorySegment segment;

        public IOObject(MemorySegment segment) {
            this.segment = segment;
        }

        public MemorySegment segment() {
            return segment;
        }

        public boolean isNull() {
            return segment == null || segment.equals(MemorySegment.NULL);
        }

        public int release() {
            if (isNull()) {
                return 0;
            }
            try {
                return IOObjectRelease(segment);
            } catch (Throwable e) {
                return -1;
            }
        }

        public boolean conformsTo(String className) {
            if (isNull()) {
                return false;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment classNameStr = arena.allocateFrom(className);
                return IOObjectConformsTo(segment, classNameStr);
            } catch (Throwable e) {
                return false;
            }
        }
    }

    class IOIterator extends IOObject {
        public IOIterator(MemorySegment segment) {
            super(segment);
        }

        public IORegistryEntry next() {
            if (isNull()) {
                return null;
            }
            try {
                MemorySegment nextObj = IOIteratorNext(segment());
                return nextObj.equals(MemorySegment.NULL) ? null : new IORegistryEntry(nextObj);
            } catch (Throwable e) {
                return null;
            }
        }

        public List<IORegistryEntry> listEntries() {
            List<IORegistryEntry> entries = new ArrayList<>();
            if (isNull()) {
                return entries;
            }

            IORegistryEntry entry;
            while ((entry = next()) != null) {
                entries.add(entry);
            }
            return entries;
        }
    }

    class IORegistryEntry extends IOObject {
        public IORegistryEntry(MemorySegment segment) {
            super(segment);
        }

        public long getRegistryEntryID() {
            if (isNull()) {
                return 0;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment idSeg = arena.allocate(ValueLayout.JAVA_LONG);
                int result = IORegistryEntryGetRegistryEntryID(segment(), idSeg);
                if (result != 0) {
                    return 0;
                }
                return idSeg.get(ValueLayout.JAVA_LONG, 0);
            } catch (Throwable e) {
                return 0;
            }
        }

        public String getName() {
            if (isNull()) {
                return null;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment nameSeg = arena.allocate(128);
                int result = IORegistryEntryGetName(segment(), nameSeg);
                if (result != 0) {
                    return null;
                }
                return nameSeg.getString(0);
            } catch (Throwable e) {
                return null;
            }
        }

        public IOIterator getChildIterator(String plane) {
            if (isNull()) {
                return null;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment planeStr = arena.allocateFrom(plane);
                MemorySegment iterSeg = arena.allocate(ADDRESS);
                int result = IORegistryEntryGetChildIterator(segment(), planeStr, iterSeg);
                if (result != 0) {
                    return null;
                }
                MemorySegment iterator = iterSeg.get(ADDRESS, 0);
                return iterator.equals(MemorySegment.NULL) ? null : new IOIterator(iterator);
            } catch (Throwable e) {
                return null;
            }
        }

        public IORegistryEntry getChildEntry(String plane) {
            if (isNull()) {
                return null;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment planeStr = arena.allocateFrom(plane);
                MemorySegment childSeg = arena.allocate(ADDRESS);
                int result = IORegistryEntryGetChildEntry(segment(), planeStr, childSeg);
                if (result == kIOReturnNoDevice || result != 0) {
                    return null;
                }
                MemorySegment child = childSeg.get(ADDRESS, 0);
                return child.equals(MemorySegment.NULL) ? null : new IORegistryEntry(child);
            } catch (Throwable e) {
                return null;
            }
        }

        public IORegistryEntry getParentEntry(String plane) {
            if (isNull()) {
                return null;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment planeStr = arena.allocateFrom(plane);
                MemorySegment parentSeg = arena.allocate(ADDRESS);
                int result = IORegistryEntryGetParentEntry(segment(), planeStr, parentSeg);
                if (result == kIOReturnNoDevice || result != 0) {
                    return null;
                }
                MemorySegment parent = parentSeg.get(ADDRESS, 0);
                return parent.equals(MemorySegment.NULL) ? null : new IORegistryEntry(parent);
            } catch (Throwable e) {
                return null;
            }
        }

        public MemorySegment createCFProperty(MemorySegment key) {
            if (isNull()) {
                return MemorySegment.NULL;
            }
            try {
                MemorySegment allocator = CFAllocatorGetDefault();
                return IORegistryEntryCreateCFProperty(segment(), key, allocator, 0);
            } catch (Throwable e) {
                return MemorySegment.NULL;
            }
        }

        public MemorySegment createCFProperties() {
            if (isNull()) {
                return MemorySegment.NULL;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment propsSeg = arena.allocate(ADDRESS);
                MemorySegment allocator = CFAllocatorGetDefault();
                int result = IORegistryEntryCreateCFProperties(segment(), propsSeg, allocator, 0);
                if (result != 0) {
                    return MemorySegment.NULL;
                }
                return propsSeg.get(ADDRESS, 0);
            } catch (Throwable e) {
                return MemorySegment.NULL;
            }
        }

        public MemorySegment searchCFProperty(String plane, MemorySegment key, int options) {
            if (isNull()) {
                return MemorySegment.NULL;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment planeStr = arena.allocateFrom(plane);
                MemorySegment allocator = CFAllocatorGetDefault();
                return IORegistryEntrySearchCFProperty(segment(), planeStr, key, allocator, options);
            } catch (Throwable e) {
                return MemorySegment.NULL;
            }
        }

        // Property accessor methods
        public String getStringProperty(String key) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment cfKey = createCFString(key, arena);
                MemorySegment cfValue = null;
                try {
                    cfValue = createCFProperty(cfKey);
                    if (cfValue.equals(MemorySegment.NULL)) {
                        return null;
                    }
                    return getStringFromCFString(cfValue, arena);
                } finally {
                    releaseCFObjects(cfKey, cfValue);
                }
            } catch (Throwable e) {
                return null;
            }
        }

        public Long getLongProperty(String key) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment cfKey = createCFString(key, arena);
                MemorySegment cfValue = null;
                try {
                    cfValue = createCFProperty(cfKey);
                    if (cfValue.equals(MemorySegment.NULL)) {
                        return null;
                    }
                    MemorySegment valuePtr = arena.allocate(ValueLayout.JAVA_LONG);
                    if (CFNumberGetValue(cfValue, kCFNumberSInt64Type, valuePtr)) {
                        return valuePtr.get(ValueLayout.JAVA_LONG, 0);
                    }
                    return null;
                } finally {
                    releaseCFObjects(cfKey, cfValue);
                }
            } catch (Throwable e) {
                return null;
            }
        }

        public Integer getIntegerProperty(String key) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment cfKey = createCFString(key, arena);
                MemorySegment cfValue = null;
                try {
                    cfValue = createCFProperty(cfKey);
                    if (cfValue.equals(MemorySegment.NULL)) {
                        return null;
                    }
                    MemorySegment valuePtr = arena.allocate(ValueLayout.JAVA_INT);
                    if (CFNumberGetValue(cfValue, kCFNumberSInt32Type, valuePtr)) {
                        return valuePtr.get(ValueLayout.JAVA_INT, 0);
                    }
                    return null;
                } finally {
                    releaseCFObjects(cfKey, cfValue);
                }
            } catch (Throwable e) {
                return null;
            }
        }

        public Double getDoubleProperty(String key) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment cfKey = createCFString(key, arena);
                MemorySegment cfValue = null;
                try {
                    cfValue = createCFProperty(cfKey);
                    if (cfValue.equals(MemorySegment.NULL)) {
                        return null;
                    }
                    MemorySegment valuePtr = arena.allocate(ValueLayout.JAVA_DOUBLE);
                    if (CFNumberGetValue(cfValue, kCFNumberFloat64Type, valuePtr)) {
                        return valuePtr.get(ValueLayout.JAVA_DOUBLE, 0);
                    }
                    return null;
                } finally {
                    releaseCFObjects(cfKey, cfValue);
                }
            } catch (Throwable e) {
                return null;
            }
        }

        public byte[] getByteArrayProperty(String key) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment cfKey = createCFString(key, arena);
                MemorySegment cfValue = null;
                try {
                    cfValue = createCFProperty(cfKey);
                    if (cfValue.equals(MemorySegment.NULL)) {
                        return null;
                    }
                    long length = CFDataGetLength(cfValue);
                    MemorySegment bytePtr = CFDataGetBytePtr(cfValue);
                    return getByteArrayFromNativePointer(bytePtr, length, arena);
                } finally {
                    releaseCFObjects(cfKey, cfValue);
                }
            } catch (Throwable e) {
                return null;
            }
        }

        private static MemorySegment createCFString(String str, Arena arena) throws Throwable {
            char[] chars = str.toCharArray();
            MemorySegment charsSeg = arena.allocateFrom(ValueLayout.JAVA_CHAR, chars);

            MemorySegment allocator = CFAllocatorGetDefault();
            return CFStringCreateWithCharacters(allocator, charsSeg, chars.length);
        }

        private static String getStringFromCFString(MemorySegment cfString, Arena arena) throws Throwable {
            if (cfString.equals(MemorySegment.NULL)) {
                return null;
            }

            // Get length and allocate buffer
            long length = CFStringGetLength(cfString);
            long bufSize = length * 4 + 1; // UTF-8 can be up to 4 bytes per char + null terminator
            MemorySegment buffer = arena.allocate(bufSize);

            boolean success = CFStringGetCString(cfString, buffer, bufSize, kCFStringEncodingUTF8);
            if (!success) {
                return null;
            }

            return buffer.getString(0);
        }
    }

    class IOService extends IORegistryEntry {
        public IOService(MemorySegment segment) {
            super(segment);
        }
    }

    private static void releaseCFObjects(MemorySegment... cfObjects) {
        for (MemorySegment segment : cfObjects) {
            if (segment != null && !segment.equals(MemorySegment.NULL)) {
                try {
                    CoreFoundationFunctions.CFRelease(segment);
                } catch (Throwable e) {
                    // Ignore
                }
            }
        }
    }
}
