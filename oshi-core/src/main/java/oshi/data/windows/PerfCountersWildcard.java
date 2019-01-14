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
package oshi.data.windows;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.win32.PdhUtil; //NOSONAR
import com.sun.jna.platform.win32.PdhUtil.PdhEnumObjectItems;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

public class PerfCountersWildcard<T extends Enum<T>> extends PerfCounters<T> {

    private EnumMap<T, List<PerfCounter>> counterListMap = null;

    /**
     * Construct a new object to hold performance counter data source and
     * results
     * 
     * @param propertyEnum
     *            An enum which implements {@link PdhCounterProperty} and
     *            contains the WMI field (Enum value) and PDH Counter string
     *            (instance and counter).
     *            <P>
     *            The instance name in this case acts as a filter for PDH
     *            instances only. If the instance is null or "*" then all
     *            counters will be added to the PDH query, otherwise the PDH
     *            counter will only match the included instance. If the counter
     *            source is WMI, the instance filtering has no effect, and it is
     *            the responsibility of the user to add filtering to the
     *            perfWmiClass string.
     * @param perfObject
     *            The PDH object for this counter; all counters on this object
     *            will be refreshed at the same time
     * @param perfWmiClass
     *            The WMI PerfData_RawData_* class corresponding to the PDH
     *            object
     */
    public PerfCountersWildcard(Class<T> propertyEnum, String perfObject, String perfWmiClass) {
        super(propertyEnum, perfObject, perfWmiClass);
        // Try PDH first, fallback to WMI
        if (!setDataSource(CounterDataSource.PDH)) {
            setDataSource(CounterDataSource.WMI);
        }
        // Release handles on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                unInitPdhCounters();
            }
        });
    }

    /**
     * Initialize PDH counters for this data source. Adds necessary counters to
     * a PDH Query.
     * 
     * @return True if the counters were successfully added.
     */
    @Override
    protected boolean initPdhCounters() {
        // Get list of instances
        PdhEnumObjectItems objectItems = PdhUtil.PdhEnumObjectItems(null, null, perfObject, 100);
        List<String> instances = objectItems.getInstances();
        // Populate map
        this.counterListMap = new EnumMap<>(propertyEnum);
        for (T prop : propertyEnum.getEnumConstants()) {
            List<PerfCounter> counterList = new ArrayList<>(instances.size());
            for (String instance : instances) {
                // If user passed a non-wildcard instance, filter
                // TODO: Actually use regexp to match wildcards and allow for
                // negation
                if (((PdhCounterProperty) prop).getInstance() != null
                        && !"*".equals(((PdhCounterProperty) prop).getInstance())) {
                    if (!((PdhCounterProperty) prop).getInstance().equalsIgnoreCase(instance)) {
                        continue;
                    }
                }
                PerfCounter counter = PerfDataUtil.createCounter(perfObject, instance,
                        ((PdhCounterProperty) prop).getCounter());
                if (!PerfDataUtil.addCounterToQuery(counter)) {
                    unInitPdhCounters();
                    return false;
                }
                counterList.add(counter);
            }
            this.counterListMap.put(prop, counterList);
        }
        return this.counterListMap.size() > 0;
    }

    /**
     * Uninitialize PDH counters for this data source. Removes necessary
     * counters from the PDH Query, releasing their handles.
     */
    @Override
    protected void unInitPdhCounters() {
        if (this.counterListMap != null) {
            for (List<PerfCounter> counterList : this.counterListMap.values()) {
                for (PerfCounter counter : counterList) {
                    PerfDataUtil.removeCounterFromQuery(counter);
                }
            }
        }
        this.counterListMap = null;
    }

    /**
     * This method is not implemented on this class.
     * 
     * @see {@link #queryValuesWildcard()}
     */
    @Override
    public Map<T, Long> queryValues() {
        throw new UnsupportedOperationException("Use queryValuesWildcard() on this class.");
    }

    /**
     * Query the current data source (PDH or WMI) for the Performance Counter
     * values corresponding to the property enum.
     * 
     * @return A map of the values by the counter enum.
     */
    public Map<T, List<Long>> queryValuesWildcard() {
        EnumMap<T, List<Long>> valueMap = new EnumMap<>(propertyEnum);
        T[] props = this.propertyEnum.getEnumConstants();
        if (source.equals(CounterDataSource.PDH)) {
            queryPdhWildcard(valueMap, props);
        }
        // The pdh query may fail and set the source to WMI, so this is
        // intentionally not an "else"
        if (source.equals(CounterDataSource.WMI)) {
            queryWmiWildcard(valueMap, props);
        }
        return valueMap;
    }

    private void queryPdhWildcard(Map<T, List<Long>> valueMap, T[] props) {
        long timeStamp = 0L;
        if (counterListMap != null && !counterListMap.get(props[0]).isEmpty()) {
            List<PerfCounter> counterList = counterListMap.get(props[0]);
            if (counterList != null) {
                timeStamp = PerfDataUtil.updateQuery(counterList.get(0));
            }
        }
        if (timeStamp > 0) {
            for (T prop : props) {
                List<Long> values = new ArrayList<>();
                for (PerfCounter counter : counterListMap.get(prop)) {
                    values.add(PerfDataUtil.queryCounter(counter));
                }
                valueMap.put(prop, values);
            }
        } else {
            // Zero timestamp means update failed after muliple
            // attempts; fallback to WMI
            setDataSource(CounterDataSource.WMI);
        }
    }

    private void queryWmiWildcard(Map<T, List<Long>> valueMap, T[] props) {
        WmiResult<T> result = WmiQueryHandler.getInstance().queryWMI(this.counterQuery);
        if (result.getResultCount() > 0) {
            for (T prop : props) {
                List<Long> values = new ArrayList<>();
                for (int i = 0; i < result.getResultCount(); i++) {
                    switch (result.getVtType(prop)) {
                    case Variant.VT_I2:
                        values.add(Long.valueOf(WmiUtil.getUint16(result, prop, i)));
                        break;
                    case Variant.VT_I4:
                        values.add(WmiUtil.getUint32asLong(result, prop, i));
                        break;
                    case Variant.VT_BSTR:
                        values.add(WmiUtil.getUint64(result, prop, i));
                        break;
                    default:
                        throw new ClassCastException("Unimplemented VT Type Mapping.");
                    }
                }
                valueMap.put(prop, values);
            }
        }
    }
}
