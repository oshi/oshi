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
package oshi;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.Platform; // NOSONAR squid:S1191

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.hardware.platform.mac.MacHardwareAbstractionLayer;
import oshi.hardware.platform.unix.aix.AixHardwareAbstractionLayer;
import oshi.hardware.platform.unix.freebsd.FreeBsdHardwareAbstractionLayer;
import oshi.hardware.platform.unix.solaris.SolarisHardwareAbstractionLayer;
import oshi.hardware.platform.windows.WindowsHardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxFileSystem;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.software.os.mac.MacOperatingSystem;
import oshi.software.os.unix.aix.AixFileSystem;
import oshi.software.os.unix.aix.AixOperatingSystem;
import oshi.software.os.unix.freebsd.FreeBsdFileSystem;
import oshi.software.os.unix.freebsd.FreeBsdOperatingSystem;
import oshi.software.os.unix.solaris.SolarisFileSystem;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;
import oshi.software.os.windows.WindowsOperatingSystem;
import oshi.util.GlobalConfig;

import java.util.Arrays;
import java.util.List;

/**
 * System information. This is the main entry point to Oshi.
 * <p>
 * This object provides getters which instantiate the appropriate
 * platform-specific implementations of {@link oshi.software.os.OperatingSystem}
 * (software) and {@link oshi.hardware.HardwareAbstractionLayer} (hardware).
 */
public class SystemInfo {

    // The platform isn't going to change, and making this static enables easy
    // access from outside this class
    private static final PlatformEnum currentPlatformEnum;

    static {
        if (Platform.isWindows()) {
            currentPlatformEnum = PlatformEnum.WINDOWS;
        } else if (Platform.isLinux()) {
            currentPlatformEnum = PlatformEnum.LINUX;
        } else if (Platform.isMac()) {
            currentPlatformEnum = PlatformEnum.MACOSX;
        } else if (Platform.isSolaris()) {
            currentPlatformEnum = PlatformEnum.SOLARIS;
        } else if (Platform.isFreeBSD()) {
            currentPlatformEnum = PlatformEnum.FREEBSD;
        } else if (Platform.isAIX()) {
            currentPlatformEnum = PlatformEnum.AIX;
        } else {
            currentPlatformEnum = PlatformEnum.UNKNOWN;
        }
    }

    private final Supplier<OperatingSystem> os = memoize(this::createOperatingSystem);

    private final Supplier<HardwareAbstractionLayer> hardware = memoize(this::createHardware);

    /**
     * <p>
     * Getter for the field <code>currentPlatformEnum</code>.
     * </p>
     *
     * @return Returns the currentPlatformEnum.
     */
    public static PlatformEnum getCurrentPlatformEnum() {
        return currentPlatformEnum;
    }

    /**
     * Creates a new instance of the appropriate platform-specific
     * {@link oshi.software.os.OperatingSystem}.
     *
     * @return A new instance of {@link oshi.software.os.OperatingSystem}.
     */
    public OperatingSystem getOperatingSystem() {
        return os.get();
    }

    private OperatingSystem createOperatingSystem() {
        switch (currentPlatformEnum) {

        case WINDOWS:
            initializeSuppressedConfig(null);
            return new WindowsOperatingSystem();
        case LINUX:
            initializeSuppressedConfig(LinuxFileSystem.TMP_FS_PATHS_DEFAULT);
            return new LinuxOperatingSystem();
        case MACOSX:
            initializeSuppressedConfig(null);
            return new MacOperatingSystem();
        case SOLARIS:
            initializeSuppressedConfig(SolarisFileSystem.TMP_FS_PATHS_DEFAULT);
            return new SolarisOperatingSystem();
        case FREEBSD:
            initializeSuppressedConfig(FreeBsdFileSystem.TMP_FS_PATHS_DEFAULT);
            return new FreeBsdOperatingSystem();
        case AIX:
            initializeSuppressedConfig(AixFileSystem.TMP_FS_PATHS_DEFAULT);
            return new AixOperatingSystem();
        default:
            throw new UnsupportedOperationException("Operating system not supported: " + Platform.getOSType());
        }
    }

    /**
     * Save config of suppressed paths/types if they didn't exist before
     *
     * @param TMP_FS_PATHS if exists for this OS.
     */
    static void initializeSuppressedConfig(List<String> TMP_FS_PATHS) {
        if (GlobalConfig.get("NETWORK_FS_TYPES", null) == null) {
            GlobalConfig.set("NETWORK_FS_TYPES", String.join(",", NETWORK_FS_TYPES_DEFAULT));
        }
        if (GlobalConfig.get("PSEUDO_FS_TYPES", null) == null) {
            GlobalConfig.set("PSEUDO_FS_TYPES", String.join(",", PSEUDO_FS_TYPES_DEFAULT));
        }
        if (TMP_FS_PATHS != null && GlobalConfig.get("TMP_FS_PATHS", null) == null) {
            GlobalConfig.set("TMP_FS_PATHS", String.join(",", TMP_FS_PATHS));
        }
    }

    /**
     * Creates a new instance of the appropriate platform-specific
     * {@link oshi.hardware.HardwareAbstractionLayer}.
     *
     * @return A new instance of {@link oshi.hardware.HardwareAbstractionLayer}.
     */
    public HardwareAbstractionLayer getHardware() {
        return hardware.get();
    }

    private HardwareAbstractionLayer createHardware() {
        switch (currentPlatformEnum) {

        case WINDOWS:
            return new WindowsHardwareAbstractionLayer();
        case LINUX:
            return new LinuxHardwareAbstractionLayer();
        case MACOSX:
            return new MacHardwareAbstractionLayer();
        case SOLARIS:
            return new SolarisHardwareAbstractionLayer();
        case FREEBSD:
            return new FreeBsdHardwareAbstractionLayer();
        case AIX:
            return new AixHardwareAbstractionLayer();
        default:
            throw new UnsupportedOperationException("Operating system not supported: " + Platform.getOSType());
        }
    }

    /**
     * FileSystem types which are network-based and should be excluded from
     * local-only lists
     */
    protected static final List<String> NETWORK_FS_TYPES_DEFAULT = Arrays.asList("afs", "cifs", "smbfs", "sshfs", "ncpfs",
        "ncp", "nfs", "nfs4", "gfs", "gds2", "glusterfs");

    protected static final List<String> PSEUDO_FS_TYPES_DEFAULT = Arrays.asList(//
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

}
