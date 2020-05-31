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
package oshi.util.platform.windows;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.WinNT.HANDLEByReference; // NOSONAR

import oshi.annotation.concurrent.NotThreadSafe;
import oshi.util.FormatUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;

/**
 * Utility to handle Performance Counter Queries
 * <p>
 * Not thread safe. Each query handler should only be used in a single thread.
 */
@NotThreadSafe
public final class PerfCounterQueryHandler implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PerfCounterQueryHandler.class);

    // Map of counter handles
    private Map<PerfCounter, HANDLEByReference> counterHandleMap = new HashMap<>();
    // The query handle
    private HANDLEByReference queryHandle = null;

    /**
     * Begin monitoring a Performance Data counter.
     *
     * @param counter
     *            A PerfCounter object.
     * @return True if the counter was successfully added to the query.
     */
    public boolean addCounterToQuery(PerfCounter counter) {
        // Open a new query or get the handle to an existing one
        if (this.queryHandle == null) {
            this.queryHandle = new HANDLEByReference();
            if (!PerfDataUtil.openQuery(this.queryHandle)) {
                LOG.error("Failed to open a query for PDH object: {}", counter.getObject());
                this.queryHandle = null;
                return false;
            }
        }
        // Get a new handle for the counter
        HANDLEByReference p = new HANDLEByReference();
        if (!PerfDataUtil.addCounter(this.queryHandle, counter.getCounterPath(), p)) {
            LOG.error("Failed to add counter for PDH object: {}", counter.getObject());
            return false;
        }
        counterHandleMap.put(counter, p);
        return true;
    }

    /**
     * Stop monitoring a Performance Data counter.
     *
     * @param counter
     *            A PerfCounter object
     * @return True if the counter was successfully removed.
     */
    public boolean removeCounterFromQuery(PerfCounter counter) {
        boolean success = false;
        HANDLEByReference href = counterHandleMap.remove(counter);
        // null if handle wasn't present
        if (href != null) {
            success = PerfDataUtil.removeCounter(href);
        }
        if (counterHandleMap.isEmpty()) {
            PerfDataUtil.closeQuery(queryHandle);
            queryHandle = null;
        }
        return success;
    }

    /**
     * Stop monitoring all Performance Data counters and release their resources
     */
    public void removeAllCounters() {
        // Remove all counters from counterHandle map
        for (HANDLEByReference href : counterHandleMap.values()) {
            PerfDataUtil.removeCounter(href);
        }
        counterHandleMap.clear();
        // Remove query
        if (this.queryHandle != null) {
            PerfDataUtil.closeQuery(this.queryHandle);
        }
        this.queryHandle = null;
    }

    /**
     * Update all counters on this query.
     *
     * @return The timestamp for the update of all the counters, in milliseconds
     *         since the epoch, or 0 if the update failed
     */
    public long updateQuery() {
        if (queryHandle == null) {
            LOG.error("Query does not exist to update.");
            return 0L;
        }
        return PerfDataUtil.updateQueryTimestamp(queryHandle);
    }

    /**
     * Query the raw counter value of a Performance Data counter. Further
     * mathematical manipulation/conversion is left to the caller.
     *
     * @param counter
     *            The counter to query
     * @return The raw value of the counter
     */
    public long queryCounter(PerfCounter counter) {
        if (!counterHandleMap.containsKey(counter)) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Counter {} does not exist to query.", counter.getCounterPath());
            }
            return 0;
        }
        long value = PerfDataUtil.queryCounter(counterHandleMap.get(counter));
        if (value < 0) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Error querying counter {}: {}", counter.getCounterPath(),
                        String.format(FormatUtil.formatError((int) value)));
            }
            return 0L;
        }
        return value;
    }

    @Override
    public void close() {
        removeAllCounters();
    }
}
