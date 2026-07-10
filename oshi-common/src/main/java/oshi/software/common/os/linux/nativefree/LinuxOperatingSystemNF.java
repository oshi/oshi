/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux.nativefree;

import java.nio.file.Files;
import java.nio.file.Paths;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.linux.LinuxOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSProcess.State;
import oshi.util.ExceptionUtil;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.ProcPath;

/**
 * Native-free Linux operating system implementation. Extends {@link LinuxOperatingSystem}, providing implementations
 * that require no native access.
 */
@ThreadSafe
public class LinuxOperatingSystemNF extends LinuxOperatingSystem {

    private static final long USER_HZ;
    private static final long PAGE_SIZE;

    static {
        USER_HZ = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf CLK_TCK"), 100L);
        PAGE_SIZE = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf PAGE_SIZE"), 4096L);
    }

    /**
     * Gets Jiffies per second.
     *
     * @return Jiffies per second.
     */
    public static long hz() {
        return USER_HZ;
    }

    /**
     * Gets Page Size in bytes.
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
    public FileSystem getFileSystem() {
        return new LinuxFileSystemNF();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new LinuxNetworkParamsNF();
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
        return new LinuxOSProcessNF(pid, this);
    }

    @Override
    public int getProcessId() {
        // First field of /proc/self/stat is the PID
        String stat = FileUtil.getStringFromFile(ProcPath.SELF_STAT);
        int space = stat.indexOf(' ');
        if (space > 0) {
            return ParseUtil.parseIntOrDefault(stat.substring(0, space), 0);
        }
        return 0;
    }

    @Override
    public int getThreadId() {
        return ExceptionUtil
                .getIntOrDefault(
                        () -> ParseUtil.parseIntOrDefault(
                                Files.readSymbolicLink(Paths.get(ProcPath.THREAD_SELF)).getFileName().toString(), 0),
                        0);
    }

    @Override
    public int getThreadCount() {
        // /proc/loadavg format: "load1 load5 load15 running/total lastpid"
        String loadavg = FileUtil.getStringFromFile(ProcPath.LOADAVG);
        if (!loadavg.isEmpty()) {
            String[] split = loadavg.split("\\s+");
            if (split.length >= 4) {
                String[] runTotal = split[3].split("/");
                if (runTotal.length == 2) {
                    return ParseUtil.parseIntOrDefault(runTotal[1], 0);
                }
            }
        }
        return 0;
    }
}
