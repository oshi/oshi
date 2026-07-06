/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * Base class providing utility methods for working with the Java Foreign Function and Memory (FFM) API.
 * <p>
 * Subclasses use these helpers to load native libraries and frameworks, create downcall handles, and read data from
 * native memory segments.
 */
public abstract class ForeignFunctions {

    /**
     * Represents an operation that uses a confined {@link Arena} and returns an object.
     *
     * @param <T> the return type
     */
    @FunctionalInterface
    public interface ArenaCallable<T> {
        /**
         * Executes this operation with the provided arena.
         *
         * @param arena the confined arena scoped to this operation
         * @return the operation result
         * @throws Throwable if the operation fails
         */
        T call(Arena arena) throws Throwable;
    }

    /**
     * Represents an operation that uses a confined {@link Arena} and returns an {@code int}.
     */
    @FunctionalInterface
    public interface ArenaIntCallable {
        /**
         * Executes this operation with the provided arena.
         *
         * @param arena the confined arena scoped to this operation
         * @return the operation result
         * @throws Throwable if the operation fails
         */
        int call(Arena arena) throws Throwable;
    }

    /**
     * Represents an operation that uses a confined {@link Arena} and returns a {@code long}.
     */
    @FunctionalInterface
    public interface ArenaLongCallable {
        /**
         * Executes this operation with the provided arena.
         *
         * @param arena the confined arena scoped to this operation
         * @return the operation result
         * @throws Throwable if the operation fails
         */
        long call(Arena arena) throws Throwable;
    }

    /**
     * Represents an operation that uses a confined {@link Arena} and returns a {@code double}.
     */
    @FunctionalInterface
    public interface ArenaDoubleCallable {
        /**
         * Executes this operation with the provided arena.
         *
         * @param arena the confined arena scoped to this operation
         * @return the operation result
         * @throws Throwable if the operation fails
         */
        double call(Arena arena) throws Throwable;
    }

    /**
     * Represents an operation that uses a confined {@link Arena} and returns a {@code boolean}.
     */
    @FunctionalInterface
    public interface ArenaBooleanCallable {
        /**
         * Executes this operation with the provided arena.
         *
         * @param arena the confined arena scoped to this operation
         * @return the operation result
         * @throws Throwable if the operation fails
         */
        boolean call(Arena arena) throws Throwable;
    }

    /**
     * Represents an operation that uses a confined {@link Arena} and does not return a value.
     */
    @FunctionalInterface
    public interface ArenaRunnable {
        /**
         * Executes this operation with the provided arena.
         *
         * @param arena the confined arena scoped to this operation
         * @throws Throwable if the operation fails
         */
        void run(Arena arena) throws Throwable;
    }

    /** The native linker for the current platform. */
    protected static final Linker LINKER = Linker.nativeLinker();

    /** A shared auto arena used for library symbol lookups. */
    protected static final Arena LIBRARY_ARENA = Arena.ofAuto();

    /** Symbol lookup for libraries already loaded into the current process. */
    protected static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup();

    /** The size in bytes of the C {@code long} type on this platform. */
    public static final long NATIVE_LONG_SIZE = LINKER.canonicalLayouts().get("long").byteSize();

    /** The size in bytes of the C {@code size_t} type on this platform. */
    public static final long NATIVE_SIZE_T_SIZE = LINKER.canonicalLayouts().get("size_t").byteSize();

    /** The size in bytes of a native pointer on this platform. */
    public static final long NATIVE_POINTER_SIZE = ValueLayout.ADDRESS.byteSize();

    /** Not intended for instantiation. */
    protected ForeignFunctions() {
    }

    /**
     * Executes an operation in a confined arena, returning a default value if the operation throws.
     * <p>
     * This helper centralizes the common FFM call pattern where temporary native memory is scoped to a confined arena
     * and failures from method-handle invocation or native binding are logged at the caller's chosen level. The
     * provided arena is closed before this method returns, so returned objects must not depend on memory allocated from
     * it.
     *
     * @param <T>          the return type
     * @param callable     the operation to execute
     * @param logger       the logger for the calling class
     * @param level        the level at which to log thrown failures
     * @param message      the message to log if the operation throws
     * @param defaultValue the value to return if the operation throws
     * @return the operation result, or {@code defaultValue} if the operation throws
     */
    public static <T> T callInArenaOrDefault(ArenaCallable<T> callable, Logger logger, Level level, String message,
            T defaultValue) {
        Objects.requireNonNull(callable, "callable");
        try (Arena arena = Arena.ofConfined()) {
            return callable.call(arena);
        } catch (Throwable t) {
            logThrowable(logger, level, message, t);
            return defaultValue;
        }
    }

    /**
     * Executes an {@code int}-returning operation in a confined arena, returning a default value if the operation
     * throws.
     * <p>
     * Use this primitive-specialized variant to avoid boxing when wrapping FFM calls whose result is an {@code int}.
     * The provided arena is closed before this method returns.
     *
     * @param callable     the operation to execute
     * @param logger       the logger for the calling class
     * @param level        the level at which to log thrown failures
     * @param message      the message to log if the operation throws
     * @param defaultValue the value to return if the operation throws
     * @return the operation result, or {@code defaultValue} if the operation throws
     */
    public static int callInArenaIntOrDefault(ArenaIntCallable callable, Logger logger, Level level, String message,
            int defaultValue) {
        Objects.requireNonNull(callable, "callable");
        try (Arena arena = Arena.ofConfined()) {
            return callable.call(arena);
        } catch (Throwable t) {
            logThrowable(logger, level, message, t);
            return defaultValue;
        }
    }

    /**
     * Executes a {@code long}-returning operation in a confined arena, returning a default value if the operation
     * throws.
     * <p>
     * Use this primitive-specialized variant to avoid boxing when wrapping FFM calls whose result is a {@code long}.
     * The provided arena is closed before this method returns.
     *
     * @param callable     the operation to execute
     * @param logger       the logger for the calling class
     * @param level        the level at which to log thrown failures
     * @param message      the message to log if the operation throws
     * @param defaultValue the value to return if the operation throws
     * @return the operation result, or {@code defaultValue} if the operation throws
     */
    public static long callInArenaLongOrDefault(ArenaLongCallable callable, Logger logger, Level level, String message,
            long defaultValue) {
        Objects.requireNonNull(callable, "callable");
        try (Arena arena = Arena.ofConfined()) {
            return callable.call(arena);
        } catch (Throwable t) {
            logThrowable(logger, level, message, t);
            return defaultValue;
        }
    }

    /**
     * Executes a {@code double}-returning operation in a confined arena, returning a default value if the operation
     * throws.
     * <p>
     * Use this primitive-specialized variant to avoid boxing when wrapping FFM calls whose result is a {@code double}.
     * The provided arena is closed before this method returns.
     *
     * @param callable     the operation to execute
     * @param logger       the logger for the calling class
     * @param level        the level at which to log thrown failures
     * @param message      the message to log if the operation throws
     * @param defaultValue the value to return if the operation throws
     * @return the operation result, or {@code defaultValue} if the operation throws
     */
    public static double callInArenaDoubleOrDefault(ArenaDoubleCallable callable, Logger logger, Level level,
            String message, double defaultValue) {
        Objects.requireNonNull(callable, "callable");
        try (Arena arena = Arena.ofConfined()) {
            return callable.call(arena);
        } catch (Throwable t) {
            logThrowable(logger, level, message, t);
            return defaultValue;
        }
    }

    /**
     * Executes a {@code boolean}-returning operation in a confined arena, returning a default value if the operation
     * throws.
     * <p>
     * Use this primitive-specialized variant to avoid boxing when wrapping FFM calls whose result is a {@code boolean}.
     * The provided arena is closed before this method returns.
     *
     * @param callable     the operation to execute
     * @param logger       the logger for the calling class
     * @param level        the level at which to log thrown failures
     * @param message      the message to log if the operation throws
     * @param defaultValue the value to return if the operation throws
     * @return the operation result, or {@code defaultValue} if the operation throws
     */
    public static boolean callInArenaBooleanOrDefault(ArenaBooleanCallable callable, Logger logger, Level level,
            String message, boolean defaultValue) {
        Objects.requireNonNull(callable, "callable");
        try (Arena arena = Arena.ofConfined()) {
            return callable.call(arena);
        } catch (Throwable t) {
            logThrowable(logger, level, message, t);
            return defaultValue;
        }
    }

    /**
     * Executes an operation in a confined arena, logging and swallowing any thrown failure.
     * <p>
     * This helper is intended for FFM operations whose useful result is a side effect. The provided arena is closed
     * before this method returns.
     *
     * @param runnable the operation to execute
     * @param logger   the logger for the calling class
     * @param level    the level at which to log thrown failures
     * @param message  the message to log if the operation throws
     */
    public static void runInArenaCatchingThrowable(ArenaRunnable runnable, Logger logger, Level level, String message) {
        Objects.requireNonNull(runnable, "runnable");
        try (Arena arena = Arena.ofConfined()) {
            runnable.run(arena);
        } catch (Throwable t) {
            logThrowable(logger, level, message, t);
        }
    }

    /**
     * Lookup a native library by simple name, mapping it to the platform-specific filename (e.g. {@code "c"} →
     * {@code "libc.so"} on Linux).
     *
     * @param libraryName the platform-independent library name
     * @return the symbol lookup for the library
     */
    public static SymbolLookup libraryLookup(String libraryName) {
        return SymbolLookup.libraryLookup(System.mapLibraryName(libraryName), LIBRARY_ARENA);
    }

    // ---- dlopen/dlsym escape hatch ----
    // SymbolLookup.libraryLookup() can't pass custom flags; some platforms need them (notably AIX
    // archive-member loading via RTLD_MEMBER). We bind dlopen/dlsym from libc directly so callers
    // can dlopen with whatever flags they need and wrap the handle into a SymbolLookup.
    //
    // Bill Pugh holder pattern: these symbols are POSIX-only (Windows has LoadLibrary +
    // GetProcAddress instead), so binding them eagerly in <clinit> on ForeignFunctions would break
    // every Windows FFM class that extends this one. The holder class is only loaded on first
    // access — i.e., on the first call to dlopenWithFlags — which Windows code paths never make.

    private static final class DlSyms {
        static final MethodHandle DLOPEN = LINKER.downcallHandle(LINKER.defaultLookup().findOrThrow("dlopen"),
                FunctionDescriptor.of(java.lang.foreign.ValueLayout.ADDRESS, java.lang.foreign.ValueLayout.ADDRESS,
                        java.lang.foreign.ValueLayout.JAVA_INT));

        static final MethodHandle DLSYM = LINKER.downcallHandle(LINKER.defaultLookup().findOrThrow("dlsym"),
                FunctionDescriptor.of(java.lang.foreign.ValueLayout.ADDRESS, java.lang.foreign.ValueLayout.ADDRESS,
                        java.lang.foreign.ValueLayout.ADDRESS));
    }

    /**
     * Calls {@code dlopen(path, flags)} directly through the C runtime, returning a {@link SymbolLookup} over the
     * loaded library. Use this when {@link SymbolLookup#libraryLookup(java.nio.file.Path, Arena)} can't pass the flag
     * set the platform requires — most notably AIX's {@code RTLD_MEMBER} for archive-member loading.
     * <p>
     * The library is loaded into {@link Arena#global()} so the lookup persists for the JVM lifetime, mirroring how
     * statically-bound system libraries are loaded.
     *
     * @param path  absolute path passed verbatim to {@code dlopen} (must include any platform-specific syntax, e.g.
     *              {@code "/usr/lib/libperfstat.a(shr_64.o)"} on AIX)
     * @param flags bitwise-OR of the {@code RTLD_*} flags from the platform's {@code <dlfcn.h>}
     * @return a {@link SymbolLookup} backed by {@code dlsym} on the loaded library
     * @throws UnsatisfiedLinkError if {@code dlopen} returns {@code NULL}
     */
    public static SymbolLookup dlopenWithFlags(String path, int flags) {
        try {
            Arena global = Arena.global();
            MemorySegment pathSeg = global.allocateFrom(path);
            MemorySegment handle = (MemorySegment) DlSyms.DLOPEN.invokeExact(pathSeg, flags);
            if (handle.address() == 0L) {
                throw new UnsatisfiedLinkError("dlopen returned NULL for " + path);
            }
            return dlsymLookup(handle);
        } catch (UnsatisfiedLinkError e) {
            throw e;
        } catch (Throwable t) {
            throw new UnsatisfiedLinkError("dlopen failed for " + path + ": " + t.getMessage());
        }
    }

    /**
     * Calls {@link #dlopenWithFlags(String, int)} on each path in turn, returning the first lookup that succeeds.
     * Useful for libraries that may live under multiple names — e.g. AIX's 64-bit/32-bit shared-object members of
     * {@code libperfstat.a}.
     *
     * @param paths candidate absolute paths to try, in order
     * @param flags bitwise-OR of {@code RTLD_*} flags
     * @return the first successful {@link SymbolLookup}
     * @throws UnsatisfiedLinkError if every path's {@code dlopen} returns {@code NULL}
     */
    public static SymbolLookup dlopenFirstAvailable(java.util.List<String> paths, int flags) {
        UnsatisfiedLinkError last = null;
        for (String path : paths) {
            try {
                return dlopenWithFlags(path, flags);
            } catch (UnsatisfiedLinkError e) {
                last = e;
            }
        }
        throw new UnsatisfiedLinkError(
                "dlopen returned NULL for every candidate: " + paths + (last == null ? "" : " (" + last + ")"));
    }

    private static SymbolLookup dlsymLookup(MemorySegment libHandle) {
        return name -> {
            try (Arena local = Arena.ofConfined()) {
                MemorySegment nameSeg = local.allocateFrom(name);
                MemorySegment sym = (MemorySegment) DlSyms.DLSYM.invokeExact(libHandle, nameSeg);
                return sym.address() == 0L ? java.util.Optional.empty()
                        : java.util.Optional.of(MemorySegment.ofAddress(sym.address()));
            } catch (Throwable _) {
                return java.util.Optional.empty();
            }
        };
    }

    /**
     * Reinterpret a raw native pointer as a struct of the given layout, scoped to the provided arena.
     *
     * @param pointer the native pointer
     * @param layout  the struct layout
     * @param arena   the arena to scope the resulting segment to
     * @return a memory segment over the struct, or {@code null} if the pointer is null or {@link MemorySegment#NULL}
     */
    public static MemorySegment getStructFromNativePointer(MemorySegment pointer, StructLayout layout, Arena arena) {
        if (pointer == null || pointer.equals(MemorySegment.NULL)) {
            return null;
        }
        return MemorySegment.ofAddress(pointer.address()).reinterpret(layout.byteSize(), arena, null);
    }

    /**
     * Read a null-terminated UTF-8 string from a raw native pointer.
     *
     * @param pointer the native pointer
     * @param arena   the arena to scope the reinterpreted segment to
     * @return the Java string, or {@code null} if the pointer is null or {@link MemorySegment#NULL}
     */
    public static String getStringFromNativePointer(MemorySegment pointer, Arena arena) {
        if (pointer == null || pointer.equals(MemorySegment.NULL)) {
            return null;
        }
        // More than enough space for 255 UTF characters
        return MemorySegment.ofAddress(pointer.address()).reinterpret(1024, arena, null).getString(0);
    }

    /**
     * Copy {@code length} bytes from a raw native pointer into a Java byte array.
     *
     * @param pointer the native pointer
     * @param length  the number of bytes to copy
     * @param arena   the arena to scope the reinterpreted segment to
     * @return the byte array, or {@code null} if the pointer is null or {@link MemorySegment#NULL}
     */
    public static byte[] getByteArrayFromNativePointer(MemorySegment pointer, long length, Arena arena) {
        if (pointer == null || pointer.equals(MemorySegment.NULL)) {
            return null;
        }
        MemorySegment bytesSegment = MemorySegment.ofAddress(pointer.address()).reinterpret(length, arena, null);
        byte[] result = new byte[(int) length];
        MemorySegment.copy(bytesSegment, JAVA_BYTE, 0, result, 0, (int) length);
        return result;
    }

    /**
     * Lookup a library by name in the global arena.
     *
     * @param name the library name
     * @return the symbol lookup for the library
     */
    public static SymbolLookup lib(String name) {
        return SymbolLookup.libraryLookup(name, Arena.global());
    }

    /**
     * Create a downcall handle for a symbol in a library.
     *
     * @param lib        the symbol lookup
     * @param symbol     the symbol name
     * @param resLayout  the return layout
     * @param argLayouts the argument layouts
     * @return the method handle
     */
    public static MethodHandle downcall(SymbolLookup lib, String symbol, MemoryLayout resLayout,
            MemoryLayout... argLayouts) {
        MemorySegment sym = lib.findOrThrow(symbol);
        FunctionDescriptor fd = (resLayout == null) ? FunctionDescriptor.ofVoid(argLayouts)
                : FunctionDescriptor.of(resLayout, argLayouts);
        return LINKER.downcallHandle(sym, fd);
    }

    /** Linker option to capture {@code errno} after a native call. */
    protected static final Linker.Option CAPTURE_CALL_STATE = Linker.Option.captureCallState("errno");

    /** Layout of the captured call state segment, containing {@code errno}. */
    public static final StructLayout CAPTURED_STATE_LAYOUT = Linker.Option.captureStateLayout();

    /** Handle to read the {@code errno} field from a captured call state segment. */
    protected static final VarHandle ERRNO_HANDLE = CAPTURED_STATE_LAYOUT.varHandle(PathElement.groupElement("errno"));

    /**
     * Read the {@code errno} value from a captured call state segment.
     *
     * @param callState the memory segment returned by a call made with {@link #CAPTURE_CALL_STATE}
     * @return the {@code errno} value
     */
    public static int getErrno(MemorySegment callState) {
        return (int) ERRNO_HANDLE.get(callState, 0);
    }

    private static void logThrowable(Logger logger, Level level, String message, Throwable t) {
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(level, "level");
        String format = message + ": {}";
        switch (level) {
            case ERROR:
                logger.error(format, t.getMessage(), t);
                break;
            case WARN:
                logger.warn(format, t.getMessage(), t);
                break;
            case INFO:
                logger.info(format, t.getMessage(), t);
                break;
            case TRACE:
                logger.trace(format, t.getMessage(), t);
                break;
            case DEBUG:
            default:
                logger.debug(format, t.getMessage(), t);
                break;
        }
    }
}
