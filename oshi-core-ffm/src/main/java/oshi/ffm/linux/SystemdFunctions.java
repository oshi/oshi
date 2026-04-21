/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.linux;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for libsystemd session query functions.
 */
public final class SystemdFunctions extends ForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(SystemdFunctions.class);

    private SystemdFunctions() {
    }

    private static final MethodHandle sd_get_sessions;
    private static final MethodHandle sd_session_get_username;
    private static final MethodHandle sd_session_get_start_time;
    private static final MethodHandle sd_session_get_tty;
    private static final MethodHandle sd_session_get_remote_host;
    private static final MethodHandle free;

    private static final boolean AVAILABLE;

    static {
        boolean available = false;
        MethodHandle hGetSessions = null;
        MethodHandle hGetUsername = null;
        MethodHandle hGetStartTime = null;
        MethodHandle hGetTty = null;
        MethodHandle hGetRemoteHost = null;
        MethodHandle hFree = null;
        try {
            SymbolLookup systemd = libraryLookup("systemd");
            SymbolLookup libc = LINKER.defaultLookup();

            // int sd_get_sessions(char ***sessions)
            hGetSessions = LINKER.downcallHandle(systemd.findOrThrow("sd_get_sessions"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS));
            // int sd_session_get_username(const char *session, char **username)
            hGetUsername = LINKER.downcallHandle(systemd.findOrThrow("sd_session_get_username"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            // int sd_session_get_start_time(const char *session, uint64_t *usec)
            hGetStartTime = LINKER.downcallHandle(systemd.findOrThrow("sd_session_get_start_time"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            // int sd_session_get_tty(const char *session, char **tty)
            hGetTty = LINKER.downcallHandle(systemd.findOrThrow("sd_session_get_tty"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            // int sd_session_get_remote_host(const char *session, char **remote_host)
            hGetRemoteHost = LINKER.downcallHandle(systemd.findOrThrow("sd_session_get_remote_host"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            // void free(void *ptr)
            hFree = LINKER.downcallHandle(libc.findOrThrow("free"), FunctionDescriptor.ofVoid(ADDRESS));
            available = true;
        } catch (Throwable t) {
            LOG.debug("libsystemd not available via FFM: {}", t.toString());
        }
        sd_get_sessions = hGetSessions;
        sd_session_get_username = hGetUsername;
        sd_session_get_start_time = hGetStartTime;
        sd_session_get_tty = hGetTty;
        sd_session_get_remote_host = hGetRemoteHost;
        free = hFree;
        AVAILABLE = available;
    }

    /**
     * Returns whether libsystemd was successfully loaded.
     *
     * @return true if all systemd symbols were bound
     */
    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Calls {@code sd_get_sessions(char ***sessions)}.
     *
     * @param sessionsPtr pointer-to-pointer output segment
     * @return number of sessions on success, negative errno on failure
     * @throws Throwable if the native call fails
     */
    public static int sdGetSessions(MemorySegment sessionsPtr) throws Throwable {
        return (int) sd_get_sessions.invokeExact(sessionsPtr);
    }

    /**
     * Calls {@code sd_session_get_username(session, &amp;username)}.
     *
     * @param session     session ID segment (null-terminated)
     * @param usernamePtr pointer-to-pointer output segment
     * @return 0 on success, negative errno on failure
     * @throws Throwable if the native call fails
     */
    public static int sdSessionGetUsername(MemorySegment session, MemorySegment usernamePtr) throws Throwable {
        return (int) sd_session_get_username.invokeExact(session, usernamePtr);
    }

    /**
     * Calls {@code sd_session_get_start_time(session, &amp;usec)}.
     *
     * @param session session ID segment (null-terminated)
     * @param usecPtr pointer to uint64_t output
     * @return 0 on success, negative errno on failure
     * @throws Throwable if the native call fails
     */
    public static int sdSessionGetStartTime(MemorySegment session, MemorySegment usecPtr) throws Throwable {
        return (int) sd_session_get_start_time.invokeExact(session, usecPtr);
    }

    /**
     * Calls {@code sd_session_get_tty(session, &amp;tty)}.
     *
     * @param session session ID segment (null-terminated)
     * @param ttyPtr  pointer-to-pointer output segment
     * @return 0 on success, negative errno on failure
     * @throws Throwable if the native call fails
     */
    public static int sdSessionGetTty(MemorySegment session, MemorySegment ttyPtr) throws Throwable {
        return (int) sd_session_get_tty.invokeExact(session, ttyPtr);
    }

    /**
     * Calls {@code sd_session_get_remote_host(session, &amp;remote_host)}.
     *
     * @param session       session ID segment (null-terminated)
     * @param remoteHostPtr pointer-to-pointer output segment
     * @return 0 on success, negative errno on failure
     * @throws Throwable if the native call fails
     */
    public static int sdSessionGetRemoteHost(MemorySegment session, MemorySegment remoteHostPtr) throws Throwable {
        return (int) sd_session_get_remote_host.invokeExact(session, remoteHostPtr);
    }

    /**
     * Calls {@code free(ptr)} to release memory allocated by systemd.
     *
     * @param ptr the pointer to free
     * @throws Throwable if the native call fails
     */
    public static void free(MemorySegment ptr) throws Throwable {
        free.invokeExact(ptr);
    }

    /**
     * Reads a null-terminated string from a pointer and frees the pointer.
     *
     * @param ptr   the pointer to a C string allocated by systemd
     * @param arena arena to scope the reinterpret
     * @return the Java string, or {@code null} if the pointer is NULL
     * @throws Throwable if the native call fails
     */
    public static String readAndFreeString(MemorySegment ptr, Arena arena) throws Throwable {
        if (ptr == null || ptr.equals(MemorySegment.NULL)) {
            return null;
        }
        try {
            return getStringFromNativePointer(ptr, arena);
        } finally {
            free(ptr);
        }
    }
}
