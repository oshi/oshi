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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.BaseTSD.DWORD_PTR; // NOSONAR
import com.sun.jna.platform.win32.Pdh;
import com.sun.jna.platform.win32.Pdh.PDH_RAW_COUNTER;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;

import oshi.jna.platform.windows.PdhUtil;

/**
 * Helper class to centralize the boilerplate portions of PDH counter setup and
 * allow applications to easily add, query, and remove counters.
 * 
 * @author widdis[at]gmail[dot]com
 */
public class PerfDataUtil {
    private static final Logger LOG = LoggerFactory.getLogger(PerfDataUtil.class);

    private static final DWORD_PTR PZERO = new DWORD_PTR(0);
    private static final DWORDByReference PDH_FMT_RAW = new DWORDByReference(new DWORD(Pdh.PDH_FMT_RAW));
    private static final PDH_RAW_COUNTER counterValue = new PDH_RAW_COUNTER();
    private static final Pdh PDH = Pdh.INSTANCE;

    private static final String HEX_ERROR_FMT = "0x%08X";
    private static final String LOG_COUNTER_NOT_EXISTS = "Counter does not exist: {}";

    // Maps to hold pointers to the relevant counter information
    private static final Map<String, HANDLEByReference> counterMap = new HashMap<>();
    private static final Map<String, HANDLEByReference> queryMap = new HashMap<>();

    // Regexp to match PDH counter string
    // Format is \Path(Instance)\Counter or \Path\Counter
    private static Pattern COUNTER_PATTERN = Pattern.compile("\\\\(.*?)(\\(.*\\))?\\\\(.*)");


    private PerfDataUtil() {
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
     * Translate an English counter path to its locale-specific string
     * 
     * @param englishPath
     *            The english path of the counter
     * @return The path of the counter in the machine's locale
     */
    private static String localizeCounterPath(String englishPath) {
        Matcher match = COUNTER_PATTERN.matcher(englishPath);
        if (match.matches()) {
            StringBuilder sb = new StringBuilder();
            sb.append('\\');
            sb.append(PdhUtil.PdhLookupPerfNameByIndex(null, PdhUtil.PdhLookupPerfIndexByEnglishName(match.group(1))));
            if (match.group(2) != null) {
                sb.append(match.group(2));
            }
            sb.append('\\');
            sb.append(PdhUtil.PdhLookupPerfNameByIndex(null, PdhUtil.PdhLookupPerfIndexByEnglishName(match.group(3))));
            return sb.toString();
        }
        return englishPath;
    }

    /**
     * Report if a performance counter is being monitored
     * 
     * @param counterString
     *            The counter to monitor
     * @return True if the counter already exists
     */
    public static boolean isCounter(String counterString) {
        return counterMap.containsKey(counterString);
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
        if (openQuery(q)) {
            HANDLEByReference p = new HANDLEByReference();
            addCounter(q, localizeCounterPath(counterString), p);
            counterMap.put(counterString, p);
            queryMap.put(counterString, q);
            return true;
        }
        return false;
    }

    /**
     * Begin monitoring a 2D array of Performance Data counters
     * 
     * @param name
     *            A unique name that will always correspond to the same String
     *            array
     * @param counterStringArray
     *            A 2D array of string counter names to monitor
     * @return True if the counters have been successfully added or already
     *         exist
     */
    public static boolean addCounter2DArray(String name, String[][] counterStringArray) {
        if (queryMap.containsKey(name)) {
            LOG.warn("Counters already exists: {}", name);
            return true;
        }
        if (counterStringArray.length == 0 || counterStringArray[0].length == 0) {
            LOG.error("This array has a zero dimension: {}", name);
            return false;
        }
        HANDLEByReference q = new HANDLEByReference();
        if (openQuery(q)) {
            for (int i = 0; i < counterStringArray.length; i++) {
                for (int j = 0; j < counterStringArray[i].length; j++) {
                    if (counterStringArray[i][j] != null) {
                        HANDLEByReference p = new HANDLEByReference();
                        addCounter(q, counterStringArray[i][j], p);
                        counterMap.put(counterStringArray[i][j], p);
                    }
                }
            }
            queryMap.put(name, q);
            return true;
        }
        return false;
    }

    /**
     * Stop monitoring a Performance Data counter
     * 
     * @param counterString
     *            The counter to stop monitoring
     */
    public static void removeCounter(String counterString) {
        if (queryMap.containsKey(counterString)) {
            PDH.PdhCloseQuery(queryMap.get(counterString).getValue());
            counterMap.remove(counterString);
            queryMap.remove(counterString);
        } else {
            LOG.warn(LOG_COUNTER_NOT_EXISTS, counterString);
        }
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
            LOG.error(LOG_COUNTER_NOT_EXISTS, counterString);
            return 0;
        }
        updateCounters(queryMap.get(counterString));
        return queryCounter(counterMap.get(counterString));
    }

    /**
     * Get the timestamp of a raw counter value of a Performance Data counter.
     * Does not update the counter, and should normally be called after querying
     * the counter.
     * 
     * @param counterString
     *            The counter to query
     * @return The raw value of the counter
     */
    public static long queryCounterTimestamp(String counterString) {
        if (!queryMap.containsKey(counterString)) {
            LOG.error(LOG_COUNTER_NOT_EXISTS, counterString);
            return 0;
        }
        return queryCounterTimestamp(counterMap.get(counterString)).toDWordLong().longValue() / 10000L;
    }

    /**
     * Query the raw counter value of an array of Performance Data counters.
     * Further mathematical manipulation/conversion is left to the caller.
     * 
     * @param name
     *            A unique name that will always correspond to the same String
     *            array
     * @param counterStringArray
     *            A 2D array of string counter names to monitor
     * @return The raw values of the counters corresponding to the string
     */
    public static long[][] queryCounter2DArray(String name, String[][] counterStringArray) {
        if (!queryMap.containsKey(name)) {
            LOG.error(LOG_COUNTER_NOT_EXISTS, name);
            return new long[0][0];
        }
        if (counterStringArray.length == 0 || counterStringArray[0].length == 0) {
            LOG.error("This array has a zero dimension: {}", name);
            return new long[0][0];
        }
        updateCounters(queryMap.get(name));
        long[][] values = new long[counterStringArray.length][counterStringArray[0].length];
        for (int i = 0; i < counterStringArray.length; i++) {
            for (int j = 0; j < counterStringArray[i].length; j++) {
                if (counterStringArray[i][j] != null) {
                    values[i][j] = queryCounter(counterMap.get(counterStringArray[i][j]));
                }
            }
        }
        return values;
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
        if (pdhOpenQueryError != WinError.ERROR_SUCCESS && LOG.isErrorEnabled()) {
            LOG.error("Failed to open PDH Query. Error code: {}", String.format(HEX_ERROR_FMT, pdhOpenQueryError));
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
        if (pdhAddCounterError != WinError.ERROR_SUCCESS && LOG.isErrorEnabled()) {
            LOG.error("Failed to add PDH Counter: {}, Error code: {}", path,
                    String.format(HEX_ERROR_FMT, pdhAddCounterError));
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
        if (ret != WinError.ERROR_SUCCESS && LOG.isErrorEnabled()) {
            LOG.error("Failed to update counters. Error code: {}", String.format(HEX_ERROR_FMT, ret));
            return false;
        }
        return true;
    }

    /**
     * Get value of pdh counter
     * 
     * @param counter
     *            The counter to get the value of
     * @return long value of the counter
     */
    private static long queryCounter(WinNT.HANDLEByReference counter) {
        int ret = PDH.PdhGetRawCounterValue(counter.getValue(), PDH_FMT_RAW, counterValue);
        if (ret != WinError.ERROR_SUCCESS && LOG.isErrorEnabled()) {
            LOG.warn("Failed to get counter. Error code: {}", String.format(HEX_ERROR_FMT, ret));
            return 0L;
        }
        return counterValue.FirstValue;
    }

    /**
     * Get timestamp of pdh counter
     * 
     * @param counter
     *            The counter to get the value of
     * @return FILETIME value of the counter. This is in 100-ns increments and
     *         uses the 1601 Epoch.
     */
    private static FILETIME queryCounterTimestamp(WinNT.HANDLEByReference counter) {
        int ret = PDH.PdhGetRawCounterValue(counter.getValue(), PDH_FMT_RAW, counterValue);
        if (ret != WinError.ERROR_SUCCESS && LOG.isErrorEnabled()) {
            LOG.warn("Failed to get counter. Error code: {}", String.format(HEX_ERROR_FMT, ret));
            return new FILETIME();
        }
        return counterValue.TimeStamp;
    }
}