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
package oshi.util.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.windows.Pdh;
import oshi.jna.platform.windows.Pdh.PdhFmtCounterValue;

/**
 * Provides access to open pdh queries and add/query counters
 * 
 * @author widdis[at]gmail[dot]com
 */
public class PdhUtil {
    private static final Logger LOG = LoggerFactory.getLogger(PdhUtil.class);

    private static final IntByReference PZERO = new IntByReference(0);

    /**
     * Open a pdh query
     * 
     * @param p
     *            pointer to the query
     * @return true if successful
     */
    public static boolean openQuery(PointerByReference p) {
        int pdhOpenQueryError = Pdh.INSTANCE.PdhOpenQuery(null, PZERO, p);
        if (pdhOpenQueryError != 0) {
            LOG.error("Failed to open PDH Query. Error code: {}", String.format("0x%08X", pdhOpenQueryError));
        }
        return pdhOpenQueryError == 0;
    }

    /**
     * Adds a pdh counter to a query
     * 
     * @param query
     *            Pointer to the query to add the counter
     * @param path
     *            String name of the PerfMon counter
     * @param p
     *            Pointer to the counter
     */
    public static void addCounter(PointerByReference query, String path, PointerByReference p) {
        int pdhAddCounterError = Pdh.INSTANCE.PdhAddEnglishCounterA(query.getValue(), path, PZERO, p);
        if (pdhAddCounterError != 0) {
            LOG.error("Failed to add PDH Counter: {}, Error code: {}", path,
                    String.format("0x%08X", pdhAddCounterError));
        }
    }

    /**
     * Update counters to values since the last call
     * 
     * @param query
     *            The query whose counters to update
     * @return True if successful
     */
    public static boolean updateCounters(PointerByReference query) {
        int ret = Pdh.INSTANCE.PdhCollectQueryData(query.getValue());
        if (ret != 0) {
            LOG.error("Failed to update counters. Error code: {}", String.format("0x%08X", ret));
            return false;
        }
        return true;
    }

    /**
     * Get value of pdh counter
     * 
     * @param counter
     *            The counter to get the value of
     * @return long value of the counter x 1000
     */
    public static long queryCounter(PointerByReference counter) {
        PdhFmtCounterValue counterValue = new PdhFmtCounterValue();
        int ret = Pdh.INSTANCE.PdhGetFormattedCounterValue(counter.getValue(), Pdh.PDH_FMT_LARGE | Pdh.PDH_FMT_1000,
                null, counterValue);
        if (ret != 0) {
            LOG.warn("Failed to get counter. Error code: {}", String.format("0x%08X", ret));
            return 0L;
        }
        return counterValue.value.largeValue;
    }
}