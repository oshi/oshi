/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.DragonFlyBsdLibc;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.dragonflybsd.DragonFlyBsdOSProcess;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.dragonflybsd.ProcstatUtil;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class DragonFlyBsdOSProcessJNA extends DragonFlyBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(DragonFlyBsdOSProcessJNA.class);

    private final DragonFlyBsdOperatingSystemJNA os;

    public DragonFlyBsdOSProcessJNA(int pid, Map<BsdPsKeyword, String> psMap, DragonFlyBsdOperatingSystemJNA os) {
        super(pid);
        this.os = os;
        updateAttributes(psMap);
    }

    @Override
    protected List<String> queryArguments() {
        // DragonFlyBSD provides command line via /proc filesystem
        byte[] cmdBytes = FileUtil.readAllBytes("/proc/" + getProcessID() + "/cmdline", false);
        if (cmdBytes != null && cmdBytes.length > 0) {
            return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(cmdBytes));
        }
        return Collections.emptyList();
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        // DragonFlyBSD's /proc does not expose environ for other processes.
        // For the current process, use Java's System.getenv().
        if (getProcessID() == DragonFlyBsdLibc.INSTANCE.getpid()) {
            return System.getenv();
        }
        return Collections.emptyMap();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return ProcstatUtil.getCwd(getProcessID());
    }

    @Override
    public long getOpenFiles() {
        return ProcstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            DragonFlyBsdLibc.INSTANCE.getrlimit(DragonFlyBsdLibc.RLIMIT_NOFILE, rlimit);
            return rlimit.rlim_cur;
        } else {
            return getProcessOpenFileLimit(getProcessID(), 1);
        }
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            DragonFlyBsdLibc.INSTANCE.getrlimit(DragonFlyBsdLibc.RLIMIT_NOFILE, rlimit);
            return rlimit.rlim_max;
        } else {
            return getProcessOpenFileLimit(getProcessID(), 2);
        }
    }

    @Override
    protected int queryBitness() {
        // Get process abi vector
        int[] mib = new int[4];
        mib[0] = 1; // CTL_KERN
        mib[1] = 14; // KERN_PROC
        mib[2] = 9; // KERN_PROC_SV_NAME
        mib[3] = getProcessID();
        // Allocate memory for arguments
        try (Memory abi = new Memory(32); CloseableSizeTByReference size = new CloseableSizeTByReference(32)) {
            // Fetch abi vector
            if (0 == DragonFlyBsdLibc.INSTANCE.sysctl(mib, mib.length, abi, size, null, size_t.ZERO)) {
                String elf = abi.getString(0);
                if (elf.contains("ELF32")) {
                    return 32;
                } else if (elf.contains("ELF64")) {
                    return 64;
                }
            }
        }
        return 0;
    }

}
