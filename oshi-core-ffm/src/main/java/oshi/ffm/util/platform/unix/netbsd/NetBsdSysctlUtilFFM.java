/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.netbsd;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.getErrno;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions;
import oshi.ffm.util.SysctlFFM;

/**
 * Provides access to sysctl calls on NetBSD via the FFM API.
 * <p>
 * Mirrors the API of {@code oshi.util.platform.unix.netbsd.NetBsdSysctlUtil} (JNA). As there, callers must gate every
 * native call on {@link #FFM_AVAILABLE} and fall back to the command-line implementations when it is {@code false}:
 * NetBSD's JDK builds for some architectures use the Zero VM, which may have no native linker.
 */
@ThreadSafe
public final class NetBsdSysctlUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(NetBsdSysctlUtilFFM.class);

    private static final String SYSCTL_FAIL = "Failed sysctl call: {}, Error code: {}";

    /**
     * Whether FFM native calls work on this system. When {@code false}, the native methods in this class will not
     * function and callers must use the command-line fallbacks. Detected once at class load by loading the
     * {@link NetBsdLibcFunctions} bindings and making a trivial call.
     */
    public static final boolean FFM_AVAILABLE;
    static {
        boolean available = false;
        try {
            // Loading the bindings creates the native linker, which throws on a JVM that has none
            available = NetBsdLibcFunctions.getpid() >= 0;
        } catch (Throwable e) { // NOSONAR java:S1181 - a missing linker throws an Error
            LOG.info("FFM native access is not available on NetBSD; using command-line fallbacks.");
        }
        FFM_AVAILABLE = available;
    }

    private NetBsdSysctlUtilFFM() {
    }

    /**
     * Executes a sysctl call with an int result.
     *
     * @param mib MIB array identifying the sysctl
     * @param def default int value
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(int[] mib, int def) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlMib(arena, mib, oldp, oldlenp), def, LOG,
                Arrays.toString(mib));
    }

    /**
     * Executes a sysctl call with a long result.
     *
     * @param mib MIB array identifying the sysctl
     * @param def default long value
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(int[] mib, long def) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlMib(arena, mib, oldp, oldlenp), def, LOG,
                Arrays.toString(mib));
    }

    /**
     * Executes a sysctl call with a String result.
     *
     * @param mib MIB array identifying the sysctl
     * @param def default String value
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(int[] mib, String def) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlMib(arena, mib, oldp, oldlenp), def, LOG,
                Arrays.toString(mib));
    }

    /**
     * Executes a sysctl call returning a freshly allocated buffer of the natural size.
     *
     * @param mib MIB array identifying the sysctl
     * @return an auto-arena {@link MemorySegment} containing the result on success; {@code null} otherwise
     */
    public static @Nullable MemorySegment sysctl(int[] mib) {
        return SysctlFFM.sysctl((arena, oldp, oldlenp) -> sysctlMib(arena, mib, oldp, oldlenp), LOG,
                Arrays.toString(mib));
    }

    /**
     * Calls this platform's {@code sysctl} with a MIB passed as a native segment. Matches
     * {@code oshi.driver.unix.bsd.BsdRouteDumpFFM.Sysctl}, so it can be passed as a method reference.
     *
     * @param callState errno capture
     * @param name      the MIB
     * @param namelen   entries in the MIB
     * @param oldp      buffer to fill, or {@link MemorySegment#NULL} to ask only for the size
     * @param oldlenp   size of the buffer, and on return the bytes written
     * @return 0 on success
     * @throws Throwable on invocation error
     */
    public static int sysctl(MemorySegment callState, MemorySegment name, int namelen, MemorySegment oldp,
            MemorySegment oldlenp) throws Throwable {
        return NetBsdLibcFunctions.sysctl(callState, name, namelen, oldp, oldlenp, MemorySegment.NULL, 0L);
    }

    private static boolean sysctlMib(Arena arena, int[] mib, MemorySegment oldp, MemorySegment oldlenp)
            throws Throwable {
        MemorySegment mibSeg = arena.allocateFrom(JAVA_INT, mib);
        MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
        int result = sysctl(callState, mibSeg, mib.length, oldp, oldlenp);
        if (result != 0) {
            // Guard so the native getErrno read is skipped when WARN is disabled
            if (LOG.isWarnEnabled()) {
                LOG.warn(SYSCTL_FAIL, Arrays.toString(mib), getErrno(callState));
            }
            return false;
        }
        return true;
    }
}
