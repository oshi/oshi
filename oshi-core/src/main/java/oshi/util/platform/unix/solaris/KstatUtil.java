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
            return ((KstatNamed.UNION.STR) data.value.str).addr.ptr.getString(0);
        default:
            LOG.error("Unimplemented kstat data type {}", data.data_type);
            return "";
        }
    }

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

    public static Kstat kstatLookup(String module, int instance, String name) {
        int ret = LibKstat.INSTANCE.kstat_chain_update(kc);
        if (ret < 0) {
            LOG.error("Failed to update kstat chain");
            return null;
        }
        Kstat ksp = LibKstat.INSTANCE.kstat_lookup(kc, module, instance, name);
        return ksp;
    }
}