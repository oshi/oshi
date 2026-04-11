/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.linux.LibC;
import com.sun.jna.platform.linux.Udev;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.WhoJNA;
import oshi.driver.linux.proc.AuxvJNA;
import oshi.jna.Struct.CloseableSysinfo;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.software.common.os.linux.LinuxOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSProcess.State;
import oshi.software.os.OSSession;
import oshi.util.ExecutingCommand;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.driver.linux.proc.Auxv;
import oshi.util.linux.ProcPath;

/**
 * JNA-based Linux operating system implementation. Extends {@link LinuxOperatingSystem}, providing JNA implementations
 * of process/thread ID queries and thread count via {@code sysinfo}.
 */
@ThreadSafe
public class LinuxOperatingSystemJNA extends LinuxOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOperatingSystemJNA.class);

    /** This static field identifies if the udev library can be loaded. */
    public static final boolean HAS_UDEV;
    private static final long USER_HZ;
    private static final long PAGE_SIZE;
    /** This static field identifies if the gettid function is in the c library. */
    public static final boolean HAS_GETTID;
    /** This static field identifies if the syscall for gettid returns sane results. */
    public static final boolean HAS_SYSCALL_GETTID;

    static {
        long userHz = 100L;
        long pageSz = 4096L;
        boolean hasUdev = false;
        boolean hasGettid = false;
        boolean hasSyscallGettid = false;
        try {
            Map<Integer, Long> auxv = AuxvJNA.queryAuxv();
            long hz = auxv.getOrDefault(Auxv.AT_CLKTCK, 0L);
            userHz = hz > 0 ? hz
                    : ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf CLK_TCK"), 100L);
            long pagesz = auxv.getOrDefault(Auxv.AT_PAGESZ, 0L);
            pageSz = pagesz > 0 ? pagesz
                    : ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf PAGE_SIZE"), 4096L);
            if (GlobalConfig.get(GlobalConfig.OSHI_OS_LINUX_ALLOWUDEV, true)) {
                try {
                    @SuppressWarnings("unused")
                    Udev lib = Udev.INSTANCE;
                    hasUdev = true;
                } catch (UnsatisfiedLinkError e) {
                    LOG.warn("Did not find udev library in operating system. Some features may not work.");
                }
            } else {
                LOG.info("Loading of udev not allowed by configuration. Some features may not work.");
            }

            try {
                LinuxLibc.INSTANCE.gettid();
                hasGettid = true;
            } catch (UnsatisfiedLinkError e) {
                LOG.debug("Did not find gettid function in operating system. Using fallbacks.");
            }

            hasSyscallGettid = hasGettid;
            if (!hasGettid) {
                try {
                    hasSyscallGettid = LinuxLibc.INSTANCE.syscall(LinuxLibc.SYS_GETTID).intValue() > 0;
                } catch (UnsatisfiedLinkError e) {
                    LOG.debug("Did not find working syscall gettid function in operating system. Using procfs");
                }
            }
        } catch (NoClassDefFoundError e) {
            LOG.error("Did not find JNA classes. Investigate incompatible version or missing native dll.");
        }
        HAS_UDEV = hasUdev;
        HAS_GETTID = hasGettid;
        HAS_SYSCALL_GETTID = hasSyscallGettid;
        USER_HZ = userHz;
        PAGE_SIZE = pageSz;
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? oshi.util.driver.linux.Who.queryWho() : WhoJNA.queryUtxent();
    }

    @Override
    public FileSystem getFileSystem() {
        return new LinuxFileSystemJNA();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new LinuxNetworkParamsJNA();
    }

    @Override
    public OSProcess getProcess(int pid) {
        OSProcess proc = createOSProcess(pid);
        if (!proc.getState().equals(State.INVALID)) {
            return proc;
        }
        return null;
    }

    @Override
    protected OSProcess createOSProcess(int pid) {
        return new LinuxOSProcessJNA(pid, this);
    }

    @Override
    public int getProcessId() {
        return LinuxLibc.INSTANCE.getpid();
    }

    @Override
    public int getThreadId() {
        if (HAS_SYSCALL_GETTID) {
            return HAS_GETTID ? LinuxLibc.INSTANCE.gettid()
                    : LinuxLibc.INSTANCE.syscall(LinuxLibc.SYS_GETTID).intValue();
        }
        try {
            return ParseUtil.parseIntOrDefault(
                    Files.readSymbolicLink(new File(ProcPath.THREAD_SELF).toPath()).getFileName().toString(), 0);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Gets Jiffies per second, useful for converting ticks to milliseconds and vice versa.
     *
     * @return Jiffies per second.
     */
    public static long hz() {
        return USER_HZ;
    }

    /**
     * Gets Page Size, for converting memory stats from pages to bytes.
     *
     * @return Page Size in bytes.
     */
    public static long pageSize() {
        return PAGE_SIZE;
    }

    @Override
    public long getHz() {
        return USER_HZ;
    }

    @Override
    public long getPageSize() {
        return PAGE_SIZE;
    }

    @Override
    public int getThreadCount() {
        try (CloseableSysinfo info = new CloseableSysinfo()) {
            if (0 != LibC.INSTANCE.sysinfo(info)) {
                LOG.error("Failed to get process thread count. Error code: {}", Native.getLastError());
                return 0;
            }
            return Short.toUnsignedInt(info.procs);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            LOG.error("Failed to get procs from sysinfo. {}", e.getMessage());
        }
        return 0;
    }

}
