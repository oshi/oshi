/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.freebsd.FreeBsdOSProcess;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * JNA-backed FreeBSD OSProcess.
 */
@ThreadSafe
public class FreeBsdOSProcessJNA extends FreeBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdOSProcessJNA.class);

    private static final int ARGMAX = BsdSysctlUtil.sysctl("kern.argmax", 0);

    private final FreeBsdOperatingSystemJNA os;

    public FreeBsdOSProcessJNA(int pid, Map<BsdPsKeyword, String> psMap, FreeBsdOperatingSystemJNA os) {
        super(pid);
        this.os = os;
        updateAttributes(psMap);
    }

    @Override
    protected List<String> queryArguments() {
        if (ARGMAX > 0) {
            // Get arguments via sysctl(3)
            int[] mib = new int[4];
            mib[0] = 1; // CTL_KERN
            mib[1] = 14; // KERN_PROC
            mib[2] = 7; // KERN_PROC_ARGS
            mib[3] = getProcessID();
            // Allocate memory for arguments
            try (Memory m = new Memory(ARGMAX);
                    CloseableSizeTByReference size = new CloseableSizeTByReference(ARGMAX)) {
                // Fetch arguments
                if (FreeBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, size_t.ZERO) == 0) {
                    return Collections.unmodifiableList(
                            ParseUtil.parseByteArrayToStrings(m.getByteArray(0, size.getValue().intValue())));
                } else {
                    LOG.warn(
                            "Failed sysctl call for process arguments (kern.proc.args), process {} may not exist. Error code: {}",
                            getProcessID(), Native.getLastError());
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        if (ARGMAX > 0) {
            // Get environment variables via sysctl(3)
            int[] mib = new int[4];
            mib[0] = 1; // CTL_KERN
            mib[1] = 14; // KERN_PROC
            mib[2] = 35; // KERN_PROC_ENV
            mib[3] = getProcessID();
            // Allocate memory for environment variables
            try (Memory m = new Memory(ARGMAX);
                    CloseableSizeTByReference size = new CloseableSizeTByReference(ARGMAX)) {
                // Fetch environment variables
                if (FreeBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, size_t.ZERO) == 0) {
                    return Collections.unmodifiableMap(
                            ParseUtil.parseByteArrayToStringMap(m.getByteArray(0, size.getValue().intValue())));
                } else {
                    LOG.warn(
                            "Failed sysctl call for process environment variables (kern.proc.env), process {} may not exist. Error code: {}",
                            getProcessID(), Native.getLastError());
                }
            }
        }
        return Collections.emptyMap();
    }

    @Override
    protected int queryOwnProcessId() {
        return this.os.getProcessId();
    }

    @Override
    protected long queryRlimitNofile(boolean soft) {
        Resource.Rlimit rlimit = new Resource.Rlimit();
        FreeBsdLibc.INSTANCE.getrlimit(FreeBsdLibc.RLIMIT_NOFILE, rlimit);
        return soft ? rlimit.rlim_cur : rlimit.rlim_max;
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
            if (0 == FreeBsdLibc.INSTANCE.sysctl(mib, mib.length, abi, size, null, size_t.ZERO)) {
                return elfBitness(abi.getString(0));
            }
        }
        return 0;
    }
}
