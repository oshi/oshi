/*
 * Copyright 2016-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.mac;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.mac.MacSystemFunctions.SIZE_T;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.MacSystemFunctions;

/**
 * Provides access to sysctl calls on macOS
 */
@ThreadSafe
public final class SysctlUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(SysctlUtil.class);

    private static final String SYSCTL_FAIL = "Failed sysctl call: {}, Error code: {}";

    private SysctlUtilFFM() {
    }

    /**
     * Executes a sysctl call with an int result
     *
     * @param name name of the sysctl
     * @param def  default int value
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(String name, int def) {
        return sysctl(name, def, true);
    }

    /**
     * Executes a sysctl call with an int result
     *
     * @param name       name of the sysctl
     * @param def        default int value
     * @param logWarning whether to log the warning if not available
     * @return The int result of the call if successful; the default otherwise
     */
    public static int sysctl(String name, int def, boolean logWarning) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment valueSeg = arena.allocate(JAVA_INT);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, Integer.BYTES);
            int result = MacSystemFunctions.sysctlbyname(nameSeg, valueSeg, sizeSeg, MemorySegment.NULL, 0L);

            if (result != 0) {
                if (logWarning) {
                    LOG.warn(SYSCTL_FAIL, name, result);
                }
                return def;
            }
            return valueSeg.get(JAVA_INT, 0);
        } catch (Throwable e) {
            if (logWarning) {
                LOG.warn("Failed to get sysctl value for {}", name, e);
            }
            return def;
        }
    }

    /**
     * Executes a sysctl call with a long result
     *
     * @param name name of the sysctl
     * @param def  default long value
     * @return The long result of the call if successful; the default otherwise
     */
    public static long sysctl(String name, long def) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment valueSeg = arena.allocate(JAVA_LONG);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, Integer.BYTES);
            int result = MacSystemFunctions.sysctlbyname(nameSeg, valueSeg, sizeSeg, MemorySegment.NULL, 0L);

            if (result != 0) {
                LOG.warn(SYSCTL_FAIL, name, result);
                return def;
            }
            return valueSeg.get(JAVA_LONG, 0);
        } catch (Throwable e) {
            LOG.warn("Failed to get sysctl value for {}", name, e);
            return def;
        }
    }

    /**
     * Executes a sysctl call with a String result
     *
     * @param name name of the sysctl
     * @param def  default String value
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(String name, String def) {
        return sysctl(name, def, true);
    }

    /**
     * Executes a sysctl call with a String result
     *
     * @param name       name of the sysctl
     * @param def        default String value
     * @param logWarning whether to log the warning if not available
     * @return The String result of the call if successful; the default otherwise
     */
    public static String sysctl(String name, String def, boolean logWarning) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment sizeSeg = arena.allocate(SIZE_T);
            int result = MacSystemFunctions.sysctlbyname(nameSeg, MemorySegment.NULL, sizeSeg, MemorySegment.NULL, 0L);
            if (result != 0) {
                if (logWarning) {
                    LOG.warn(SYSCTL_FAIL, name, result);
                }
                return def;
            }
            long size = sizeSeg.get(SIZE_T, 0);
            MemorySegment valueSeg = arena.allocate(size + 1); // +1 for null terminator
            result = MacSystemFunctions.sysctlbyname(nameSeg, valueSeg, sizeSeg, MemorySegment.NULL, 0L);

            if (result != 0) {
                if (logWarning) {
                    LOG.warn(SYSCTL_FAIL, name, result);
                }
                return def;
            }
            return valueSeg.getString(0);
        } catch (Throwable e) {
            if (logWarning) {
                LOG.warn("Failed to get sysctl value for {}", name, e);
            }
            return def;
        }
    }

    /**
     * Executes a sysctl call with a Structure result
     *
     * @param name   name of the sysctl
     * @param struct structure for the result
     * @return True if structure is successfuly populated, false otherwise
     */
    public static boolean sysctl(String name, MemorySegment struct) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, struct.byteSize());
            int result = MacSystemFunctions.sysctlbyname(nameSeg, struct, sizeSeg, MemorySegment.NULL, 0L);

            if (result != 0) {
                LOG.warn(SYSCTL_FAIL, name, result);
                return false;
            }
            return true;
        } catch (Throwable e) {
            LOG.warn("Failed to get sysctl value for {}", name, e);
            return false;
        }
    }

    /**
     * Executes a sysctl call with a Pointer result
     *
     * @param name name of the sysctl
     * @return An allocated memory buffer containing the result on success, null otherwise. Its value on failure is
     *         undefined.
     */
    public static MemorySegment sysctl(String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment sizeSeg = arena.allocate(SIZE_T);
            int result = MacSystemFunctions.sysctlbyname(nameSeg, MemorySegment.NULL, sizeSeg, MemorySegment.NULL, 0L);
            if (result != 0) {
                LOG.warn(SYSCTL_FAIL, name, result);
                return null;
            }
            long size = sizeSeg.get(SIZE_T, 0);
            MemorySegment valueSeg = arena.allocate(size);
            result = MacSystemFunctions.sysctlbyname(nameSeg, valueSeg, sizeSeg, MemorySegment.NULL, 0L);

            if (result != 0) {
                LOG.warn(SYSCTL_FAIL, name, result);
                return null;
            }
            // Need to copy to a segment that will be released on GC
            MemorySegment returnSeg = Arena.ofAuto().allocate(size);
            returnSeg.copyFrom(valueSeg);
            return returnSeg;
        } catch (Throwable e) {
            LOG.warn("Failed to get sysctl value for {}", name, e);
            return null;
        }
    }

    /**
     * Executes a sysctl call with a Memory Segment result
     *
     * @param mib    definition of the sysctl
     * @param buffer buffer to hold the result. Must be allocated
     * @return The size of data written to the buffer, or -1 if the call failed
     */
    public static long sysctl(int[] mib, MemorySegment buffer) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment mibSeg = arena.allocateFrom(JAVA_INT, mib);
            MemorySegment sizeSeg = arena.allocateFrom(SIZE_T, buffer.byteSize());
            int result = MacSystemFunctions.sysctl(mibSeg, mib.length, buffer, sizeSeg, MemorySegment.NULL, 0L);

            if (result != 0) {
                LOG.warn(SYSCTL_FAIL, Arrays.toString(mib), result);
                return -1;
            }
            return sizeSeg.get(SIZE_T, 0);
        } catch (Throwable e) {
            LOG.warn("Failed to get sysctl value for {}", Arrays.toString(mib), e);
            return -1;
        }
    }
}
