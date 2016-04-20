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
 * enrico[dot]bianchi[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.hardware.GlobalMemory;
import oshi.jna.platform.windows.Pdh;
import oshi.jna.platform.windows.Pdh.PdhFmtCounterValue;
import oshi.jna.platform.windows.Psapi;
import oshi.jna.platform.windows.Psapi.PERFORMANCE_INFORMATION;
import oshi.json.NullAwareJsonObjectBuilder;

/**
 * Memory obtained by GlobalMemoryStatusEx.
 *
 * @author dblock[at]dblock[dot]org
 */
public class WindowsGlobalMemory implements GlobalMemory {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemory.class);

    private PERFORMANCE_INFORMATION perfInfo = new PERFORMANCE_INFORMATION();

    private long lastUpdate = 0;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    // Set up Performance Data Helper thread for % pagefile usage
    private PointerByReference pagefileQuery = new PointerByReference();

    private final IntByReference pFour = new IntByReference(4);

    private PointerByReference pPagefile;

    public WindowsGlobalMemory() {
        initPdh();
    }

    /**
     * Initialize performance monitor counter
     */
    private void initPdh() {
        // Open Pagefile query
        int pdhOpenPagefileQueryError = Pdh.INSTANCE.PdhOpenQuery(null, pFour, pagefileQuery);
        if (pdhOpenPagefileQueryError != 0) {
            LOG.error("Failed to open PDH Pagefile Query. Error code: {}",
                    String.format("0x%08X", pdhOpenPagefileQueryError));
        }
        if (pdhOpenPagefileQueryError == 0) {
            // \Paging File(_Total)\% Usage
            String pagefilePath = "\\Paging File(_Total)\\% Usage";
            pPagefile = new PointerByReference();
            int pdhAddPagefileCounterError = Pdh.INSTANCE.PdhAddEnglishCounterA(pagefileQuery.getValue(), pagefilePath,
                    pFour, pPagefile);
            if (pdhAddPagefileCounterError != 0) {
                LOG.error("Failed to add PDH Pagefile Counter. Error code: {}",
                        String.format("0x%08X", pdhAddPagefileCounterError));
            }
        }
        // Initialize by collecting data the first time
        Pdh.INSTANCE.PdhCollectQueryData(pagefileQuery.getValue());

        // Set up hook to close the query on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Pdh.INSTANCE.PdhCloseQuery(pagefileQuery.getValue());
            }
        });
    }

    /**
     * Update the performance information no more frequently than every 100ms
     */
    private void updatePerfInfo() {
        long now = System.currentTimeMillis();
        if (now - this.lastUpdate > 100) {
            if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
                this.perfInfo = null;
            }
            this.lastUpdate = now;
        }
    }

    @Override
    public long getAvailable() {
        updatePerfInfo();
        return this.perfInfo == null ? 0L : perfInfo.PageSize.longValue() * perfInfo.PhysicalAvailable.longValue();
    }

    @Override
    public long getTotal() {
        updatePerfInfo();
        return this.perfInfo == null ? 0L : perfInfo.PageSize.longValue() * perfInfo.PhysicalTotal.longValue();
    }

    @Override
    public long getSwapTotal() {
        updatePerfInfo();
        return this.perfInfo == null ? 0L
                : perfInfo.PageSize.longValue()
                        * (perfInfo.CommitLimit.longValue() - perfInfo.PhysicalTotal.longValue());
    }

    @Override
    public long getSwapUsed() {
        PdhFmtCounterValue phPagefileCounterValue = new PdhFmtCounterValue();
        int ret = Pdh.INSTANCE.PdhGetFormattedCounterValue(pPagefile.getValue(), Pdh.PDH_FMT_LARGE | Pdh.PDH_FMT_1000,
                null, phPagefileCounterValue);
        if (ret != 0) {
            LOG.warn("Failed to get Pagefile % Usage counter. Error code: {}", String.format("0x%08X", ret));
            return 0L;
        }
        // Returns results in 1000's of percent, e.g. 5% is 5000
        // Multiply by page file size and Divide by 100 * 1000
        // Putting division at end avoids need to cast division to double
        return getSwapTotal() * phPagefileCounterValue.value.largeValue / 100000;
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("available", getAvailable())
                .add("total", getTotal()).add("swapTotal", getSwapTotal()).add("swapUsed", getSwapUsed()).build();
    }
}
