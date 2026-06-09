/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.unix.dragonflybsd;

import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for DragonFly BSD-specific libc functions. DragonFly shares most of its libc surface with FreeBSD, so
 * shared functions ({@code sysctlbyname}, {@code sysctl}, {@code getpid}, {@code getrlimit}, etc.) are called via
 * {@link oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions}. This class adds only the DragonFly-specific
 * {@code lwp_gettid()} (FreeBSD uses {@code thr_self} instead).
 */
public final class DragonFlyBsdLibcFunctions extends ForeignFunctions {

    private DragonFlyBsdLibcFunctions() {
    }

    private static final SymbolLookup LIBC = LINKER.defaultLookup();

    // int lwp_gettid(void);
    private static final MethodHandle lwp_gettid = LINKER.downcallHandle(LIBC.findOrThrow("lwp_gettid"),
            FunctionDescriptor.of(JAVA_INT));

    /**
     * Calls {@code lwp_gettid()} — returns the kernel thread ID of the calling LWP.
     *
     * @return the kernel thread ID, or -1 on error
     * @throws Throwable on FFM invocation error
     */
    public static int lwp_gettid() throws Throwable {
        return (int) lwp_gettid.invokeExact();
    }
}
