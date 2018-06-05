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
import java.util.List;
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

import oshi.util.ParseUtil;

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

    // Maps to hold pointers to the relevant counter information
    private static final Map<String, HANDLEByReference> counterMap = new HashMap<>();
    private static final Map<String, HANDLEByReference> queryMap = new HashMap<>();
    private static final Map<String, DWORDByReference> formatMap = new HashMap<>();

    private PdhUtil() {
        // Set up hook to close all queries on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (HANDLEByReference query : queryMap.values()) {
                    Pdh.INSTANCE.PdhCloseQuery(query.getValue());
                }
            }
        });
    }

    public static boolean addCounter(String counterString) {
        if (queryMap.containsKey(counterString)) {
            LOG.error("Counter already exists: {}", counterString);
            return false;
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

    public static boolean removeCounter(String counterString) {
        if (!queryMap.containsKey(counterString)) {
            LOG.error("Counter does not exist: {}", counterString);
            return false;
        }
        Pdh.INSTANCE.PdhCloseQuery(queryMap.get(counterString).getValue());
        counterMap.remove(counterString);
        queryMap.remove(counterString);
        return true;
    }

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
        int pdhOpenQueryError = Pdh.INSTANCE.PdhOpenQuery(null, PZERO, p);
        if (pdhOpenQueryError != 0) {
            System.out.format("Failed to open PDH Query. Error code: %s", String.format("0x$08X", pdhOpenQueryError));
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
        int pdhAddCounterError = Pdh.INSTANCE.PdhAddEnglishCounter(query.getValue(), path, PZERO, p);
        if (pdhAddCounterError != 0) {
            System.out.format("Failed to add PDH Counter: %s, Error code: %s", path,
                    String.format("0x%08X", pdhAddCounterError));
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
        int ret = Pdh.INSTANCE.PdhCollectQueryData(query.getValue());
        if (ret != 0) {
            System.out.format("Failed to update counters. Error code: %s", String.format("0x%08X", ret));
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
        PDH_RAW_COUNTER counterValue = new PDH_RAW_COUNTER();
        // | Pdh.PDH_FMT_LARGE | Pdh.PDH_FMT_1000));
        int ret = Pdh.INSTANCE.PdhGetRawCounterValue(counter.getValue(), PDH_FMT_RAW, counterValue);
        if (ret != 0) {
            LOG.warn("Failed to get counter. Error code: {}", String.format("0x%08X", ret));
            return 0L;
        }
        System.out.println("First :" + counterValue.FirstValue);
        return counterValue.FirstValue;
    }

    // TODO Remove, temp for debug
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String s = "\\Processor(0)\\% user time";
        String t = "\\Processor(1)\\% user time";
        String u = "\\Processor(2)\\% user time";
        String v = "\\Processor(3)\\% user time";
        addCounter(s);
        queryCounter(s);
        addCounter(t);
        queryCounter(t);
        addCounter(u);
        queryCounter(u);
        addCounter(v);
        queryCounter(v);
        System.out.println(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();

        Map<String, List<String>> wmiTicks = WmiUtil.selectStringsFrom(null,
                "Win32_PerfRawData_Counters_ProcessorInformation",
                "Name,PercentIdleTime,PercentPrivilegedTime,PercentUserTime,PercentInterruptTime,PercentDPCTime",
                "WHERE NOT Name LIKE \"%_Total\"");
        for (int cpu = 0; cpu < 4; cpu++) {
            for (int index = 0; index < wmiTicks.get("Name").size(); index++) {
                // It would be too easy if the WMI order matched logical
                // processors
                // but alas, it goes "0,3"; "0,2"; "0,1"; "0,0". So let's do it
                // right and actually string match the name. The first 0 will be
                // there unless we're dealing with NUMA nodes
                String name = "0," + cpu;
                if (wmiTicks.get("Name").get(index).equals(name)) {
                    // Skipping nice and IOWait, they'll stay 0
                    System.out.println(
                            "USER  :" + ParseUtil.parseLongOrDefault(wmiTicks.get("PercentUserTime").get(index), 0L));
                }
            }
        }
        System.out.println(System.currentTimeMillis() - start);
    }
}