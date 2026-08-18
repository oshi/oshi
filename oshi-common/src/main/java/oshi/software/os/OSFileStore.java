/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;

/**
 * A FileStore represents a storage pool, device, partition, volume, concrete file system or other implementation
 * specific means of file storage. This object carries the same interpretation as core Java's
 * {@link java.nio.file.FileStore} class, with additional information.
 * <p>
 * File stores are obtained from the {@link FileSystem#getFileStores()} method. The {@link #getMount()} value can be
 * correlated with {@link HWPartition#getMountPoint()} to link file stores to their underlying hardware partitions and
 * disk stores.
 *
 * @see HWDiskStore
 * @see HWPartition
 */
@PublicApi
@ThreadSafe
public interface OSFileStore {

    /**
     * Name of the File System. A human-readable label that does not necessarily correspond to a file system path.
     *
     * @return The file system name
     */
    String getName();

    /**
     * Volume name of the File System. Generally a path representing the device (e.g., {@code /dev/foo} which is being
     * mounted.
     *
     * @return The volume name of the file system
     */
    String getVolume();

    /**
     * Label of the File System. An optional replacement for the name on Windows and Linux.
     *
     * @return The volume label of the file system. Only relevant on Windows and on Linux, if assigned; otherwise
     *         defaults to the FileSystem name. On other operating systems is redundant with the name.
     */
    String getLabel();

    /**
     * Logical volume of the File System.
     *
     * Provides an optional alternative volume identifier for the file system. Only supported on Linux, provides symlink
     * value via '/dev/mapper/' (used with LVM file systems).
     *
     * @return The logical volume of the file system
     */
    String getLogicalVolume();

    /**
     * Mount point of the File System. The directory users will normally use to interface with the file store.
     *
     * @return The mountpoint of the file system
     */
    String getMount();

    /**
     * Description of the File System.
     *
     * @return The file system description
     */
    String getDescription();

    /**
     * Type of the File System (FAT, NTFS, etx2, ext4, etc.)
     *
     * @return The file system type
     */
    String getType();

    /**
     * Filesystem options.
     *
     * @return A comma-deimited string of options
     */
    String getOptions();

    /**
     * UUID/GUID of the File System.
     *
     * @return The file system UUID/GUID
     */
    String getUUID();

    /**
     * Indicates whether this file store is local, providing low-latency access.
     * <p>
     * Local file stores are directly attached storage that return immediate results for file operations. This excludes
     * network-mounted file systems (e.g., NFS, CIFS/SMB, AFP) which may have higher latency or require network
     * round-trips.
     * <p>
     * On macOS, this corresponds to the {@code MNT_LOCAL} mount flag. On other platforms, this returns {@code false}
     * for file systems with network types such as {@code nfs}, {@code cifs}, {@code smbfs}, and {@code afs}.
     *
     * @return {@code true} if the file store is locally attached (not a network drive); {@code false} otherwise
     */
    boolean isLocal();

    /**
     * Free space on the drive. This space is unallocated but may require elevated permissions to write.
     * <p>
     * The difference between this value and {@link #getUsableSpace()} is space the operating system has reserved: on
     * many UNIX filesystems a percentage held back for the superuser, on Windows the caller's disk quota. It is zero on
     * filesystems that reserve nothing at this layer, such as ZFS and APFS.
     *
     * @return Free space on the drive (in bytes)
     * @see #getTotalSpace()
     */
    long getFreeSpace();

    /**
     * Usable space on the drive. This is space available to unprivileged users.
     *
     * @return Usable space on the drive (in bytes)
     * @see #getTotalSpace()
     */
    long getUsableSpace();

    /**
     * Total space/capacity of the drive.
     * <p>
     * This value and those from {@link #getFreeSpace()} and {@link #getUsableSpace()} always satisfy
     * {@code 0 <= usable <= free <= total}. Most platforms obtain the three from separate queries, so on a filesystem
     * whose capacity is computed rather than fixed — a ZFS dataset drawing on its pool, or a swap-backed {@code tmpfs}
     * — they are readings from adjacent instants rather than one coherent snapshot, and may contradict each other.
     * Where that happens the values are clamped downward to restore the ordering; none is ever reported higher than the
     * operating system reported it.
     *
     * @return Total capacity of the drive (in bytes)
     */
    long getTotalSpace();

    /**
     * Usable / free inodes on the drive. Not applicable on Windows.
     *
     * @return Usable / free inodes on the drive (count), or -1 if unimplemented
     */
    long getFreeInodes();

    /**
     * Total / maximum number of inodes of the filesystem. Not applicable on Windows.
     *
     * @return Total / maximum number of inodes of the filesystem (count), or -1 if unimplemented
     */
    long getTotalInodes();

    /**
     * Make a best effort to update all the statistics about the file store without needing to recreate the file store
     * list. This method provides for more frequent periodic updates of file store statistics.
     *
     * @return True if the update was (probably) successful, false if the disk was not found
     */
    boolean updateAttributes();
}
