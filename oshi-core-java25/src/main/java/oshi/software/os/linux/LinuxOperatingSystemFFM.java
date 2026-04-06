/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.linux.LinuxLibcFunctions;
import oshi.ffm.linux.UdevFunctions;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSProcess.State;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

/**
 * FFM-based Linux operating system implementation.
 * <p>
 * Extends {@link LinuxOperatingSystem}, overriding methods to use FFM implementations as they become available.
 * <p>
 * Udev availability is determined by {@link UdevFunctions#isAvailable()}, which checks both the
 * {@code oshi.os.linux.allowudev} configuration property and whether libudev could be loaded and all symbols bound. FFM
 * consumer classes should import {@link #HAS_UDEV} from this class rather than from {@link LinuxOperatingSystemJNA}.
 */
@ThreadSafe
public class LinuxOperatingSystemFFM extends LinuxOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOperatingSystemFFM.class);

    /**
     * Identifies if the udev library was successfully loaded and all symbols bound via FFM. Also respects the
     * {@code oshi.os.linux.allowudev} configuration property, consistent with the JNA-based
     * {@link LinuxOperatingSystemJNA#HAS_UDEV}.
     */
    public static final boolean HAS_UDEV = UdevFunctions.isAvailable();

    private static final boolean HAS_SYSCALL_GETTID;

    static {
        boolean hasSyscallGettid = LinuxLibcFunctions.hasGettid();
        if (!hasSyscallGettid) {
            try {
                hasSyscallGettid = LinuxLibcFunctions.syscallGettid() > 0;
            } catch (Throwable e) {
                LOG.debug("Did not find working syscall gettid via FFM. Using procfs");
            }
        }
        HAS_SYSCALL_GETTID = hasSyscallGettid;
    }

    @Override
    public FileSystem getFileSystem() {
        return new LinuxFileSystemFFM();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new LinuxNetworkParamsFFM();
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
        return new LinuxOSProcessFFM(pid, this);
    }

    @Override
    public int getProcessId() {
        try {
            return LinuxLibcFunctions.getpid();
        } catch (Throwable e) {
            LOG.warn("FFM getpid failed: {}", e.toString());
            return 0;
        }
    }

    @Override
    public int getThreadId() {
        if (HAS_SYSCALL_GETTID) {
            try {
                return LinuxLibcFunctions.hasGettid() ? LinuxLibcFunctions.gettid()
                        : (int) LinuxLibcFunctions.syscallGettid();
            } catch (Throwable e) {
                LOG.warn("FFM gettid failed: {}", e.toString());
            }
        }
        try {
            return ParseUtil.parseIntOrDefault(
                    Files.readSymbolicLink(new File(ProcPath.THREAD_SELF).toPath()).getFileName().toString(), 0);
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public int getThreadCount() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(LinuxLibcFunctions.SYSINFO_LAYOUT);
            if (0 == LinuxLibcFunctions.sysinfo(info)) {
                return LinuxLibcFunctions.sysinfoProcs(info);
            }
            LOG.error("FFM sysinfo failed");
        } catch (Throwable e) {
            LOG.error("FFM sysinfo error: {}", e.toString());
        }
        return 0;
    }
}
