/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractHWDiskStore;

/**
 * Base class for macOS HWDiskStore implementations. Subclasses provide platform-specific disk enumeration and
 * statistics updates.
 */
@ThreadSafe
public abstract class MacHWDiskStore extends AbstractHWDiskStore {

    /**
     * Creates a MacHWDiskStore with unknown disk type.
     *
     * @param name   the disk name
     * @param model  the disk model
     * @param serial the serial number
     * @param size   the disk size in bytes
     */
    protected MacHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Creates a MacHWDiskStore.
     *
     * @param name     the disk name
     * @param model    the disk model
     * @param serial   the serial number
     * @param size     the disk size in bytes
     * @param diskType the disk type
     */
    protected MacHWDiskStore(String name, String model, String serial, long size, String diskType) {
        super(name, model, serial, size, diskType);
    }

    /**
     * Classifies a disk from the IOKit {@code Medium Type} device characteristic.
     *
     * @param mediumType the {@code Medium Type} value from the device's {@code Device Characteristics} dictionary, or
     *                   {@code null} if the property is absent
     * @return {@code "SSD"}, {@code "HDD"}, or {@code "Unknown"} if the medium type is absent or unrecognized
     */
    protected static String parseMediumType(@Nullable String mediumType) {
        if (mediumType != null) {
            if (mediumType.contains("Solid State") || mediumType.contains("SSD")) {
                return "SSD";
            } else if (mediumType.contains("Rotational")) {
                return "HDD";
            }
        }
        return "Unknown";
    }

    /**
     * Strings to convert to CFStringRef for pointer lookups.
     */
    protected enum CFKey {
        /** IOPropertyMatch key. */
        IO_PROPERTY_MATCH("IOPropertyMatch"),
        /** Statistics dictionary key. */
        STATISTICS("Statistics"),
        /** Read operations count. */
        READ_OPS("Operations (Read)"),
        /** Read bytes count. */
        READ_BYTES("Bytes (Read)"),
        /** Read time. */
        READ_TIME("Total Time (Read)"),
        /** Write operations count. */
        WRITE_OPS("Operations (Write)"),
        /** Write bytes count. */
        WRITE_BYTES("Bytes (Write)"),
        /** Write time. */
        WRITE_TIME("Total Time (Write)"),
        /** BSD unit number. */
        BSD_UNIT("BSD Unit"),
        /** Leaf node indicator. */
        LEAF("Leaf"),
        /** Whole disk indicator. */
        WHOLE("Whole"),
        /** DiskArbitration media name. */
        DA_MEDIA_NAME("DAMediaName"),
        /** DiskArbitration volume name. */
        DA_VOLUME_NAME("DAVolumeName"),
        /** DiskArbitration media size. */
        DA_MEDIA_SIZE("DAMediaSize"),
        /** DiskArbitration device model. */
        DA_DEVICE_MODEL("DADeviceModel"),
        /** IOKit model property. */
        MODEL("Model");

        private final String key;

        CFKey(String key) {
            this.key = key;
        }

        /**
         * Gets the CoreFoundation key string.
         *
         * @return the key string
         */
        public String getKey() {
            return this.key;
        }
    }
}
