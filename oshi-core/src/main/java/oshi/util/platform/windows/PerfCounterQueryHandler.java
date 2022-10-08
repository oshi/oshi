/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.NotThreadSafe;
import oshi.jna.ByRef.CloseableHANDLEByReference;
import oshi.util.FormatUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;

/**
 * Utility to handle Performance Counter Queries
 * <p>
 * This class is not thread safe. Each query handler instance should only be used in a single thread, preferably in a
 * try-with-resources block.
 */
@NotThreadSafe
public final class PerfCounterQueryHandler implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PerfCounterQueryHandler.class);

    // Map of counter handles
    private Map<PerfCounter, CloseableHANDLEByReference> counterHandleMap = new HashMap<>();
    // The query handle
    private CloseableHANDLEByReference queryHandle = null;

    /**
     * Begin monitoring a Performance Data counter.
     *
     * @param counter A PerfCounter object.
     * @return True if the counter was successfully added to the query.
     */
    public boolean addCounterToQuery(PerfCounter counter) {
        // Open a new query or get the handle to an existing one
        if (this.queryHandle == null) {
            this.queryHandle = new CloseableHANDLEByReference();
            if (!PerfDataUtil.openQuery(this.queryHandle)) {
                LOG.warn("Failed to open a query for PDH counter: {}", counter.getCounterPath());
                this.queryHandle.close();
                this.queryHandle = null;
                return false;
            }
        }
        // Get a new handle for the counter
        CloseableHANDLEByReference p = new CloseableHANDLEByReference();
        if (!PerfDataUtil.addCounter(this.queryHandle, counter.getCounterPath(), p)) {
            LOG.warn("Failed to add counter for PDH counter: {}", counter.getCounterPath());
            p.close();
            return false;
        }
        counterHandleMap.put(counter, p);
        return true;
    }

    /**
     * Stop monitoring a Performance Data counter.
     *
     * @param counter A PerfCounter object
     * @return True if the counter was successfully removed.
     */
    public boolean removeCounterFromQuery(PerfCounter counter) {
        boolean success = false;
        try (CloseableHANDLEByReference href = counterHandleMap.remove(counter)) {
            // null if handle wasn't present
            if (href != null) {
                success = PerfDataUtil.removeCounter(href);
            }
        }
        if (counterHandleMap.isEmpty()) {
            PerfDataUtil.closeQuery(this.queryHandle);
            this.queryHandle.close();
            this.queryHandle = null;
        }
        return success;
    }

    /**
     * Stop monitoring all Performance Data counters and release their resources
     */
    public void removeAllCounters() {
        // Remove all counters from counterHandle map
        for (CloseableHANDLEByReference href : counterHandleMap.values()) {
            PerfDataUtil.removeCounter(href);
            href.close();
        }
        counterHandleMap.clear();
        // Remove query
        if (this.queryHandle != null) {
            PerfDataUtil.closeQuery(this.queryHandle);
            this.queryHandle.close();
            this.queryHandle = null;
        }
    }

    /**
     * Update all counters on this query.
     *
     * @return The timestamp for the update of all the counters, in milliseconds since the epoch, or 0 if the update
     *         failed
     */
    public long updateQuery() {
        if (this.queryHandle == null) {
            LOG.warn("Query does not exist to update.");
            return 0L;
        }
        return PerfDataUtil.updateQueryTimestamp(queryHandle);
    }

    /**
     * Query the raw counter value of a Performance Data counter. Further mathematical manipulation/conversion is left
     * to the caller.
     *
     * @param counter The counter to query
     * @return The raw value of the counter
     */
    public long queryCounter(PerfCounter counter) {
        if (!counterHandleMap.containsKey(counter)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Counter {} does not exist to query.", counter.getCounterPath());
            }
            return 0;
        }
        long value = counter.isBaseCounter() ? PerfDataUtil.querySecondCounter(counterHandleMap.get(counter))
                : PerfDataUtil.queryCounter(counterHandleMap.get(counter));
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
