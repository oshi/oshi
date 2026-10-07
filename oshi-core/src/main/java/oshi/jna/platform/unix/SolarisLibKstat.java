/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.solaris.LibKstat;
import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;
import com.sun.jna.platform.unix.solaris.LibKstat.KstatCtl;

/**
 * Re-declares the {@code libkstat} entry points that take a {@code kstat_ctl_t *}, mapping that argument as an opaque
 * {@link Pointer} rather than as JNA's {@link KstatCtl} structure.
 * <p>
 * JNA's {@code KstatCtl} declares {@code kc_chain} as an inline {@link Kstat} structure instead of a pointer to one, so
 * JNA computes {@code sizeof(kstat_ctl_t)} as 200 bytes where the real struct, from {@code <kstat.h>}, is 24:
 *
 * <pre>
 * typedef struct kstat_ctl {
 *     kid_t    kc_chain_id;
 *     kstat_t *kc_chain;
 *     int      kc_kd;
 * } kstat_ctl_t;
 * </pre>
 *
 * A {@code Structure} passed by reference is auto-written to native memory before every call and auto-read after it, so
 * each {@code kstat_chain_update}, {@code kstat_lookup}, or {@code kstat_read} wrote 176 bytes of a stale snapshot past
 * the end of the 24-byte block {@code kstat_open} allocated, and read 176 bytes past it. On illumos that silently
 * rewrites whichever neighbouring heap buffers share the allocator's slab, which shows up far from the kstat code as a
 * {@code UMEM_SLAB_MEMBER} assertion failure in {@code umem_slab_alloc} (a freed neighbour's free-list link
 * overwritten) or as a SIGSEGV on a half-overwritten pointer.
 * <p>
 * Treating the control structure as opaque, as the FFM implementation already does, removes both the overrunning write
 * and the overrunning read. {@code kstat_data_lookup} needs no redeclaration because it does not take a
 * {@code kstat_ctl_t *}, and {@link Kstat} itself is mapped correctly at its real 184 bytes.
 * <p>
 * This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA project.
 */
public interface SolarisLibKstat extends Library {

    /** The singleton instance of this library. */
    SolarisLibKstat INSTANCE = Native.load("kstat", SolarisLibKstat.class);

    /**
     * Initializes a kstat control structure, which provides access to the kernel statistics library.
     *
     * @return An opaque pointer to the {@code kstat_ctl_t}, which must be supplied as the {@code kc} argument in
     *         subsequent libkstat calls, or {@code null} on failure.
     */
    Pointer kstat_open();

    /**
     * Frees all resources that were associated with {@code kc}.
     *
     * @param kc A pointer to a kstat control structure
     * @return 0 on success and -1 on failure.
     */
    int kstat_close(Pointer kc);

    /**
     * Brings the user's kstat header chain in sync with that of the kernel.
     *
     * @param kc A pointer to a kstat control structure
     * @return the new KCID if the kstat chain has changed, 0 if it hasn't, or -1 on failure.
     */
    int kstat_chain_update(Pointer kc);

    /**
     * Traverses the kstat chain searching for a kstat with the given {@code ks_module}, {@code ks_instance}, and
     * {@code ks_name}. If {@code ks_module} is {@code null}, {@code ks_instance} is -1, or {@code ks_name} is
     * {@code null}, those fields are ignored in the search.
     *
     * @param kc          A pointer to a kstat control structure
     * @param ks_module   The kstat module to search
     * @param ks_instance The kstat instance number
     * @param ks_name     The kstat name to search
     * @return the requested kstat if it is found, or {@code null} if it is not.
     */
    Kstat kstat_lookup(Pointer kc, String ks_module, int ks_instance, String ks_name);

    /**
     * Gets data from the kernel for the kstat pointed to by {@code ksp}.
     *
     * @param kc  A pointer to a kstat control structure
     * @param ksp The kstat from which to retrieve data
     * @param p   If non-{@code null}, the data is copied from {@code ksp.ks_data} into it
     * @return the current kstat chain ID (KCID) on success, or -1 on failure.
     */
    int kstat_read(Pointer kc, Kstat ksp, Pointer p);

    /**
     * Searches the kstat's data section for the record with the specified name. Valid only for
     * {@link LibKstat#KSTAT_TYPE_NAMED} and {@link LibKstat#KSTAT_TYPE_TIMER} kstats.
     *
     * @param ksp  The kstat to search
     * @param name The key for the name-value pair, or name of the timer as applicable
     * @return a pointer to the requested data record, or {@code null} if it is not found or the kstat type is invalid.
     */
    Pointer kstat_data_lookup(Kstat ksp, String name);
}
