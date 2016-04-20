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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.windows.Pdh;

/**
 * Provides access to open pdh queries and add counters
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
}