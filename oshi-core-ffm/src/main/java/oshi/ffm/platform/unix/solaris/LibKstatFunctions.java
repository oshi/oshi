/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.unix.solaris;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for Solaris/illumos {@code libkstat}, the kernel-statistics library.
 * <p>
 * Mirrors the surface of {@code com.sun.jna.platform.unix.solaris.LibKstat} so {@code KstatUtilFFM} can offer the same
 * API as the JNA {@code KstatUtil}. Chain operations are not thread-safe; {@code KstatUtilFFM} synchronizes externally.
 */
public final class LibKstatFunctions extends ForeignFunctions {

    private LibKstatFunctions() {
    }

    /** Maximum length of kstat module/name/class strings, including the NUL terminator. */
    public static final int KSTAT_STRLEN = 31;

    /** kstat data type: name/value pairs. */
    public static final byte KSTAT_TYPE_NAMED = 1;
    /** kstat data type: event timers. */
    public static final byte KSTAT_TYPE_TIMER = 4;

    /** {@code kstat_named_t} value tag: 16-byte character buffer. */
    public static final byte KSTAT_DATA_CHAR = 0;
    /** {@code kstat_named_t} value tag: signed 32-bit. */
    public static final byte KSTAT_DATA_INT32 = 1;
    /** {@code kstat_named_t} value tag: unsigned 32-bit. */
    public static final byte KSTAT_DATA_UINT32 = 2;
    /** {@code kstat_named_t} value tag: signed 64-bit. */
    public static final byte KSTAT_DATA_INT64 = 3;
    /** {@code kstat_named_t} value tag: unsigned 64-bit. */
    public static final byte KSTAT_DATA_UINT64 = 4;
    /** {@code kstat_named_t} value tag: variable-length string ({@code value.str}). */
    public static final byte KSTAT_DATA_STRING = 9;

    // ---- Struct layouts ----

    /**
     * Layout of {@code struct kstat} (the kstat header). All field offsets follow the C struct definition in
     * {@code <kstat.h>} on LP64 Solaris/illumos. Total size: 184 bytes.
     */
    public static final StructLayout KSTAT_LAYOUT = MemoryLayout.structLayout(JAVA_LONG.withName("ks_crtime"),
            ADDRESS.withName("ks_next"), JAVA_INT.withName("ks_kid"),
            MemoryLayout.sequenceLayout(KSTAT_STRLEN, JAVA_BYTE).withName("ks_module"), JAVA_BYTE.withName("ks_resv"),
            JAVA_INT.withName("ks_instance"), MemoryLayout.sequenceLayout(KSTAT_STRLEN, JAVA_BYTE).withName("ks_name"),
            JAVA_BYTE.withName("ks_type"), MemoryLayout.sequenceLayout(KSTAT_STRLEN, JAVA_BYTE).withName("ks_class"),
            JAVA_BYTE.withName("ks_flags"), ADDRESS.withName("ks_data"), JAVA_INT.withName("ks_ndata"),
            MemoryLayout.paddingLayout(4), JAVA_LONG.withName("ks_data_size"), JAVA_LONG.withName("ks_snaptime"),
            JAVA_INT.withName("ks_update"), MemoryLayout.paddingLayout(4), ADDRESS.withName("ks_private"),
            JAVA_INT.withName("ks_snapshot"), MemoryLayout.paddingLayout(4), ADDRESS.withName("ks_lock"));

    private static final VarHandle KSTAT_NEXT = KSTAT_LAYOUT.varHandle(PathElement.groupElement("ks_next"));
    private static final VarHandle KSTAT_INSTANCE = KSTAT_LAYOUT.varHandle(PathElement.groupElement("ks_instance"));
    private static final VarHandle KSTAT_TYPE = KSTAT_LAYOUT.varHandle(PathElement.groupElement("ks_type"));
    private static final VarHandle KSTAT_DATA = KSTAT_LAYOUT.varHandle(PathElement.groupElement("ks_data"));
    private static final VarHandle KSTAT_NDATA = KSTAT_LAYOUT.varHandle(PathElement.groupElement("ks_ndata"));
    private static final long KSTAT_MODULE_OFFSET = KSTAT_LAYOUT.byteOffset(PathElement.groupElement("ks_module"));
    private static final long KSTAT_NAME_OFFSET = KSTAT_LAYOUT.byteOffset(PathElement.groupElement("ks_name"));
    private static final long KSTAT_CLASS_OFFSET = KSTAT_LAYOUT.byteOffset(PathElement.groupElement("ks_class"));

    /**
     * Layout of {@code struct kstat_named}. Total size: 48 bytes. The {@code value} union occupies 16 bytes starting at
     * offset 32; readers must select the correct accessor based on {@link #namedDataType(MemorySegment)}.
     */
    public static final StructLayout KSTAT_NAMED_LAYOUT = MemoryLayout.structLayout(
            MemoryLayout.sequenceLayout(KSTAT_STRLEN, JAVA_BYTE).withName("name"), JAVA_BYTE.withName("data_type"),
            MemoryLayout.sequenceLayout(16, JAVA_BYTE).withName("value"));

    /** Byte size of a {@code kstat_named} record (48). */
    public static final long KSTAT_NAMED_SIZE = KSTAT_NAMED_LAYOUT.byteSize();

    /**
     * Layout of {@code kstat_io_t} ({@code <kstat.h>}). 80 bytes total on LP64 — no internal padding because the long
     * and int fields naturally align.
     */
    public static final StructLayout KSTAT_IO_LAYOUT = MemoryLayout.structLayout(JAVA_LONG.withName("nread"),
            JAVA_LONG.withName("nwritten"), JAVA_INT.withName("reads"), JAVA_INT.withName("writes"),
            JAVA_LONG.withName("wtime"), JAVA_LONG.withName("wlentime"), JAVA_LONG.withName("wlastupdate"),
            JAVA_LONG.withName("rtime"), JAVA_LONG.withName("rlentime"), JAVA_LONG.withName("rlastupdate"),
            JAVA_INT.withName("wcnt"), JAVA_INT.withName("rcnt"));

    private static final VarHandle KSTAT_IO_NREAD = KSTAT_IO_LAYOUT.varHandle(PathElement.groupElement("nread"));
    private static final VarHandle KSTAT_IO_NWRITTEN = KSTAT_IO_LAYOUT.varHandle(PathElement.groupElement("nwritten"));
    private static final VarHandle KSTAT_IO_READS = KSTAT_IO_LAYOUT.varHandle(PathElement.groupElement("reads"));
    private static final VarHandle KSTAT_IO_WRITES = KSTAT_IO_LAYOUT.varHandle(PathElement.groupElement("writes"));
    private static final VarHandle KSTAT_IO_RTIME = KSTAT_IO_LAYOUT.varHandle(PathElement.groupElement("rtime"));
    private static final VarHandle KSTAT_IO_WCNT = KSTAT_IO_LAYOUT.varHandle(PathElement.groupElement("wcnt"));
    private static final VarHandle KSTAT_IO_RCNT = KSTAT_IO_LAYOUT.varHandle(PathElement.groupElement("rcnt"));

    private static final VarHandle KSTAT_SNAPTIME = KSTAT_LAYOUT.varHandle(PathElement.groupElement("ks_snaptime"));

    /**
     * Reads {@code ks_snaptime} from a kstat header.
     *
     * @param ksp segment reinterpreted to {@link #KSTAT_LAYOUT}
     * @return the snapshot time, in nanoseconds since boot
     */
    public static long kstatSnaptime(MemorySegment ksp) {
        return (long) KSTAT_SNAPTIME.get(ksp, 0L);
    }

    /**
     * Reads {@code nread} from a {@code kstat_io_t} segment.
     *
     * @param io segment reinterpreted to {@link #KSTAT_IO_LAYOUT}
     * @return the number of bytes read
     */
    public static long kstatIoNread(MemorySegment io) {
        return (long) KSTAT_IO_NREAD.get(io, 0L);
    }

    /**
     * Reads {@code nwritten} from a {@code kstat_io_t} segment.
     *
     * @param io segment reinterpreted to {@link #KSTAT_IO_LAYOUT}
     * @return the number of bytes written
     */
    public static long kstatIoNwritten(MemorySegment io) {
        return (long) KSTAT_IO_NWRITTEN.get(io, 0L);
    }

    /**
     * Reads {@code reads} from a {@code kstat_io_t} segment.
     *
     * @param io segment reinterpreted to {@link #KSTAT_IO_LAYOUT}
     * @return the number of read operations
     */
    public static int kstatIoReads(MemorySegment io) {
        return (int) KSTAT_IO_READS.get(io, 0L);
    }

    /**
     * Reads {@code writes} from a {@code kstat_io_t} segment.
     *
     * @param io segment reinterpreted to {@link #KSTAT_IO_LAYOUT}
     * @return the number of write operations
     */
    public static int kstatIoWrites(MemorySegment io) {
        return (int) KSTAT_IO_WRITES.get(io, 0L);
    }

    /**
     * Reads {@code rtime} from a {@code kstat_io_t} segment.
     *
     * @param io segment reinterpreted to {@link #KSTAT_IO_LAYOUT}
     * @return cumulative run (service) time, in nanoseconds
     */
    public static long kstatIoRtime(MemorySegment io) {
        return (long) KSTAT_IO_RTIME.get(io, 0L);
    }

    /**
     * Reads {@code wcnt} from a {@code kstat_io_t} segment.
     *
     * @param io segment reinterpreted to {@link #KSTAT_IO_LAYOUT}
     * @return the count of elements in the wait queue
     */
    public static int kstatIoWcnt(MemorySegment io) {
        return (int) KSTAT_IO_WCNT.get(io, 0L);
    }

    /**
     * Reads {@code rcnt} from a {@code kstat_io_t} segment.
     *
     * @param io segment reinterpreted to {@link #KSTAT_IO_LAYOUT}
     * @return the count of elements in the run queue
     */
    public static int kstatIoRcnt(MemorySegment io) {
        return (int) KSTAT_IO_RCNT.get(io, 0L);
    }

    private static final VarHandle KSTAT_NAMED_DATA_TYPE = KSTAT_NAMED_LAYOUT
            .varHandle(PathElement.groupElement("data_type"));
    private static final long KSTAT_NAMED_NAME_OFFSET = KSTAT_NAMED_LAYOUT.byteOffset(PathElement.groupElement("name"));
    private static final long KSTAT_NAMED_VALUE_OFFSET = KSTAT_NAMED_LAYOUT
            .byteOffset(PathElement.groupElement("value"));

    // ---- Library binding ----

    // libkstat is loaded lazily via Arena.global so the symbol lookup persists for the JVM lifetime.
    private static final SymbolLookup LIBKSTAT = SymbolLookup.libraryLookup("libkstat.so.1", Arena.global());

    // kstat_ctl_t *kstat_open(void);
    private static final MethodHandle kstat_open = LINKER.downcallHandle(LIBKSTAT.findOrThrow("kstat_open"),
            FunctionDescriptor.of(ADDRESS));

    /**
     * Opens a kstat control structure.
     *
     * @return a {@code MemorySegment} addressing the {@code kstat_ctl_t}, or {@link MemorySegment#NULL} on failure
     * @throws Throwable on FFM invocation error
     */
    public static MemorySegment kstat_open() throws Throwable {
        return (MemorySegment) kstat_open.invokeExact();
    }

    // int kstat_close(kstat_ctl_t *kc);
    private static final MethodHandle kstat_close = LINKER.downcallHandle(LIBKSTAT.findOrThrow("kstat_close"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * Frees all resources associated with the given kstat control structure.
     *
     * @param kc the kstat control structure
     * @return 0 on success, -1 on failure
     * @throws Throwable on FFM invocation error
     */
    public static int kstat_close(MemorySegment kc) throws Throwable {
        return (int) kstat_close.invokeExact(kc);
    }

    // kid_t kstat_chain_update(kstat_ctl_t *kc);
    private static final MethodHandle kstat_chain_update = LINKER
            .downcallHandle(LIBKSTAT.findOrThrow("kstat_chain_update"), FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * Syncs the user kstat header chain with the kernel's.
     *
     * @param kc the kstat control structure
     * @return the new KCID if the chain changed, 0 if unchanged, or -1 on failure
     * @throws Throwable on FFM invocation error
     */
    public static int kstat_chain_update(MemorySegment kc) throws Throwable {
        return (int) kstat_chain_update.invokeExact(kc);
    }

    // kstat_t *kstat_lookup(kstat_ctl_t *kc, char *ks_module, int ks_instance, char *ks_name);
    private static final MethodHandle kstat_lookup = LINKER.downcallHandle(LIBKSTAT.findOrThrow("kstat_lookup"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS));

    /**
     * Searches the chain for a kstat matching {@code (module, instance, name)}.
     *
     * @param kc       the kstat control structure
     * @param module   the module name, or {@link MemorySegment#NULL} to wildcard
     * @param instance the instance number, or {@code -1} to wildcard
     * @param name     the kstat name, or {@link MemorySegment#NULL} to wildcard
     * @return a pointer to the matching kstat, or {@link MemorySegment#NULL} if none
     * @throws Throwable on FFM invocation error
     */
    public static MemorySegment kstat_lookup(MemorySegment kc, MemorySegment module, int instance, MemorySegment name)
            throws Throwable {
        return (MemorySegment) kstat_lookup.invokeExact(kc, module, instance, name);
    }

    // kid_t kstat_read(kstat_ctl_t *kc, kstat_t *ksp, void *buf);
    private static final MethodHandle kstat_read = LINKER.downcallHandle(LIBKSTAT.findOrThrow("kstat_read"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /**
     * Reads data for the kstat pointed to by {@code ksp} from the kernel.
     *
     * @param kc  the kstat control structure
     * @param ksp the kstat header
     * @param buf if non-NULL, data is copied here; otherwise allocated in {@code ksp.ks_data}
     * @return the current KCID on success, -1 on failure
     * @throws Throwable on FFM invocation error
     */
    public static int kstat_read(MemorySegment kc, MemorySegment ksp, MemorySegment buf) throws Throwable {
        return (int) kstat_read.invokeExact(kc, ksp, buf);
    }

    // void *kstat_data_lookup(kstat_t *ksp, char *name);
    private static final MethodHandle kstat_data_lookup = LINKER.downcallHandle(
            LIBKSTAT.findOrThrow("kstat_data_lookup"), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

    /**
     * Searches a named-data kstat for the record with the given key.
     *
     * @param ksp  the kstat to search
     * @param name the key
     * @return a pointer to the record (a {@code kstat_named_t} for {@link #KSTAT_TYPE_NAMED}), or
     *         {@link MemorySegment#NULL} if not found
     * @throws Throwable on FFM invocation error
     */
    public static MemorySegment kstat_data_lookup(MemorySegment ksp, MemorySegment name) throws Throwable {
        return (MemorySegment) kstat_data_lookup.invokeExact(ksp, name);
    }

    // ---- Field accessors (kstat header) ----

    /**
     * Returns the {@code ks_next} pointer of the kstat header, reinterpreted to {@link #KSTAT_LAYOUT}.
     *
     * @param ksp segment reinterpreted to {@link #KSTAT_LAYOUT}
     * @return next kstat segment (already reinterpreted), or {@link MemorySegment#NULL} if end of chain
     */
    public static MemorySegment kstatNext(MemorySegment ksp) {
        MemorySegment next = (MemorySegment) KSTAT_NEXT.get(ksp, 0L);
        if (next.address() == 0L) {
            return MemorySegment.NULL;
        }
        return next.reinterpret(KSTAT_LAYOUT.byteSize());
    }

    /**
     * Returns {@code ks_instance} from a kstat header.
     *
     * @param ksp segment reinterpreted to {@link #KSTAT_LAYOUT}
     * @return the module instance number
     */
    public static int kstatInstance(MemorySegment ksp) {
        return (int) KSTAT_INSTANCE.get(ksp, 0L);
    }

    /**
     * Returns {@code ks_type} from a kstat header.
     *
     * @param ksp segment reinterpreted to {@link #KSTAT_LAYOUT}
     * @return the kstat data type (e.g. {@link #KSTAT_TYPE_NAMED})
     */
    public static byte kstatType(MemorySegment ksp) {
        return (byte) KSTAT_TYPE.get(ksp, 0L);
    }

    /**
     * Returns the {@code ks_data} pointer of the kstat header.
     *
     * @param ksp segment reinterpreted to {@link #KSTAT_LAYOUT}
     * @return the data segment as a raw pointer (size unknown until reinterpreted)
     */
    public static MemorySegment kstatData(MemorySegment ksp) {
        return (MemorySegment) KSTAT_DATA.get(ksp, 0L);
    }

    /**
     * Returns {@code ks_ndata} from a kstat header.
     *
     * @param ksp segment reinterpreted to {@link #KSTAT_LAYOUT}
     * @return the number of data records
     */
    public static int kstatNdata(MemorySegment ksp) {
        return (int) KSTAT_NDATA.get(ksp, 0L);
    }

    /**
     * Reads {@code ks_module} as a NUL-terminated string.
     *
     * @param ksp segment reinterpreted to {@link #KSTAT_LAYOUT}
     * @return the module name
     */
    public static String kstatModule(MemorySegment ksp) {
        return ksp.getString(KSTAT_MODULE_OFFSET);
    }

    /**
     * Reads {@code ks_name} as a NUL-terminated string.
     *
     * @param ksp segment reinterpreted to {@link #KSTAT_LAYOUT}
     * @return the kstat name
     */
    public static String kstatName(MemorySegment ksp) {
        return ksp.getString(KSTAT_NAME_OFFSET);
    }

    /**
     * Reads {@code ks_class} as a NUL-terminated string.
     *
     * @param ksp segment reinterpreted to {@link #KSTAT_LAYOUT}
     * @return the kstat class
     */
    public static String kstatClass(MemorySegment ksp) {
        return ksp.getString(KSTAT_CLASS_OFFSET);
    }

    // ---- Field accessors (kstat_named) ----

    /**
     * Returns the {@code data_type} tag of a {@code kstat_named_t} record.
     *
     * @param named segment reinterpreted to {@link #KSTAT_NAMED_LAYOUT}
     * @return the data-type tag (one of the {@code KSTAT_DATA_*} constants)
     */
    public static byte namedDataType(MemorySegment named) {
        return (byte) KSTAT_NAMED_DATA_TYPE.get(named, 0L);
    }

    /**
     * Reads the {@code name} field of a {@code kstat_named_t} record as a NUL-terminated string.
     *
     * @param named segment reinterpreted to {@link #KSTAT_NAMED_LAYOUT}
     * @return the record name
     */
    public static String namedName(MemorySegment named) {
        return named.getString(KSTAT_NAMED_NAME_OFFSET);
    }

    /**
     * Reads the union of a {@code kstat_named_t} record as {@code int32}.
     *
     * @param named segment reinterpreted to {@link #KSTAT_NAMED_LAYOUT}
     * @return the value as a 32-bit signed integer
     */
    public static int namedValueInt32(MemorySegment named) {
        return named.get(JAVA_INT, KSTAT_NAMED_VALUE_OFFSET);
    }

    /**
     * Reads the union of a {@code kstat_named_t} record as {@code int64}.
     *
     * @param named segment reinterpreted to {@link #KSTAT_NAMED_LAYOUT}
     * @return the value as a 64-bit signed integer
     */
    public static long namedValueInt64(MemorySegment named) {
        return named.get(JAVA_LONG, KSTAT_NAMED_VALUE_OFFSET);
    }

    /**
     * Reads the union as a {@code KSTAT_DATA_CHAR} buffer (16 bytes, NUL-terminated).
     *
     * @param named segment reinterpreted to {@link #KSTAT_NAMED_LAYOUT}
     * @return the string
     */
    public static String namedValueChar(MemorySegment named) {
        return named.getString(KSTAT_NAMED_VALUE_OFFSET);
    }

    /**
     * Reads the union as a {@code KSTAT_DATA_STRING} ({@code struct kstat_named.value.str}): pointer + length. The
     * pointer addresses a string elsewhere in the kstat data buffer.
     *
     * @param named segment reinterpreted to {@link #KSTAT_NAMED_LAYOUT}
     * @return the string, or empty if the pointer is NULL
     */
    public static String namedValueString(MemorySegment named) {
        MemorySegment strPtr = named.get(ADDRESS, KSTAT_NAMED_VALUE_OFFSET);
        if (strPtr.address() == 0L) {
            return "";
        }
        // Read length from offset+8 (struct STR { Pointer addr; int len; })
        int len = named.get(JAVA_INT, KSTAT_NAMED_VALUE_OFFSET + 8);
        if (len <= 0) {
            return "";
        }
        return strPtr.reinterpret(len).getString(0);
    }

    /**
     * Returns {@code true} if {@code libkstat.so.1} loaded successfully. Effectively always {@code true} on Solaris and
     * illumos systems where this module is exercised; included for symmetry with {@code Kstat2Functions.HAS_KSTAT2}.
     *
     * @return whether {@code libkstat.so.1} was loadable at class-init time
     */
    public static boolean isLibKstatLoaded() {
        return true;
    }
}
