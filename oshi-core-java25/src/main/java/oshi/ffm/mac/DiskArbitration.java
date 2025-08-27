/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static oshi.ffm.mac.DiskArbitrationFunctions.DADiskCopyDescription;
import static oshi.ffm.mac.DiskArbitrationFunctions.DADiskCreateFromBSDName;
import static oshi.ffm.mac.DiskArbitrationFunctions.DADiskCreateFromIOMedia;
import static oshi.ffm.mac.DiskArbitrationFunctions.DADiskGetBSDName;
import static oshi.ffm.mac.DiskArbitrationFunctions.DASessionCreate;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import oshi.ffm.mac.CoreFoundation.CFAllocatorRef;
import oshi.ffm.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFTypeRef;

/**
 * Disk Arbitration is a low-level framework based on Core Foundation. The Disk Arbitration framework provides the
 * ability to get various pieces of information about a volume.
 */
public interface DiskArbitration {

    /**
     * Type of a reference to DASession instances.
     */
    class DASessionRef extends CFTypeRef {
        public DASessionRef(MemorySegment segment) {
            super(segment);
        }

        /**
         * Creates a new session.
         *
         * @param allocator The allocator to use, or null for default
         * @return A reference to a new DASession
         */
        public static DASessionRef create(CFAllocatorRef allocator) {
            MemorySegment allocSeg = allocator != null ? allocator.segment() : MemorySegment.NULL;
            MemorySegment sessionSeg = DASessionCreate(allocSeg);
            return new DASessionRef(sessionSeg);
        }
    }

    /**
     * Type of a reference to DADisk instances.
     */
    class DADiskRef extends CFTypeRef {
        public DADiskRef(MemorySegment segment) {
            super(segment);
        }

        /**
         * Creates a new disk object from a BSD device name.
         *
         * @param allocator The allocator to use, or null for default
         * @param session   The DASession in which to contact Disk Arbitration
         * @param bsdName   The BSD device name
         * @return A reference to a new DADisk
         */
        public static DADiskRef createFromBSDName(CFAllocatorRef allocator, DASessionRef session, String bsdName) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment allocSeg = allocator != null ? allocator.segment() : MemorySegment.NULL;
                MemorySegment sessionSeg = session != null ? session.segment() : MemorySegment.NULL;
                MemorySegment bsdNameSeg = arena.allocateFrom(bsdName);

                MemorySegment diskSeg = DADiskCreateFromBSDName(allocSeg, sessionSeg, bsdNameSeg);
                return new DADiskRef(diskSeg);
            }
        }

        /**
         * Creates a new disk object from an IOMedia object.
         *
         * @param allocator The allocator to use, or null for default
         * @param session   The DASession in which to contact Disk Arbitration
         * @param media     The IOKit.IOObject media object
         * @return A reference to a new DADisk
         */
        public static DADiskRef createFromIOMedia(CFAllocatorRef allocator, DASessionRef session,
                IOKit.IOObject media) {
            MemorySegment allocSeg = allocator != null ? allocator.segment() : MemorySegment.NULL;
            MemorySegment sessionSeg = session != null ? session.segment() : MemorySegment.NULL;
            MemorySegment mediaSeg = media != null ? media.segment() : MemorySegment.NULL;

            MemorySegment diskSeg = DADiskCreateFromIOMedia(allocSeg, sessionSeg, mediaSeg);
            return new DADiskRef(diskSeg);
        }

        /**
         * Obtains the Disk Arbitration description of the specified disk.
         *
         * @return The disk's Disk Arbitration description
         */
        public CFDictionaryRef copyDescription() {
            if (isNull()) {
                return new CFDictionaryRef(MemorySegment.NULL);
            }
            MemorySegment dictSeg = DADiskCopyDescription(segment());
            return new CFDictionaryRef(dictSeg);
        }

        /**
         * Obtains the BSD device name for the specified disk.
         *
         * @return The disk's BSD device name
         */
        public String getBSDName() {
            if (isNull()) {
                return null;
            }
            MemorySegment nameSeg = DADiskGetBSDName(segment());
            if (nameSeg.equals(MemorySegment.NULL)) {
                return null;
            }
            return nameSeg.getString(0);
        }
    }
}
