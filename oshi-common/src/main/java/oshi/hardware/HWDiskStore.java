/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import java.util.List;
import java.util.Optional;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSFileStore;

/**
 * A storage mechanism where data are recorded by various electronic, magnetic, optical, or mechanical changes to a
 * surface layer of one or more rotating disks or or flash storage such as a removable or solid state drive. In
 * constrast to a File System, defining the way an Operating system uses the storage, the Disk Store represents the
 * hardware which a FileSystem uses for its File Stores.
 * <p>
 * Each disk store contains a list of {@link HWPartition} objects. Partitions that are mounted have a
 * {@link HWPartition#getMountPoint()} value that can be correlated with {@link OSFileStore#getMount()} to link hardware
 * partitions to their corresponding file stores.
 * <p>
 * Example: finding the disk store and partition for a given file path:
 *
 * <pre>{@code
 * String filePath = "/home/user/data.txt";
 * for (HWDiskStore disk : hal.getDiskStores()) {
 *     for (HWPartition part : disk.getPartitions()) {
 *         if (!part.getMountPoint().isEmpty() && filePath.startsWith(part.getMountPoint())) {
 *             System.out.printf("File is on disk %s, partition %s%n", disk.getName(), part.getName());
 *         }
 *     }
 * }
 * }</pre>
 *
 * Thread safe for the designed use of retrieving the most recent data. Users should be aware that the
 * {@link #updateAttributes()} method may update attributes, including the time stamp, and should externally synchronize
 * such usage to ensure consistent calculations.
 *
 * @see HWPartition
 * @see OSFileStore
 */
@PublicApi
@ThreadSafe
public interface HWDiskStore {

    /**
     * The disk name
     *
     * @return the name
     */
    String getName();

    /**
     * The type of the disk's media.
     * <p>
     * Determined on Linux from the device's {@code removable} and {@code queue/rotational} sysfs attributes, on macOS
     * from the IOKit registry's {@code Removable} property and {@code Medium Type}, and on Windows from the storage
     * driver's removable-media flag, bus type and seek penalty, falling back to {@code MSFT_PhysicalDisk}'s
     * {@code MediaType} and {@code SpindleSpeed}. Other platforms always report {@code Unknown}.
     * <p>
     * {@code Virtual} is reported only for a Linux device-mapper device and for a Windows file-backed virtual or
     * Storage Spaces disk. A hypervisor's disk that the guest sees as an ordinary device is reported by its media
     * technology, not as {@code Virtual}. Whether a disk duplicates the capacity and I/O of another entry in the list
     * is reported by {@link #getBackingStores()}, not by this type.
     *
     * @return one of {@code SSD}, {@code HDD}, {@code Removable}, {@code Virtual}, or {@code Unknown} where the type
     *         could not be determined, which includes a disk whose driver does not report it
     */
    default String getDiskType() {
        return "Unknown";
    }

    /**
     * The disk model
     *
     * @return the model
     */
    String getModel();

    /**
     * The disk serial number, if available.
     *
     * @return the serial number
     */
    String getSerial();

    /**
     * The size of the disk
     *
     * @return the disk size, in bytes
     */
    long getSize();

    /**
     * The number of reads from the disk
     *
     * @return the reads
     */
    long getReads();

    /**
     * The number of bytes read from the disk
     *
     * @return the bytes read
     */
    long getReadBytes();

    /**
     * The number of writes to the disk
     *
     * @return the writes
     */
    long getWrites();

    /**
     * The number of bytes written to the disk
     *
     * @return the bytes written
     */
    long getWriteBytes();

    /**
     * The length of the disk queue (#I/O's in progress). Includes I/O requests that have been issued to the device
     * driver but have not yet completed. Not supported on macOS.
     *
     * @return the current disk queue length
     */
    long getCurrentQueueLength();

    /**
     * The time spent reading or writing, in milliseconds.
     *
     * @return the transfer time
     */
    long getTransferTime();

    /**
     * The partitions on this disk.
     *
     * @return an {@code UnmodifiableList} of the partitions on this drive.
     */
    List<HWPartition> getPartitions();

    /**
     * The stores this disk is built on, when it is a layer over other storage rather than a device in its own right.
     * <p>
     * A layered disk is listed alongside the stores it is built on, so its capacity and I/O statistics duplicate
     * theirs. This is the case for a device-mapper (LVM, LUKS, multipath) or md RAID device on Linux, an APFS container
     * or a disk image on macOS, and a Storage Spaces or file-backed virtual disk on Windows.
     * <p>
     * An empty {@link Optional} means the backing stores are not reported, either because the platform does not report
     * them or because they could not be read, and says nothing about whether the disk is layered. A present but empty
     * list means the disk is not built on anything OSHI can see. Summing the I/O of only the disks whose result is
     * present and empty therefore counts each transfer once, provided every disk's result is present.
     * <p>
     * Each entry is a {@link StoreKind} prefix followed by a name that matches a getter on the corresponding class:
     * <table>
     * <caption>Backing store entries</caption>
     * <tr>
     * <th>Kind</th>
     * <th>Name matches</th>
     * <th>Examples</th>
     * </tr>
     * <tr>
     * <td>{@link StoreKind#DISK}</td>
     * <td>{@link HWDiskStore#getName()}</td>
     * <td>{@code disk:/dev/sda}, {@code disk:\\.\PHYSICALDRIVE1}</td>
     * </tr>
     * <tr>
     * <td>{@link StoreKind#PARTITION}</td>
     * <td>{@link HWPartition#getIdentification()}</td>
     * <td>{@code partition:/dev/sda2}, {@code partition:disk0s2}</td>
     * </tr>
     * <tr>
     * <td>{@link StoreKind#FILE}</td>
     * <td>{@link OSFileStore#getMount()} of the file store holding the backing file</td>
     * <td>{@code file:/System/Volumes/Data}, {@code file:C:\}</td>
     * </tr>
     * </table>
     * An entry may name a store that is not in the current list, such as a network file store when only local file
     * stores were requested.
     *
     * @return An {@link Optional} containing the stores this disk is built on, sorted, which is an empty list for a
     *         disk that is not layered; or empty if the backing stores are not reported.
     */
    default Optional<List<String>> getBackingStores() {
        return Optional.empty();
    }

    /**
     * The time this disk's statistics were updated.
     *
     * @return the timeStamp, in milliseconds since the epoch.
     */
    long getTimeStamp();

    /**
     * Make a best effort to update all the statistics about the drive without needing to recreate the drive list. This
     * method provides for more frequent periodic updates of individual drive statistics but may be less efficient to
     * use if updating all drives. It will not detect if a removable drive has been removed and replaced by a different
     * drive in between method calls.
     *
     * @return True if the update was (probably) successful, false if the disk was not found
     */
    boolean updateAttributes();

    /**
     * The kind of store named by an entry in {@link #getBackingStores()}, each identified by a prefix on the entry.
     */
    @PublicApi
    enum StoreKind {
        /**
         * A disk store, named by {@link HWDiskStore#getName()}.
         */
        DISK("disk:"),
        /**
         * A partition, named by {@link HWPartition#getIdentification()}.
         */
        PARTITION("partition:"),
        /**
         * A file store, named by {@link OSFileStore#getMount()}.
         */
        FILE("file:");

        private final String prefix;

        StoreKind(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Gets the prefix that identifies this kind of entry, including its trailing colon.
         *
         * @return the prefix, such as {@code disk:}
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * Creates an entry of this kind.
         *
         * @param name the name of the store, as returned by the getter this kind matches
         * @return the entry, such as {@code disk:/dev/sda}
         */
        public String entry(String name) {
            return prefix + name;
        }

        /**
         * Tests whether an entry is of this kind.
         *
         * @param entry an entry from {@link #getBackingStores()}
         * @return true if the entry starts with this kind's prefix
         */
        public boolean matches(String entry) {
            return entry.startsWith(prefix);
        }

        /**
         * Gets the name of the store an entry of this kind refers to.
         *
         * @param entry an entry from {@link #getBackingStores()}
         * @return the name following the prefix, or an empty string if the entry is not of this kind
         */
        public String nameOf(String entry) {
            return matches(entry) ? entry.substring(prefix.length()) : "";
        }
    }
}
