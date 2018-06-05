/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.util.platform.windows;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.BaseTSD.DWORD_PTR;
import com.sun.jna.platform.win32.Pdh;
import com.sun.jna.platform.win32.Pdh.PDH_RAW_COUNTER;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;

/**
 * Helper class to centralize the boilerplate portions of PDH counter setup and
 * allow applications to easily add, query, and remove counters.
 * 
 * @author widdis[at]gmail[dot]com
 */
public class PdhUtil {
    private static final Logger LOG = LoggerFactory.getLogger(PdhUtil.class);

    private static final DWORD_PTR PZERO = new DWORD_PTR(0);
    private static final DWORDByReference PDH_FMT_RAW = new DWORDByReference(new DWORD(Pdh.PDH_FMT_RAW));
    private static final PDH_RAW_COUNTER counterValue = new PDH_RAW_COUNTER();
    private static final Pdh PDH = Pdh.INSTANCE;

    // Maps to hold pointers to the relevant counter information
    private static final Map<String, HANDLEByReference> counterMap = new HashMap<>();
    private static final Map<String, HANDLEByReference> queryMap = new HashMap<>();

    private PdhUtil() {
        // Set up hook to close all queries on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (HANDLEByReference query : queryMap.values()) {
                    PDH.PdhCloseQuery(query.getValue());
                }
            }
        });
    }

    /**
     * Begin monitoring a Performance Data counter
     * 
     * @param counterString
     *            The counter to monitor
     * @return True if the counter has been successfully added or already exists
     */
    public static boolean addCounter(String counterString) {
        if (queryMap.containsKey(counterString)) {
            LOG.warn("Counter already exists: {}", counterString);
            return true;
        }
        HANDLEByReference q = new HANDLEByReference();
        if (PdhUtil.openQuery(q)) {
            HANDLEByReference p = new HANDLEByReference();
            PdhUtil.addCounter(q, counterString, p);
            counterMap.put(counterString, p);
            queryMap.put(counterString, q);
            return true;
        }
        return false;
    }

    /**
     * Stop monitoring a Performance Data counter
     * 
     * @param counterString
     *            The counter to stop monitoring
     * @return True if the counter is successfully removed or doesn't exist
     */
    public static boolean removeCounter(String counterString) {
        if (!queryMap.containsKey(counterString)) {
            LOG.warn("Counter does not exist: {}", counterString);
            return true;
        }
        PDH.PdhCloseQuery(queryMap.get(counterString).getValue());
        counterMap.remove(counterString);
        queryMap.remove(counterString);
        return true;
    }

    /**
     * Query the raw counter value of a Performance Data counter. Further
     * mathematical manipulation/conversion is left to the caller.
     * 
     * @param counterString
     *            The counter to query
     * @return The raw value of the counter
     */
    public static long queryCounter(String counterString) {
        if (!queryMap.containsKey(counterString)) {
            LOG.error("Counter does not exist: {}", counterString);
            return 0;
        }
        updateCounters(queryMap.get(counterString));
        return queryCounter(counterMap.get(counterString));
    }

    /**
     * Open a pdh query
     * 
     * @param p
     *            pointer to the query
     * @return true if successful
     */
    private static boolean openQuery(HANDLEByReference p) {
        int pdhOpenQueryError = PDH.PdhOpenQuery(null, PZERO, p);
        if (pdhOpenQueryError != WinError.ERROR_SUCCESS) {
            LOG.error("Failed to open PDH Query. Error code: {}", String.format("0x$08X", pdhOpenQueryError));
        }
        return pdhOpenQueryError == WinError.ERROR_SUCCESS;
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
    private static void addCounter(WinNT.HANDLEByReference query, String path, WinNT.HANDLEByReference p) {
        int pdhAddCounterError = PDH.PdhAddEnglishCounter(query.getValue(), path, PZERO, p);
        if (pdhAddCounterError != WinError.ERROR_SUCCESS) {
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
    private static boolean updateCounters(WinNT.HANDLEByReference query) {
        int ret = PDH.PdhCollectQueryData(query.getValue());
        if (ret != WinError.ERROR_SUCCESS) {
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
    private static long queryCounter(WinNT.HANDLEByReference counter) {
        int ret = PDH.PdhGetRawCounterValue(counter.getValue(), PDH_FMT_RAW, counterValue);
        if (ret != WinError.ERROR_SUCCESS) {
            LOG.warn("Failed to get counter. Error code: {}", String.format("0x%08X", ret));
            return 0L;
        }
        return counterValue.FirstValue;
    }
}