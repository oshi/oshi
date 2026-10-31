/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore.StoreKind;
import oshi.hardware.common.AbstractHWDiskStore;

/**
 * Base class for macOS HWDiskStore implementations. Subclasses provide platform-specific disk enumeration and
 * statistics updates.
 */
@ThreadSafe
public abstract class MacHWDiskStore extends AbstractHWDiskStore {

    /** IORegistry property naming a media object's BSD device. */
    protected static final String BSD_NAME = "BSD Name";
    /** IORegistry property marking a media object as a whole disk rather than a partition. */
    protected static final String WHOLE = "Whole";
    /** IORegistry property on a disk image device naming the image it was attached from. */
    protected static final String DISK_IMAGE_URL = "DiskImageURL";

    private static final String VIRTUAL = "Virtual";

    private final Optional<List<String>> backingStores;

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
        this.backingStores = Optional.empty();
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
        this.backingStores = Optional.empty();
    }

    /**
     * Creates a MacHWDiskStore built on other storage. A disk with at least one backing store, such as an APFS
     * container or a disk image, is typed {@code Virtual} whatever its media reports.
     *
     * @param name          the disk name
     * @param model         the disk model
     * @param serial        the serial number
     * @param size          the disk size in bytes
     * @param diskType      the disk type its media reports
     * @param backingStores the stores this disk is built on, or empty if they could not be read
     */
    protected MacHWDiskStore(String name, String model, String serial, long size, String diskType,
            Optional<List<String>> backingStores) {
        super(name, model, serial, size, backingStores.map(List::isEmpty).orElse(true) ? diskType : VIRTUAL);
        this.backingStores = backingStores;
    }

    @Override
    public Optional<List<String>> getBackingStores() {
        return backingStores;
    }

    /**
     * Builds the backing store of a disk whose nearest IOMedia ancestor in the IOService plane is another media object:
     * the partition an APFS container is synthesized on, or the disk it is carved from.
     *
     * @param bsdName the ancestor's {@code BSD Name}, or {@code null} if it has none
     * @param whole   the ancestor's {@code Whole} property, or {@code null} if absent
     * @return the backing store, or empty if the ancestor has no BSD name
     */
    protected static Optional<List<String>> mediaBackingStore(@Nullable String bsdName, @Nullable Boolean whole) {
        if (bsdName == null) {
            return Optional.empty();
        }
        StoreKind kind = Boolean.TRUE.equals(whole) ? StoreKind.DISK : StoreKind.PARTITION;
        return Optional.of(Collections.singletonList(kind.entry(bsdName)));
    }

    /**
     * Builds the backing store of a disk image: the file store holding the image file.
     *
     * @param imageUrl the image device's {@code DiskImageURL}
     * @param mountOf  returns the mount point of the file store holding a path, or an empty string if it cannot be read
     * @return the backing store, or empty if the image is not a local file or its file store could not be read
     */
    protected static Optional<List<String>> imageBackingStore(String imageUrl, Function<String, String> mountOf) {
        String path = imagePath(imageUrl);
        if (path.isEmpty()) {
            return Optional.empty();
        }
        String mount = mountOf.apply(path);
        if (mount.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.singletonList(StoreKind.FILE.entry(mount)));
    }

    /**
     * Converts a disk image URL to the path of the image file. Package-private for testing.
     *
     * @param imageUrl the image device's {@code DiskImageURL}, such as {@code file:///Users/x/a%20b.dmg}
     * @return the decoded file path, or an empty string if the URL is not a {@code file} URL
     */
    static String imagePath(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            return "file".equals(uri.getScheme()) && uri.getPath() != null ? uri.getPath() : "";
        } catch (URISyntaxException e) {
            return "";
        }
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
