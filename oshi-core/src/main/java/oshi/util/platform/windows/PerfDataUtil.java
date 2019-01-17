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
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
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

        /**
         * Returns the path for this counter
         * 
         * @return A string representing the counter path
         */
        public String getCounterPath() {
            StringBuilder sb = new StringBuilder();
            sb.append('\\').append(object);
            if (instance != null) {
                sb.append('(').append(instance).append(')');
            }
            sb.append('\\').append(counter);
            return sb.toString();
        }
    }

    private PerfDataUtil() {
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
     * Update a query and get the timestamp
     *
     * @param query
     *            The query to update all counters in
     * @return The update timestamp of the first counter in the query
     */
    public static long updateQueryTimestamp(WinNT.HANDLEByReference query) {
        LONGLONGByReference pllTimeStamp = new LONGLONGByReference();
        int ret = IS_VISTA_OR_GREATER ? PDH.PdhCollectQueryDataWithTime(query.getValue(), pllTimeStamp)
                : PDH.PdhCollectQueryData(query.getValue());
        // Due to race condition, initial update may fail with PDH_NO_DATA.
        int retries = 0;
        while (ret == PdhMsg.PDH_NO_DATA && retries++ < 3) {
            // Exponential fallback.
            Util.sleep(1 << retries);
            ret = IS_VISTA_OR_GREATER ? PDH.PdhCollectQueryDataWithTime(query.getValue(), pllTimeStamp)
                    : PDH.PdhCollectQueryData(query.getValue());
        }
        if (ret != WinError.ERROR_SUCCESS) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to update counter. Error code: {}", String.format(FormatUtil.formatError(ret)));
            }
            return 0L;
        }
        // Perf Counter timestamp is in local time
        return IS_VISTA_OR_GREATER ? ParseUtil.filetimeToUtcMs(pllTimeStamp.getValue().longValue(), true)
                : System.currentTimeMillis();
    }

    /**
     * Open a pdh query
     *
     * @param q
     *            pointer to the query
     * @return true if successful
     */
    public static boolean openQuery(HANDLEByReference q) {
        int ret = PDH.PdhOpenQuery(null, PZERO, q);
        if (ret != WinError.ERROR_SUCCESS) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to open PDH Query. Error code: {}", String.format(FormatUtil.formatError(ret)));
            }
            return false;
        }
        return true;
    }

    /**
     * Close a pdh query
     * 
     * @param q
     *            pointer to the query
     * @return true if successful
     */
    public static boolean closeQuery(HANDLEByReference q) {
        return WinError.ERROR_SUCCESS == PDH.PdhCloseQuery(q.getValue());
    }

    /**
     * Get value of pdh counter
     *
     * @param counter
     *            The counter to get the value of
     * @return long value of the counter, or negative value representing an
     *         error code
     */
    public static long queryCounter(WinNT.HANDLEByReference counter) {
        PDH_RAW_COUNTER counterValue = new PDH_RAW_COUNTER();
        int ret = PDH.PdhGetRawCounterValue(counter.getValue(), PDH_FMT_RAW, counterValue);
        if (ret != WinError.ERROR_SUCCESS) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to get counter. Error code: {}", String.format(FormatUtil.formatError(ret)));
            }
            return ret;
        }
        return counterValue.FirstValue;
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
     * @return true if successful
     */
    public static boolean addCounter(WinNT.HANDLEByReference query, String path, WinNT.HANDLEByReference p) {
        int ret = IS_VISTA_OR_GREATER ? PDH.PdhAddEnglishCounter(query.getValue(), path, PZERO, p)
                : PDH.PdhAddCounter(query.getValue(), path, PZERO, p);
        if (ret != WinError.ERROR_SUCCESS) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to add PDH Counter: {}, Error code: {}", path,
                        String.format(FormatUtil.formatError(ret)));
            }
            return false;
        }
        return true;
    }

    /**
     * Remove a pdh counter
     *
     * @param p
     *            pointer to the counter
     * @return true if successful
     */
    public static boolean removeCounter(HANDLEByReference p) {
        return WinError.ERROR_SUCCESS == PDH.PdhRemoveCounter(p.getValue());
    }
}