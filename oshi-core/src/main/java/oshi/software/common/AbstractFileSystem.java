/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.common;

import java.util.Arrays;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

@ThreadSafe
public abstract class AbstractFileSystem implements FileSystem {

    /**
     * FileSystem types which are network-based and should be excluded from
     * local-only lists
     */
    protected static final List<String> NETWORK_FS_TYPES = Arrays.asList("afs", "cifs", "smbfs", "sshfs", "ncpfs",
            "ncp", "nfs", "nfs4", "gfs", "gds2", "glusterfs");

    protected static final List<String> PSEUDO_FS_TYPES = Arrays.asList(//
            // Linux defines a set of virtual file systems
            "anon_inodefs", // anonymous inodes - inodes without filenames
            "autofs", // automounter file system, used by Linux, Solaris, FreeBSD
            "bdev", // keep track of block_device vs major/minor mapping
            "binfmt_misc", // Binary format support file system
            "bpf", // Virtual filesystem for Berkeley Paket Filter
            "cgroup", // Cgroup file system
            "cgroup2", // Cgroup file system
            "configfs", // Config file system
            "cpuset", // pseudo-filesystem interface to the kernel cpuset mechanism
            "dax", // Direct Access (DAX) can be used on memory-backed block devices
            "debugfs", // Debug file system
            "devpts", // Dev pseudo terminal devices file system
            "devtmpfs", // Dev temporary file system
            "drm", // Direct Rendering Manager
            "ecryptfs", // POSIX-compliant enterprise cryptographic filesystem for Linux
            "efivarfs", // (U)EFI variable filesystem
            "fuse", //
            // NOTE: FUSE's fuseblk is not evalued because used as file system
            // representation of a FUSE block storage
            // "fuseblk" // FUSE block file system
            "fusectl", // FUSE control file system
            "hugetlbfs", // Huge pages support file system
            "inotifyfs", // support inotify
            "mqueue", // Message queue file system
            "nfsd", // NFS file system
            "overlay", // Overlay file system https://wiki.archlinux.org/index.php/Overlay_filesystem
            // "pipefs", // for pipes but only visible inside kernel
            "proc", // Proc file system, used by Linux and Solaris
            "pstore", // Pstore file system
            // "ramfs", // Old filesystem used for RAM disks
            "rootfs", // Minimal fs to support kernel boot
            "rpc_pipefs", // Sun RPC file system
            "securityfs", // Kernel security file system
            "selinuxfs", // SELinux file system
            "sunrpc", // Sun RPC file system
            "sysfs", // SysFS file system
            "systemd-1", // Systemd file system
            // "tmpfs", // Temporary file system
            // NOTE: tmpfs is evaluated apart, because Linux, Solaris, FreeBSD use it for
            // RAMdisks
            "tracefs", // thin stackable file system for capturing file system traces
            "usbfs", // removed in linux 3.5 but still seen in some systems
            // FreeBSD / Solaris defines a set of virtual file systems
            "procfs", // Proc file system
            "devfs", // Dev temporary file system
            "ctfs", // Contract file system
            "fdescfs", // fd
            "objfs", // Object file system
            "mntfs", // Mount file system
            "sharefs", // Share file system
            "lofs" // Library file system
    );

    @Override
    public List<OSFileStore> getFileStores() {
        return getFileStores(false);
    }
}
