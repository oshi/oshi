/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.util.platform.unix.solaris;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native; // NOSONAR
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.solaris.LibKstat;
import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;
import com.sun.jna.platform.unix.solaris.LibKstat.KstatCtl;
import com.sun.jna.platform.unix.solaris.LibKstat.KstatNamed;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FormatUtil;
import oshi.util.Util;

/**
 * Provides access to kstat information on Solaris
 */
@ThreadSafe
public final class KstatUtil {

    private static final Logger LOG = LoggerFactory.getLogger(KstatUtil.class);

    private static final LibKstat KS = LibKstat.INSTANCE;

    // Opens the kstat chain. Automatically closed on exit.
    // Only one thread may access the chain at any time, so we wrap this object in
    // the KstatChain class which locks the class until closed.
    private static final KstatCtl KC = KS.kstat_open();
    private static final ReentrantLock CHAIN = new ReentrantLock();

    private KstatUtil() {
    }

    /**
     * A copy of the Kstat chain, encapsulating a {@code kstat_ctl_t} object. Only
     * one thread may actively use this object at any time.
     * <p>
     * Instantiating this object is accomplished using the
     * {@link KstatUtil#openChain} method. It locks and updates the chain and is the
     * equivalent of calling {@link LibKstat#kstat_open}. The control object should
     * be closed with {@link #close}, the equivalent of calling
     * {@link LibKstat#kstat_close}
     */
    public static final class KstatChain implements AutoCloseable {

        private KstatChain() {
            CHAIN.lock();
            this.update();
        }

        /**
         * Convenience method for {@link LibKstat#kstat_read} which gets data from the
         * kernel for the kstat pointed to by {@code ksp}. {@code ksp.ks_data} is
         * automatically allocated (or reallocated) to be large enough to hold all of
         * the data. {@code ksp.ks_ndata} is set to the number of data fields,
         * {@code ksp.ks_data_size} is set to the total size of the data, and
         * ksp.ks_snaptime is set to the high-resolution time at which the data snapshot
         * was taken.
         *
         * @param ksp
         *            The kstat from which to retrieve data
         * @return {@code true} if successful; {@code false} otherwise
         */
        public boolean read(Kstat ksp) {
            int retry = 0;
            while (0 > KS.kstat_read(KC, ksp, null)) {
                if (LibKstat.EAGAIN != Native.getLastError() || 5 <= ++retry) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to read kstat {}:{}:{}",
                                new String(ksp.ks_module, StandardCharsets.US_ASCII).trim(), ksp.ks_instance,
                                new String(ksp.ks_name, StandardCharsets.US_ASCII).trim());
                    }
                    return false;
                }
                Util.sleep(8 << retry);
            }
            return true;
        }

        /**
         * Convenience method for {@link LibKstat#kstat_lookup}. Traverses the kstat
         * chain, searching for a kstat with the same {@code module}, {@code instance},
         * and {@code name} fields; this triplet uniquely identifies a kstat. If
         * {@code module} is {@code null}, {@code instance} is -1, or {@code name} is
         * {@code null}, then those fields will be ignored in the search.
         *
         * @param module
         *            The module, or null to ignore
         * @param instance
         *            The instance, or -1 to ignore
         * @param name
         *            The name, or null to ignore
         * @return The first match of the requested Kstat structure if found, or
         *         {@code null}
         */
        public Kstat lookup(String module, int instance, String name) {
            return KS.kstat_lookup(KC, module, instance, name);
        }

        /**
         * Convenience method for {@link LibKstat#kstat_lookup}. Traverses the kstat
         * chain, searching for all kstats with the same {@code module},
         * {@code instance}, and {@code name} fields; this triplet uniquely identifies a
         * kstat. If {@code module} is {@code null}, {@code instance} is -1, or
         * {@code name} is {@code null}, then those fields will be ignored in the
         * search.
         *
         * @param module
         *            The module, or null to ignore
         * @param instance
         *            The instance, or -1 to ignore
         * @param name
         *            The name, or null to ignore
         * @return All matches of the requested Kstat structure if found, or an empty
         *         list otherwise
         */
        public List<Kstat> lookupAll(String module, int instance, String name) {
            List<Kstat> kstats = new ArrayList<>();
            for (Kstat ksp = KS.kstat_lookup(KC, module, instance, name); ksp != null; ksp = ksp.next()) {
                if ((module == null || module.equals(new String(ksp.ks_module, StandardCharsets.US_ASCII).trim()))
                        && (instance < 0 || instance == ksp.ks_instance)
                        && (name == null || name.equals(new String(ksp.ks_name, StandardCharsets.US_ASCII).trim()))) {
                    kstats.add(ksp);
                }
            }
            return kstats;
        }

        /**
         * Convenience method for {@link LibKstat#kstat_chain_update}. Brings this kstat
         * header chain in sync with that of the kernel.
         * <p>
         * This function compares the kernel's current kstat chain ID(KCID), which is
         * incremented every time the kstat chain changes, to this object's KCID.
         *
         * @return the new KCID if the kstat chain has changed, 0 if it hasn't, or -1 on
         *         failure.
         */
        public int update() {
            return KS.kstat_chain_update(KC);
        }

        /**
         * Release the lock on the chain.
         */
        @Override
        public void close() {
            CHAIN.unlock();
        }
    }

    /**
     * Create a copy of the Kstat chain and lock it for use by this object.
     *
     * @return A locked copy of the chain. It should be unlocked/released when you
     *         are done with it with {@link KstatChain#close()}.
     */
    public static KstatChain openChain() {
        return new KstatChain();
    }

    /**
     * Convenience method for {@link LibKstat#kstat_data_lookup} with String return
     * values. Searches the kstat's data section for the record with the specified
     * name. This operation is valid only for kstat types which have named data
     * records. Currently, only the KSTAT_TYPE_NAMED and KSTAT_TYPE_TIMER kstats
     * have named data records.
     *
     * @param ksp
     *            The kstat to search
     * @param name
     *            The key for the name-value pair, or name of the timer as
     *            applicable
     * @return The value as a String.
     */
    public static String dataLookupString(Kstat ksp, String name) {
        if (ksp.ks_type != LibKstat.KSTAT_TYPE_NAMED && ksp.ks_type != LibKstat.KSTAT_TYPE_TIMER) {
            throw new IllegalArgumentException("Not a kstat_named or kstat_timer kstat.");
        }
        Pointer p = KS.kstat_data_lookup(ksp, name);
        if (p == null) {
            LOG.error("Failed lo lookup kstat value for key {}", name);
            return "";
        }
        KstatNamed data = new KstatNamed(p);
        switch (data.data_type) {
        case LibKstat.KSTAT_DATA_CHAR:
            return new String(data.value.charc, StandardCharsets.UTF_8).trim();
        case LibKstat.KSTAT_DATA_INT32:
            return Integer.toString(data.value.i32);
        case LibKstat.KSTAT_DATA_UINT32:
            return FormatUtil.toUnsignedString(data.value.ui32);
        case LibKstat.KSTAT_DATA_INT64:
            return Long.toString(data.value.i64);
        case LibKstat.KSTAT_DATA_UINT64:
            return FormatUtil.toUnsignedString(data.value.ui64);
        case LibKstat.KSTAT_DATA_STRING:
            return data.value.str.addr.getString(0);
        default:
            LOG.error("Unimplemented kstat data type {}", data.data_type);
            return "";
        }
    }

    /**
     * Convenience method for {@link LibKstat#kstat_data_lookup} with numeric return
     * values. Searches the kstat's data section for the record with the specified
     * name. This operation is valid only for kstat types which have named data
     * records. Currently, only the KSTAT_TYPE_NAMED and KSTAT_TYPE_TIMER kstats
     * have named data records.
     *
     * @param ksp
     *            The kstat to search
     * @param name
     *            The key for the name-value pair, or name of the timer as
     *            applicable
     * @return The value as a long. If the data type is a character or string type,
     *         returns 0 and logs an error.
     */
    public static long dataLookupLong(Kstat ksp, String name) {
        if (ksp.ks_type != LibKstat.KSTAT_TYPE_NAMED && ksp.ks_type != LibKstat.KSTAT_TYPE_TIMER) {
            throw new IllegalArgumentException("Not a kstat_named or kstat_timer kstat.");
        }
        Pointer p = KS.kstat_data_lookup(ksp, name);
        if (p == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed lo lookup kstat value on {}:{}:{} for key {}",
                        new String(ksp.ks_module, StandardCharsets.US_ASCII).trim(), ksp.ks_instance,
                        new String(ksp.ks_name, StandardCharsets.US_ASCII).trim(), name);
            }
            return 0L;
        }
        KstatNamed data = new KstatNamed(p);
        switch (data.data_type) {
        case LibKstat.KSTAT_DATA_INT32:
            return data.value.i32;
        case LibKstat.KSTAT_DATA_UINT32:
            return FormatUtil.getUnsignedInt(data.value.ui32);
        case LibKstat.KSTAT_DATA_INT64:
            return data.value.i64;
        case LibKstat.KSTAT_DATA_UINT64:
            return data.value.ui64;
        default:
            LOG.error("Unimplemented or non-numeric kstat data type {}", data.data_type);
            return 0L;
        }
    }
}
