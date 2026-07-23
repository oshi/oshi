/*
 * Copyright 2018-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import static oshi.util.LogLevel.ERROR;
import static oshi.util.LogLevel.WARN;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.BaseTSD.DWORD_PTR;
import com.sun.jna.platform.win32.Pdh;
import com.sun.jna.platform.win32.PdhMsg;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableLONGLONGByReference;
import oshi.jna.Struct.CloseablePdhRawCounter;
import oshi.util.FormatUtil;
import oshi.util.LogLevel;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * Helper class to centralize the boilerplate portions of PDH counter setup and allow applications to easily add, query,
 * and remove counters.
 */
@ThreadSafe
public final class PerfDataUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PerfDataUtil.class);

    private static final DWORD_PTR PZERO = new DWORD_PTR(0);
    private static final DWORDByReference PDH_FMT_RAW = new DWORDByReference(new DWORD(Pdh.PDH_FMT_RAW));
    private static final Pdh PDH = Pdh.INSTANCE;

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    private PerfDataUtil() {
    }

    /**
     * Update a query and get the timestamp
     *
     * @param query The query to update all counters in
     * @return The update timestamp of the first counter in the query
     */
    public static long updateQueryTimestamp(HANDLEByReference query) {
        try (CloseableLONGLONGByReference pllTimeStamp = new CloseableLONGLONGByReference()) {
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
            if (ret == WinError.ERROR_SUCCESS) {
                // Perf Counter timestamp is in local time
                return IS_VISTA_OR_GREATER ? ParseUtil.filetimeToUtcMs(pllTimeStamp.getValue().longValue(), true)
                        : System.currentTimeMillis();
            }
            return handleError(ret, WARN, "Failed to update counter.", 0L);
        }
    }

    /**
     * Open a pdh query
     *
     * @param q pointer to the query
     * @return true if successful
     */
    public static boolean openQuery(HANDLEByReference q) {
        int ret = PDH.PdhOpenQuery(null, PZERO, q);
        if (ret == WinError.ERROR_SUCCESS) {
            return true;
        }
        return handleError(ret, ERROR, "Failed to open PDH Query.", false);
    }

    /**
     * Close a pdh query
     *
     * @param q pointer to the query
     * @return true if successful
     */
    public static boolean closeQuery(HANDLEByReference q) {
        return WinError.ERROR_SUCCESS == PDH.PdhCloseQuery(q.getValue());
    }

    /**
     * Get value of pdh counter
     *
     * @param counter The counter to get the value of
     * @return long value of the counter, or negative value representing an error code
     */
    public static long queryCounter(HANDLEByReference counter) {
        try (CloseablePdhRawCounter counterValue = new CloseablePdhRawCounter()) {
            int ret = PDH.PdhGetRawCounterValue(counter.getValue(), PDH_FMT_RAW, counterValue);
            if (ret == WinError.ERROR_SUCCESS) {
                return counterValue.FirstValue;
            }
            return handleError(ret, WARN, "Failed to get counter.", (long) ret);
        }
    }

    /**
     * Get value of pdh counter's second value (base counters)
     *
     * @param counter The counter to get the value of
     * @return long value of the counter's second value, or negative value representing an error code
     */
    public static long querySecondCounter(HANDLEByReference counter) {
        try (CloseablePdhRawCounter counterValue = new CloseablePdhRawCounter()) {
            int ret = PDH.PdhGetRawCounterValue(counter.getValue(), PDH_FMT_RAW, counterValue);
            if (ret == WinError.ERROR_SUCCESS) {
                return counterValue.SecondValue;
            }
            return handleError(ret, WARN, "Failed to get counter.", (long) ret);
        }
    }

    /**
     * Adds a pdh counter to a query
     *
     * @param query Pointer to the query to add the counter
     * @param path  String name of the PerfMon counter. For Vista+, must be in English. Must localize this path for
     *              pre-Vista.
     * @param p     Pointer to the counter
     * @return true if successful
     */
    public static boolean addCounter(HANDLEByReference query, String path, HANDLEByReference p) {
        int ret = IS_VISTA_OR_GREATER ? PDH.PdhAddEnglishCounter(query.getValue(), path, PZERO, p)
                : PDH.PdhAddCounter(query.getValue(), path, PZERO, p);
        if (ret == WinError.ERROR_SUCCESS) {
            return true;
        }
        return handleError(ret, WARN, "Failed to add PDH Counter: " + path + ",", false);
    }

    /**
     * Remove a pdh counter
     *
     * @param p pointer to the counter
     * @return true if successful
     */
    public static boolean removeCounter(HANDLEByReference p) {
        return WinError.ERROR_SUCCESS == PDH.PdhRemoveCounter(p.getValue());
    }

    private static String formatErrorMessage(String message, int ret) {
        return message + " Error code: " + String.format(Locale.ROOT, FormatUtil.formatError(ret));
    }

    static <T> T handleError(int ret, LogLevel level, String message, T defaultValue) {
        switch (level) {
            case ERROR:
                if (LOG.isErrorEnabled()) {
                    LOG.error(formatErrorMessage(message, ret));
                }
                break;
            case WARN:
                if (LOG.isWarnEnabled()) {
                    LOG.warn(formatErrorMessage(message, ret));
                }
                break;
            case INFO:
                if (LOG.isInfoEnabled()) {
                    LOG.info(formatErrorMessage(message, ret));
                }
                break;
            case TRACE:
                if (LOG.isTraceEnabled()) {
                    LOG.trace(formatErrorMessage(message, ret));
                }
                break;
            case DEBUG:
            default:
                if (LOG.isDebugEnabled()) {
                    LOG.debug(formatErrorMessage(message, ret));
                }
                break;
        }
        return defaultValue;
    }
}
