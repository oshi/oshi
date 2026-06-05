/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.solaris;

import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.KSTAT_DATA_CHAR;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.KSTAT_DATA_INT32;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.KSTAT_DATA_INT64;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.KSTAT_DATA_STRING;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.KSTAT_DATA_UINT32;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.KSTAT_DATA_UINT64;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.KSTAT_LAYOUT;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.KSTAT_NAMED_LAYOUT;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.KSTAT_TYPE_NAMED;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.KSTAT_TYPE_TIMER;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.kstatClass;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.kstatInstance;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.kstatModule;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.kstatName;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.kstatNext;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.kstatType;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.namedDataType;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.namedValueChar;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.namedValueInt32;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.namedValueInt64;
import static oshi.ffm.util.platform.unix.solaris.LibKstatFunctions.namedValueString;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.GuardedBy;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FormatUtil;

/**
 * FFM equivalent of {@code oshi.util.platform.unix.solaris.KstatUtil}: thread-safe access to kstat data via
 * {@link LibKstatFunctions}. Mirrors the JNA wrapper's API surface (chain lookup, named-data accessors). Kstat2 methods
 * are intentionally not implemented here yet — see {@code Kstat2Functions} for the gated probe.
 */
@ThreadSafe
public final class KstatUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(KstatUtilFFM.class);

    private static final Lock CHAIN = new ReentrantLock();

    /** Cached {@code kstat_ctl_t} pointer for the JVM lifetime; opened lazily. */
    @GuardedBy("CHAIN")
    private static MemorySegment kstatCtl = MemorySegment.NULL;

    private KstatUtilFFM() {
    }

    /**
     * A copy of the Kstat chain. Only one thread may actively use this object at any time. Acquired via
     * {@link #openChain()} and released via {@link #close()}.
     */
    public static final class KstatChain implements AutoCloseable {

        private final MemorySegment ctl;

        private KstatChain(MemorySegment ctl) {
            this.ctl = ctl;
            update();
        }

        /**
         * Calls {@code kstat_read(kc, ksp, NULL)} to refresh the named-data buffer pointed to by {@code ksp}.
         *
         * @param ksp segment addressing a {@code kstat_t}; should be reinterpreted to
         *            {@link LibKstatFunctions#KSTAT_LAYOUT}
         * @return {@code true} on success, {@code false} on failure
         */
        @GuardedBy("CHAIN")
        public boolean read(MemorySegment ksp) {
            try {
                int retry = 0;
                while (LibKstatFunctions.kstat_read(ctl, ksp, MemorySegment.NULL) == -1) {
                    if (++retry >= 5) {
                        return false;
                    }
                    // Exponential backoff mirroring the JNA KstatUtil (16/32/64/128/256 ms).
                    // Avoids busy-spinning on transient EAGAINs while still bounded.
                    try {
                        Thread.sleep(8L << retry);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
                return true;
            } catch (Throwable t) {
                LOG.warn("kstat_read failed", t);
                return false;
            }
        }

        /**
         * Convenience method for {@code kstat_lookup}. {@code module}/{@code name} may be {@code null} (wildcard);
         * {@code instance} may be {@code -1} (wildcard).
         *
         * @param module   module name or {@code null}
         * @param instance instance number or {@code -1}
         * @param name     kstat name or {@code null}
         * @return the first match reinterpreted to {@link LibKstatFunctions#KSTAT_LAYOUT}, or
         *         {@link MemorySegment#NULL}
         */
        @GuardedBy("CHAIN")
        public MemorySegment lookup(String module, int instance, String name) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment modSeg = module == null ? MemorySegment.NULL : arena.allocateFrom(module);
                MemorySegment nameSeg = name == null ? MemorySegment.NULL : arena.allocateFrom(name);
                MemorySegment hit = LibKstatFunctions.kstat_lookup(ctl, modSeg, instance, nameSeg);
                if (hit.address() == 0L) {
                    return MemorySegment.NULL;
                }
                return hit.reinterpret(KSTAT_LAYOUT.byteSize());
            } catch (Throwable t) {
                LOG.warn("kstat_lookup failed for {}:{}:{}", module, instance, name, t);
                return MemorySegment.NULL;
            }
        }

        /**
         * Convenience method for iterating all kstats matching {@code (module, instance, name)}.
         * {@code null}/{@code -1} mean wildcard.
         *
         * @param module   module name or {@code null}
         * @param instance instance number or {@code -1}
         * @param name     kstat name or {@code null}
         * @return all matching kstats (each reinterpreted to {@link LibKstatFunctions#KSTAT_LAYOUT})
         */
        @GuardedBy("CHAIN")
        public List<MemorySegment> lookupAll(String module, int instance, String name) {
            List<MemorySegment> matches = new ArrayList<>();
            MemorySegment ksp = lookup(module, instance, name);
            while (ksp.address() != 0L) {
                if ((module == null || module.equals(kstatModule(ksp)))
                        && (instance < 0 || instance == kstatInstance(ksp))
                        && (name == null || name.equals(kstatName(ksp)))) {
                    matches.add(ksp);
                }
                ksp = kstatNext(ksp);
            }
            return matches;
        }

        /**
         * Calls {@code kstat_chain_update(kc)}.
         *
         * @return the new KCID if changed, 0 if unchanged, -1 on failure
         */
        @GuardedBy("CHAIN")
        public int update() {
            try {
                return LibKstatFunctions.kstat_chain_update(ctl);
            } catch (Throwable t) {
                LOG.warn("kstat_chain_update failed", t);
                return -1;
            }
        }

        @Override
        public void close() {
            CHAIN.unlock();
        }
    }

    /**
     * Acquires the chain lock and returns a {@link KstatChain} bound to the (lazily opened) {@code kstat_ctl_t}. Caller
     * must close the returned object to release the lock.
     *
     * @return a locked chain wrapper
     */
    public static synchronized KstatChain openChain() {
        CHAIN.lock();
        if (kstatCtl.address() == 0L) {
            try {
                MemorySegment ctl = LibKstatFunctions.kstat_open();
                if (ctl.address() == 0L) {
                    CHAIN.unlock();
                    throw new IllegalStateException("kstat_open returned NULL");
                }
                kstatCtl = ctl;
            } catch (Throwable t) {
                CHAIN.unlock();
                throw new IllegalStateException("kstat_open failed", t);
            }
        }
        return new KstatChain(kstatCtl);
    }

    /**
     * Searches the kstat's named-data section for the record with the given name. Caller must hold the chain lock
     * (typically via an enclosing {@link KstatChain}) and must have called {@link KstatChain#read(MemorySegment)}
     * first.
     *
     * @param ksp  kstat reinterpreted to {@link LibKstatFunctions#KSTAT_LAYOUT}
     * @param name record key
     * @return the value as a string (formatted per data type), or empty if not found / unsupported type
     */
    public static String dataLookupString(MemorySegment ksp, String name) {
        byte type = kstatType(ksp);
        if (type != KSTAT_TYPE_NAMED && type != KSTAT_TYPE_TIMER) {
            throw new IllegalArgumentException("Not a kstat_named or kstat_timer kstat.");
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment data = LibKstatFunctions.kstat_data_lookup(ksp, nameSeg);
            if (data.address() == 0L) {
                LOG.debug("Failed to lookup kstat value for key {}", name);
                return "";
            }
            MemorySegment named = data.reinterpret(KSTAT_NAMED_LAYOUT.byteSize());
            byte dt = namedDataType(named);
            switch (dt) {
                case KSTAT_DATA_CHAR:
                    return namedValueChar(named);
                case KSTAT_DATA_INT32:
                    return Integer.toString(namedValueInt32(named));
                case KSTAT_DATA_UINT32:
                    return FormatUtil.toUnsignedString(namedValueInt32(named));
                case KSTAT_DATA_INT64:
                    return Long.toString(namedValueInt64(named));
                case KSTAT_DATA_UINT64:
                    return FormatUtil.toUnsignedString(namedValueInt64(named));
                case KSTAT_DATA_STRING:
                    return namedValueString(named);
                default:
                    LOG.error("Unimplemented kstat data type {}", dt);
                    return "";
            }
        } catch (Throwable t) {
            LOG.warn("kstat_data_lookup failed for key {}", name, t);
            return "";
        }
    }

    /**
     * Searches the kstat's named-data section for a numeric record. Non-numeric data types return {@code 0}.
     *
     * @param ksp  kstat reinterpreted to {@link LibKstatFunctions#KSTAT_LAYOUT}
     * @param name record key
     * @return the value as a long, or {@code 0} if not found / unsupported
     */
    public static long dataLookupLong(MemorySegment ksp, String name) {
        byte type = kstatType(ksp);
        if (type != KSTAT_TYPE_NAMED && type != KSTAT_TYPE_TIMER) {
            throw new IllegalArgumentException("Not a kstat_named or kstat_timer kstat.");
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(name);
            MemorySegment data = LibKstatFunctions.kstat_data_lookup(ksp, nameSeg);
            if (data.address() == 0L) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to lookup kstat value on {}:{}:{} for key {}", kstatModule(ksp),
                            kstatInstance(ksp), kstatName(ksp), name);
                }
                return 0L;
            }
            MemorySegment named = data.reinterpret(KSTAT_NAMED_LAYOUT.byteSize());
            byte dt = namedDataType(named);
            switch (dt) {
                case KSTAT_DATA_INT32:
                    return namedValueInt32(named);
                case KSTAT_DATA_UINT32:
                    return FormatUtil.getUnsignedInt(namedValueInt32(named));
                case KSTAT_DATA_INT64:
                case KSTAT_DATA_UINT64:
                    return namedValueInt64(named);
                default:
                    LOG.error("Unimplemented or non-numeric kstat data type {}", dt);
                    return 0L;
            }
        } catch (Throwable t) {
            LOG.warn("kstat_data_lookup failed for key {}", name, t);
            return 0L;
        }
    }

    /**
     * Convenience method to look up a kstat class via {@code ksp.ks_class}.
     *
     * @param ksp kstat reinterpreted to {@link LibKstatFunctions#KSTAT_LAYOUT}
     * @return the class string
     */
    public static String kstatClassOf(MemorySegment ksp) {
        return kstatClass(ksp);
    }
}
