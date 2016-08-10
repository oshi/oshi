/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util.platform.unix.solaris;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import oshi.jna.platform.unix.solaris.LibKstat;
import oshi.jna.platform.unix.solaris.LibKstat.Kstat;
import oshi.jna.platform.unix.solaris.LibKstat.KstatCtl;
import oshi.jna.platform.unix.solaris.LibKstat.KstatNamed;
import oshi.util.FormatUtil;
import oshi.util.Util;

/**
 * Provides access to kstat information on Solaris
 *
 * @author widdis[at]gmail[dot]com
 */
public class KstatUtil {
    private static final Logger LOG = LoggerFactory.getLogger(KstatUtil.class);

    private static KstatCtl kc = LibKstat.INSTANCE.kstat_open();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LibKstat.INSTANCE.kstat_close(kc);
            }
        });
    }

    private KstatUtil() {
    }

    /**
     * Convenience method for kstat_data_lookup() with String return values.
     * Searches the kstat's data section for the record with the specified name.
     * This operation is valid only for kstat types which have named data
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
    public static String kstatDataLookupString(Kstat ksp, String name) {
        if (ksp.ks_type != LibKstat.KSTAT_TYPE_NAMED && ksp.ks_type != LibKstat.KSTAT_TYPE_TIMER) {
            throw new IllegalArgumentException("Not a kstat_named or kstat_timer kstat.");
        }
        Pointer p = LibKstat.INSTANCE.kstat_data_lookup(ksp, name);
        if (p == null) {
            LOG.error("Failed lo lookup kstat value for key {}", name);
            return "";
        }
        KstatNamed data = new KstatNamed(p);
        switch (data.data_type) {
        case LibKstat.KSTAT_DATA_CHAR:
            return new String(data.value.charc).trim();
        case LibKstat.KSTAT_DATA_INT32:
            return Integer.toString(data.value.i32);
        case LibKstat.KSTAT_DATA_UINT32:
            return Integer.toUnsignedString(data.value.ui32);
        case LibKstat.KSTAT_DATA_INT64:
            return Long.toString(data.value.i64);
        case LibKstat.KSTAT_DATA_UINT64:
            return Long.toUnsignedString(data.value.ui64);
        case LibKstat.KSTAT_DATA_STRING:
            return data.value.str.addr.getString(0);
        default:
            LOG.error("Unimplemented kstat data type {}", data.data_type);
            return "";
        }
    }

    /**
     * Convenience method for kstat_data_lookup() with numeric return values.
     * Searches the kstat's data section for the record with the specified name.
     * This operation is valid only for kstat types which have named data
     * records. Currently, only the KSTAT_TYPE_NAMED and KSTAT_TYPE_TIMER kstats
     * have named data records.
     *
     * @param ksp
     *            The kstat to search
     * @param name
     *            The key for the name-value pair, or name of the timer as
     *            applicable
     * @return The value as a long. If the data type is a character or string
     *         type, returns 0 and logs an error.
     */
    public static long kstatDataLookupLong(Kstat ksp, String name) {
        if (ksp.ks_type != LibKstat.KSTAT_TYPE_NAMED && ksp.ks_type != LibKstat.KSTAT_TYPE_TIMER) {
            throw new IllegalArgumentException("Not a kstat_named or kstat_timer kstat.");
        }
        Pointer p = LibKstat.INSTANCE.kstat_data_lookup(ksp, name);
        if (p == null) {
            LOG.error("Failed lo lookup kstat value on {}:{}:{} for key {}", new String(ksp.ks_module).trim(),
                    ksp.ks_instance, new String(ksp.ks_name).trim(), name);
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

    /**
     * Convenience method for kstat_read() which gets data from the kernel for
     * the kstat pointed to by ksp. ksp.ks_data is automatically allocated (or
     * reallocated) to be large enough to hold all of the data. ksp.ks_ndata is
     * set to the number of data fields, ksp.ks_data_size is set to the total
     * size of the data, and ksp.ks_snaptime is set to the high-resolution time
     * at which the data snapshot was taken.
     *
     * @param ksp
     *            The kstat from which to retrieve data
     * @return True if successful; false otherwise
     */
    public static boolean kstatRead(Kstat ksp) {
        int retry = 0;
        while (0 > LibKstat.INSTANCE.kstat_read(kc, ksp, null)) {
            if (LibKstat.EAGAIN != Native.getLastError() || 5 <= ++retry) {
                LOG.error("Failed to read kstat {}:{}:{}", new String(ksp.ks_module).trim(), ksp.ks_instance,
                        new String(ksp.ks_name).trim());
                return false;
            }
            Util.sleep(8 << retry);
        }
        return true;
    }

    /**
     * Convenience method for kstat_lookup(). Traverses the kstat chain,
     * searching for a kstat with the same ks_module, ks_instance, and ks_name
     * fields; this triplet uniquely identifies a kstat. If ks_module is NULL,
     * ks_instance is -1, or ks_name is NULL, then those fields will be ignored
     * in the search.
     *
     * @param module
     *            The module, or null to ignore
     * @param instance
     *            The instance, or -1 to ignore
     * @param name
     *            The name, or null to ignore
     * @return The first match of the requested Kstat structure if found, or
     *         null
     */
    public static Kstat kstatLookup(String module, int instance, String name) {
        int ret = LibKstat.INSTANCE.kstat_chain_update(kc);
        if (ret < 0) {
            LOG.error("Failed to update kstat chain");
            return null;
        }
        return LibKstat.INSTANCE.kstat_lookup(kc, module, instance, name);
    }

    /**
     * Convenience method for kstat_lookup(). Traverses the kstat chain,
     * searching for all kstats with the same ks_module, ks_instance, and
     * ks_name fields; this triplet uniquely identifies a kstat. If ks_module is
     * NULL, ks_instance is -1, or ks_name is NULL, then those fields will be
     * ignored in the search.
     *
     * @param module
     *            The module, or null to ignore
     * @param instance
     *            The instance, or -1 to ignore
     * @param name
     *            The name, or null to ignore
     * @return All matches of the requested Kstat structure if found, or an
     *         empty list otherwise
     */
    public static List<Kstat> kstatLookupAll(String module, int instance, String name) {
        List<Kstat> kstats = new ArrayList<>();
        int ret = LibKstat.INSTANCE.kstat_chain_update(kc);
        if (ret < 0) {
            LOG.error("Failed to update kstat chain");
            return kstats;
        }
        for (Kstat ksp = LibKstat.INSTANCE.kstat_lookup(kc, module, instance, name); ksp != null; ksp = ksp.next()) {
            if ((module == null || module.equals(new String(ksp.ks_module).trim()))
                    && (instance < 0 || instance == ksp.ks_instance)
                    && (name == null || name.equals(new String(ksp.ks_name).trim()))) {
                kstats.add(ksp);
            }
        }
        return kstats;
    }
}