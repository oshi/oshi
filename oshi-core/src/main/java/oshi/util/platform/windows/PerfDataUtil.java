/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.util.platform.windows;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.BaseTSD.DWORD_PTR; // NOSONAR
import com.sun.jna.platform.win32.Pdh;
import com.sun.jna.platform.win32.Pdh.PDH_RAW_COUNTER;
import com.sun.jna.platform.win32.PdhMsg;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinDef.LONGLONGByReference;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;

import oshi.jna.platform.windows.VersionHelpers;
import oshi.util.Util;

/**
 * Helper class to centralize the boilerplate portions of PDH counter setup and
 * allow applications to easily add, query, and remove counters.
 *
 * @author widdis[at]gmail[dot]com
 */
public class PerfDataUtil {
    /**
     * Instance to generate the PerfCounter class.
     */
    public static final PerfDataUtil INSTANCE = new PerfDataUtil();

    private static final Logger LOG = LoggerFactory.getLogger(PerfDataUtil.class);

    private static final DWORD_PTR PZERO = new DWORD_PTR(0);
    private static final DWORDByReference PDH_FMT_RAW = new DWORDByReference(new DWORD(Pdh.PDH_FMT_RAW));
    private static final Pdh PDH = Pdh.INSTANCE;

    private static final String HEX_ERROR_FMT = "0x%08X";
    private static final String LOG_COUNTER_NOT_EXISTS = "Counter does not exist: {}";
    private static final String LOG_COUNTER_RECREATE = "Removing and re-adding counter: {}";
    private static final String LOG_COUNTER_QUERY = "Querying counter: {}";

    // PDH timestamps are 1601 epoch, local time
    // Constants to convert to UTC millis
    private static final long EPOCH_DIFF = 11644473600000L;
    private static final int TZ_OFFSET = TimeZone.getDefault().getOffset(System.currentTimeMillis());

    // Maps to hold pointers to the relevant counter information
    private static final Map<PerfCounter, HANDLEByReference> counterMap = new ConcurrentHashMap<>();
    private static final Map<String, HANDLEByReference> queryMap = new ConcurrentHashMap<>();
    private static final Set<String> disabledQueries = new HashSet<>();

    // Is AddEnglishCounter available?
    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    public class PerfCounter {
        private String object;
        private String instance;
        private String counter;

        public PerfCounter(String objectName, String instanceName, String counterName) {
            this.object = objectName;
            this.instance = instanceName;
            this.counter = counterName;
        }

        /**
         * @return Returns the object.
         */
        public String getObject() {
            return object;
        }

        /**
         * @return Returns the instance.
         */
        public String getInstance() {
            return instance;
        }

        /**
         * @return Returns the counter.
         */
        public String getCounter() {
            return counter;
        }
    }

    private PerfDataUtil() {
        // Set up hook to close all queries on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                removeAllCounters();
            }
        });
    }

    /**
     * Create a Performance Counter
     *
     * @param object
     *            The object/path for the counter
     * @param instance
     *            The instance of the counter, or null if no instance
     * @param counter
     *            The counter name
     * @return A PerfCounter object encapsulating the object, instance, and
     *         counter
     */
    public static PerfCounter createCounter(String object, String instance, String counter) {
        return INSTANCE.new PerfCounter(object, instance, counter);
    }

    /**
     * Begin monitoring a Performance Data counter
     *
     * @param counter
     *            A PerfCounter object
     * @return True if the counter was successfully added.
     */
    public static boolean addCounterToQuery(PerfCounter counter) {
        HANDLEByReference q = openQuery(counter.object);
        if (q == null) {
            LOG.error("Failed to open a query for PDH object: {}", counter.object);
            return false;
        }
        HANDLEByReference p = new HANDLEByReference();
        String path = counterPath(counter);
        if (addCounter(q, path, p)) {
            counterMap.put(counter, p);
            return true;
        }
        return false;
    }

    /**
     * Stop monitoring a Performance Data counter
     *
     * @param counter
     *            A PerfCounter object
     * @return True if the counter was successfully removed.
     */
    public static boolean removeCounterFromQuery(PerfCounter counter) {
        if (counterMap.containsKey(counter)) {
            return WinError.ERROR_SUCCESS == PDH.PdhRemoveCounter(counterMap.get(counter).getValue());
        }
        return false;
    }

    /**
     * Update a counter, and all other counters on that object
     *
     * @param counter
     *            The counter whose object to update counters on
     * @return The timestamp for the update of all the counters, in milliseconds
     *         since the epoch, or 0 if the update failed
     */
    public static long updateQuery(PerfCounter counter) {
        if (disabledQueries.contains(counter.object)) {
            return 0L;
        }
        if (!queryMap.containsKey(counter.object) || !counterMap.containsKey(counter)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(LOG_COUNTER_NOT_EXISTS, counterPath(counter));
            }
            return 0L;
        }
        long timestamp = updateQueryTimestamp(queryMap.get(counter.object));
        if (timestamp == 0L) {
            LOG.error("Disabling future updates for {}.", counter.object);
            disabledQueries.add(counter.object);
            return 0L;
        }
        return timestamp;
    }

    /**
     * Update all counters on an object
     *
     * @param queryKey
     *            The counter object to update counters on
     * @return The timestamp for the update of all the counters, in milliseconds
     *         since the epoch, or 0 if the update failed
     */
    public static long updateQuery(String queryKey) {
        if (disabledQueries.contains(queryKey) || !queryMap.containsKey(queryKey)) {
            return 0L;
        }
        return updateQueryTimestamp(queryMap.get(queryKey));
    }

    /**
     * Query the raw counter value of a Performance Data counter. Further
     * mathematical manipulation/conversion is left to the caller.
     *
     * @param counter
     *            The counter to query
     * @return The raw value of the counter
     */
    public static long queryCounter(PerfCounter counter) {
        if (!queryMap.containsKey(counter.object) || !counterMap.containsKey(counter)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(LOG_COUNTER_NOT_EXISTS, counterPath(counter));
            }
            return 0;
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace(LOG_COUNTER_QUERY, counterPath(counter));
        }
        long value = queryCounter(counterMap.get(counter));
        if (value == PdhMsg.PDH_INVALID_HANDLE) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(LOG_COUNTER_RECREATE, counterPath(counter));
            }
            removeCounterFromQuery(counter);
            addCounterToQuery(counter);
        }
        return value < 0 ? 0 : value;
    }

    /**
     * Stop monitoring Performance Data counters for a particular queryKey and
     * release their resources
     *
     * @param queryKey
     *            The counter object to remove counters from
     */
    public static void removeAllCounters(String queryKey) {
        // Remove counters from query
        Iterator<Entry<PerfCounter, HANDLEByReference>> it = counterMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<PerfCounter, HANDLEByReference> entry = it.next();
            if (entry.getKey().object.equals(queryKey)) {
                PDH.PdhRemoveCounter(entry.getValue().getValue());
                it.remove();
            }
        }
        // Remove query
        HANDLEByReference query = queryMap.get(queryKey);
        if (query != null) {
            PDH.PdhCloseQuery(query.getValue());
        }
        queryMap.remove(queryKey);
        disabledQueries.remove(queryKey);
    }

    /**
     * Open a query for the given string, or confirm a query is already open for
     * that string. Multiple counters may be added to this string, but will all
     * be queried at the same time.
     *
     * @param objectName
     *            String to associate with the counter. Normally the English PDH
     *            object name.
     * @return A handle to the query, or null if an error occurred.
     */
    private static HANDLEByReference openQuery(String objectName) {
        if (queryMap.containsKey(objectName)) {
            return queryMap.get(objectName);
        }
        HANDLEByReference q = new HANDLEByReference();
        if (openQuery(q)) {
            queryMap.put(objectName, q);
            return q;
        }
        return null;
    }

    /**
     * Build a counter path
     *
     * counter A Counter object with the object, counter, and (optional)
     * instance
     *
     * @return A string representing the complete counter
     */
    private static String counterPath(PerfCounter counter) {
        StringBuilder sb = new StringBuilder();
        sb.append('\\').append(counter.object);
        if (counter.instance != null) {
            sb.append('(').append(counter.instance).append(')');
        }
        sb.append('\\').append(counter.counter);
        return sb.toString();
    }

    /**
     * Stop monitoring all Performance Data counters and release their resources
     */
    public static void removeAllCounters() {
        Set<String> queries = new HashSet<>(queryMap.keySet());
        for (String query : queries) {
            removeAllCounters(query);
        }
    }

    /**
     * Open a pdh query
     *
     * @param q
     *            pointer to the query
     * @return true if successful
     */
    private static boolean openQuery(HANDLEByReference q) {
        int pdhOpenQueryError = PDH.PdhOpenQuery(null, PZERO, q);
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
     * @return
     */
    private static boolean addCounter(WinNT.HANDLEByReference query, String path, WinNT.HANDLEByReference p) {
        int pdhAddCounterError;
        if (IS_VISTA_OR_GREATER) {
            // Localized version for Vista+
            pdhAddCounterError = PDH.PdhAddEnglishCounter(query.getValue(), path, PZERO, p);
        } else {
            // Only will work for English counters
            pdhAddCounterError = PDH.PdhAddCounter(query.getValue(), path, PZERO, p);
        }
        if (pdhAddCounterError != WinError.ERROR_SUCCESS && LOG.isWarnEnabled()) {
            LOG.warn("Failed to add PDH Counter: {}, Error code: {}", path,
                    String.format(HEX_ERROR_FMT, pdhAddCounterError));
        }
        return pdhAddCounterError == WinError.ERROR_SUCCESS;
    }

    /**
     * Get value of pdh counter
     *
     * @param counter
     *            The counter to get the value of
     * @return long value of the counter, or negative value representing an
     *         error code
     */
    private static long queryCounter(WinNT.HANDLEByReference counter) {
        PDH_RAW_COUNTER counterValue = new PDH_RAW_COUNTER();
        int ret = PDH.PdhGetRawCounterValue(counter.getValue(), PDH_FMT_RAW, counterValue);
        if (ret != WinError.ERROR_SUCCESS) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to get counter. Error code: {}", String.format(HEX_ERROR_FMT, ret));
            }
            // Return error code. Will be a negative value
            return ret;
        }
        return counterValue.FirstValue;
    }

    /**
     * Update a query and get the timestamp
     *
     * @param query
     *            The query to update all counters in
     * @return The update timestamp of the first counter in the query
     */
    private static long updateQueryTimestamp(WinNT.HANDLEByReference query) {
        LONGLONGByReference pllTimeStamp = new LONGLONGByReference();
        int ret;
        if (IS_VISTA_OR_GREATER) {
            ret = PDH.PdhCollectQueryDataWithTime(query.getValue(), pllTimeStamp);
        } else {
            ret = PDH.PdhCollectQueryData(query.getValue());
        }
        // Due to race condition, initial update may fail with PDH_NO_DATA.
        int retries = 0;
        while (ret == PdhMsg.PDH_NO_DATA && retries++ < 3) {
            // Exponential fallback.
            Util.sleep(1 << retries);
            if (IS_VISTA_OR_GREATER) {
                ret = PDH.PdhCollectQueryDataWithTime(query.getValue(), pllTimeStamp);
            } else {
                ret = PDH.PdhCollectQueryData(query.getValue());
            }
        }
        if (ret != WinError.ERROR_SUCCESS) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to update counter. Error code: {}", String.format(HEX_ERROR_FMT, ret));
            }
            return 0;
        }
        // Perf Counter timestamp is in local time
        if (IS_VISTA_OR_GREATER) {
            return filetimeToUtcMs(pllTimeStamp.getValue().longValue(), true);
        } else {
            // Approximate timestamp
            return System.currentTimeMillis();
        }

    }

    /**
     * Convert a long representing filetime (100-ns since 1601 epoch) to ms
     * since 1970 epoch
     *
     * @param filetime
     *            A 64-bit value equivalent to FILETIME
     * @param local
     *            True if converting from a local filetime (PDH counter); false
     *            if already UTC (WMI PerfRawData classes)
     * @return Equivalent milliseconds since the epoch
     */
    public static long filetimeToUtcMs(long filetime, boolean local) {
        return filetime / 10000L - EPOCH_DIFF - (local ? TZ_OFFSET : 0L);
    }
}