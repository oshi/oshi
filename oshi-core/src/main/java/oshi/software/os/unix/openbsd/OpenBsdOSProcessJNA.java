/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.openbsd.OpenBsdOSProcess;
import oshi.util.common.platform.unix.openbsd.FstatUtil;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class OpenBsdOSProcessJNA extends OpenBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdOSProcessJNA.class);

    private static final int ARGMAX;

    static {
        int[] mib = new int[2];
        mib[0] = 1; // CTL_KERN
        mib[1] = 8; // KERN_ARGMAX
        try (Memory m = new Memory(Integer.BYTES);
                CloseableSizeTByReference size = new CloseableSizeTByReference(Integer.BYTES)) {
            if (OpenBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, size_t.ZERO) == 0) {
                ARGMAX = m.getInt(0);
            } else {
                LOG.warn("Failed sysctl call for process arguments max size (kern.argmax). Error code: {}",
                        Native.getLastError());
                ARGMAX = 0;
            }
        }
    }

    private final OpenBsdOperatingSystemJNA os;

    public OpenBsdOSProcessJNA(int pid, Map<BsdPsKeyword, String> psMap, OpenBsdOperatingSystemJNA os) {
        super(pid);
        this.os = os;
        updateAttributes(psMap);
    }

    @Override
    protected int queryBitness() {
        // OpenBSD does not maintain a compatibility layer.
        // Process bitness is OS bitness
        return Native.LONG_SIZE * 8;
    }

    @Override
    protected List<String> queryArguments() {
        if (ARGMAX > 0) {
            // Get arguments via sysctl(3)
            int[] mib = new int[4];
            mib[0] = 1; // CTL_KERN
            mib[1] = 55; // KERN_PROC_ARGS
            mib[2] = getProcessID();
            mib[3] = 1; // KERN_PROC_ARGV
            // Allocate memory for arguments
            try (Memory m = new Memory(ARGMAX);
                    CloseableSizeTByReference size = new CloseableSizeTByReference(ARGMAX)) {
                // Fetch arguments
                if (OpenBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, size_t.ZERO) == 0) {
                    // Returns a null-terminated list of pointers to the actual data
                    List<String> args = new ArrayList<>();
                    // To iterate the pointer-list
                    long offset = 0;
                    // Get the data base address to calculate offsets
                    long baseAddr = Pointer.nativeValue(m);
                    long maxAddr = baseAddr + size.getValue().longValue();
                    // Get the address of the data. If null (0) we're done iterating
                    long argAddr = Pointer.nativeValue(m.getPointer(offset));
                    while (argAddr > baseAddr && argAddr < maxAddr) {
                        args.add(m.getString(argAddr - baseAddr));
                        offset += Native.POINTER_SIZE;
                        argAddr = Pointer.nativeValue(m.getPointer(offset));
                    }
                    return Collections.unmodifiableList(args);
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        // Get environment variables via sysctl(3)
        int[] mib = new int[4];
        mib[0] = 1; // CTL_KERN
        mib[1] = 55; // KERN_PROC_ARGS
        mib[2] = getProcessID();
        mib[3] = 3; // KERN_PROC_ENV
        // Allocate memory for environment variables
        try (Memory m = new Memory(ARGMAX); CloseableSizeTByReference size = new CloseableSizeTByReference(ARGMAX)) {
            // Fetch environment variables
            if (OpenBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, size_t.ZERO) == 0) {
                // Returns a null-terminated list of pointers to the actual data
                Map<String, String> env = new LinkedHashMap<>();
                // To iterate the pointer-list
                long offset = 0;
                // Get the data base address to calculate offsets
                long baseAddr = Pointer.nativeValue(m);
                long maxAddr = baseAddr + size.longValue();
                // Get the address of the data. If null (0) we're done iterating
                long argAddr = Pointer.nativeValue(m.getPointer(offset));
                while (argAddr > baseAddr && argAddr < maxAddr) {
                    String envStr = m.getString(argAddr - baseAddr);
                    int idx = envStr.indexOf('=');
                    if (idx > 0) {
                        env.put(envStr.substring(0, idx), envStr.substring(idx + 1));
                    }
                    offset += Native.POINTER_SIZE;
                    argAddr = Pointer.nativeValue(m.getPointer(offset));
                }
                return Collections.unmodifiableMap(env);
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return FstatUtil.getCwd(getProcessID());
    }

    @Override
    public long getOpenFiles() {
        return FstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            OpenBsdLibc.INSTANCE.getrlimit(OpenBsdLibc.RLIMIT_NOFILE, rlimit);
            return rlimit.rlim_cur;
        } else {
            return -1L; // not supported
        }
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            final Resource.Rlimit rlimit = new Resource.Rlimit();
            OpenBsdLibc.INSTANCE.getrlimit(OpenBsdLibc.RLIMIT_NOFILE, rlimit);
            return rlimit.rlim_max;
        } else {
            return -1L; // not supported
        }
    }

}
