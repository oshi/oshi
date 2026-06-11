/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.NATIVE_LONG_SIZE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CTL_KERN;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_ARGMAX;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_PROC_ARGS;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_PROC_ARGV;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_PROC_ENV;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.RLIMIT_NOFILE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.SIZE_T;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.util.common.platform.unix.openbsd.FstatUtil;

@ThreadSafe
public class OpenBsdOSProcessFFM extends oshi.software.common.os.unix.openbsd.OpenBsdOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdOSProcessFFM.class);

    private static final int ARGMAX;

    static {
        int[] mib = { CTL_KERN, KERN_ARGMAX };
        ARGMAX = OpenBsdSysctlUtilFFM.sysctl(mib, 0);
    }

    private final OpenBsdOperatingSystemFFM os;

    public OpenBsdOSProcessFFM(int pid, Map<BsdPsKeyword, String> psMap, OpenBsdOperatingSystemFFM os) {
        super(pid);
        this.os = os;
        updateAttributes(psMap);
    }

    @Override
    protected int queryBitness() {
        return (int) (NATIVE_LONG_SIZE * 8);
    }

    @Override
    protected List<String> queryArguments() {
        if (ARGMAX > 0) {
            int[] mib = { CTL_KERN, KERN_PROC_ARGS, getProcessID(), KERN_PROC_ARGV };
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment mibSeg = arena.allocate(JAVA_INT, mib.length);
                for (int i = 0; i < mib.length; i++) {
                    mibSeg.setAtIndex(JAVA_INT, i, mib[i]);
                }
                MemorySegment buf = arena.allocate(ARGMAX);
                MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, (long) ARGMAX);
                MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
                if (OpenBsdLibcFunctions.sysctl(callState, mibSeg, mib.length, buf, sizeSeg, MemorySegment.NULL,
                        0L) == 0) {
                    long actualSize = sizeSeg.get(SIZE_T, 0);
                    long baseAddr = buf.address();
                    long maxAddr = baseAddr + actualSize;
                    List<String> args = new ArrayList<>();
                    long offset = 0;
                    long argAddr = buf.get(ADDRESS, offset).address();
                    while (argAddr > baseAddr && argAddr < maxAddr) {
                        args.add(buf.getString(argAddr - baseAddr));
                        offset += ADDRESS.byteSize();
                        argAddr = buf.get(ADDRESS, offset).address();
                    }
                    return Collections.unmodifiableList(args);
                }
            } catch (Throwable e) {
                LOG.warn("Failed to get process arguments for pid {}", getProcessID(), e);
            }
        }
        return Collections.emptyList();
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        if (ARGMAX > 0) {
            int[] mib = { CTL_KERN, KERN_PROC_ARGS, getProcessID(), KERN_PROC_ENV };
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment mibSeg = arena.allocate(JAVA_INT, mib.length);
                for (int i = 0; i < mib.length; i++) {
                    mibSeg.setAtIndex(JAVA_INT, i, mib[i]);
                }
                MemorySegment buf = arena.allocate(ARGMAX);
                MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, (long) ARGMAX);
                MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
                if (OpenBsdLibcFunctions.sysctl(callState, mibSeg, mib.length, buf, sizeSeg, MemorySegment.NULL,
                        0L) == 0) {
                    long actualSize = sizeSeg.get(SIZE_T, 0);
                    long baseAddr = buf.address();
                    long maxAddr = baseAddr + actualSize;
                    Map<String, String> env = new LinkedHashMap<>();
                    long offset = 0;
                    long argAddr = buf.get(ADDRESS, offset).address();
                    while (argAddr > baseAddr && argAddr < maxAddr) {
                        String envStr = buf.getString(argAddr - baseAddr);
                        int idx = envStr.indexOf('=');
                        if (idx > 0) {
                            env.put(envStr.substring(0, idx), envStr.substring(idx + 1));
                        }
                        offset += ADDRESS.byteSize();
                        argAddr = buf.get(ADDRESS, offset).address();
                    }
                    return Collections.unmodifiableMap(env);
                }
            } catch (Throwable e) {
                LOG.warn("Failed to get environment variables for pid {}", getProcessID(), e);
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
            return rlimitNofile(true);
        }
        return -1L;
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return rlimitNofile(false);
        }
        return -1L;
    }

    private long rlimitNofile(boolean soft) {
        return ForeignFunctions.callInArenaLongOrDefault(arena -> {
            MemorySegment rlim = arena.allocate(OpenBsdLibcFunctions.RLIMIT_LAYOUT);
            if (OpenBsdLibcFunctions.getrlimit(RLIMIT_NOFILE, rlim) != 0) {
                return -1L;
            }
            return soft ? OpenBsdLibcFunctions.rlimitCur(rlim) : OpenBsdLibcFunctions.rlimitMax(rlim);
        }, LOG, org.slf4j.event.Level.WARN, "Failed getrlimit", -1L);
    }

}
