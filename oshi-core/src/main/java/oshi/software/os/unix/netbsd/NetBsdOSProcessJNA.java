/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.netbsd;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.NetBsdLibc;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.netbsd.NetBsdOSProcess;
import oshi.software.common.os.unix.netbsd.NetBsdOperatingSystem;
import oshi.util.platform.unix.netbsd.NetBsdSysctlUtil;

/**
 * An OSProcess for NetBSD that uses JNA native calls for the process open-file limits ({@code getrlimit}) and for
 * reading the environment of other processes ({@code kern.proc_args}) when the JNA native library is available, and
 * falls back to the command-line {@link NetBsdOSProcess} implementation otherwise.
 */
@ThreadSafe
public class NetBsdOSProcessJNA extends NetBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(NetBsdOSProcessJNA.class);

    // KERN_PROC_ARGS sysctl selectors from <sys/sysctl.h>
    private static final int KERN_PROC_ARGS = 48;
    private static final int KERN_PROC_ENV = 3;

    // Maximum size of process argument/environment data (kern.argmax); 0 if JNA or the call is unavailable.
    private static final int ARGMAX;
    static {
        int argmax = 0;
        if (NetBsdSysctlUtil.JNA_AVAILABLE) {
            int[] mib = { NetBsdLibc.CTL_KERN, NetBsdLibc.KERN_ARGMAX };
            try (Memory m = new Memory(Integer.BYTES);
                    CloseableSizeTByReference size = new CloseableSizeTByReference(Integer.BYTES)) {
                if (NetBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, size_t.ZERO) == 0) {
                    argmax = m.getInt(0);
                } else {
                    LOG.warn("Failed sysctl call for kern.argmax. Error code: {}", Native.getLastError());
                }
            }
        }
        ARGMAX = argmax;
    }

    public NetBsdOSProcessJNA(int pid, Map<BsdPsKeyword, String> psMap, NetBsdOperatingSystem os) {
        super(pid, psMap, os);
    }

    @Override
    public long getSoftOpenFileLimit() {
        // getrlimit only reports limits for the calling (current) process
        if (!NetBsdSysctlUtil.JNA_AVAILABLE || getProcessID() != this.os.getProcessId()) {
            return super.getSoftOpenFileLimit();
        }
        Resource.Rlimit rlimit = new Resource.Rlimit();
        NetBsdLibc.INSTANCE.getrlimit(NetBsdLibc.RLIMIT_NOFILE, rlimit);
        return rlimit.rlim_cur;
    }

    @Override
    public long getHardOpenFileLimit() {
        if (!NetBsdSysctlUtil.JNA_AVAILABLE || getProcessID() != this.os.getProcessId()) {
            return super.getHardOpenFileLimit();
        }
        Resource.Rlimit rlimit = new Resource.Rlimit();
        NetBsdLibc.INSTANCE.getrlimit(NetBsdLibc.RLIMIT_NOFILE, rlimit);
        return rlimit.rlim_max;
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        // For the current process, use Java's System.getenv()
        if (getProcessID() == this.os.getProcessId()) {
            return System.getenv();
        }
        // Other processes' environment is only accessible natively; fall back to the (empty) command-line result
        if (!NetBsdSysctlUtil.JNA_AVAILABLE || ARGMAX <= 0) {
            return super.queryEnvironmentVariables();
        }
        int[] mib = { NetBsdLibc.CTL_KERN, KERN_PROC_ARGS, getProcessID(), KERN_PROC_ENV };
        try (Memory m = new Memory(ARGMAX); CloseableSizeTByReference size = new CloseableSizeTByReference(ARGMAX)) {
            if (NetBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, size_t.ZERO) == 0) {
                Map<String, String> env = new LinkedHashMap<>();
                long bytesReturned = size.getValue().longValue();
                long offset = 0;
                while (offset < bytesReturned) {
                    String envStr = m.getString(offset);
                    if (envStr.isEmpty()) {
                        break;
                    }
                    int idx = envStr.indexOf('=');
                    if (idx > 0) {
                        env.put(envStr.substring(0, idx), envStr.substring(idx + 1));
                    }
                    offset += envStr.length() + 1;
                }
                if (!env.isEmpty()) {
                    return Collections.unmodifiableMap(env);
                }
            }
        }
        return super.queryEnvironmentVariables();
    }
}
