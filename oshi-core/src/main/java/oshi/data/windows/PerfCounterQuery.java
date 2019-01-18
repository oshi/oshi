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

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Variant; //NOSONAR
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

public class PerfCounterQuery<T extends Enum<T>> {

    private static final Logger LOG = LoggerFactory.getLogger(PerfCounter.class);

    /*
     * Set on instantiation
     */
    protected final Class<T> propertyEnum;
    protected final String perfObject;
    protected final String perfWmiClass;
    protected final String queryKey;
    protected CounterDataSource source;
    protected PerfCounterQueryHandler pdhQueryHandler;
    protected WmiQueryHandler wmiQueryHandler;
    /*
     * Only one will be non-null depending on source
     */
    private EnumMap<T, PerfCounter> counterMap = null;
    protected WmiQuery<T> counterQuery = null;

    /*
     * Multiple classes use these constants
     */
    public static final String TOTAL_INSTANCE = "_Total";
    public static final String NOT_TOTAL_INSTANCE = "^" + TOTAL_INSTANCE;

    /**
     * Construct a new object to hold performance counter data source and
     * results
     * 
     * @param propertyEnum
     *            An enum which implements {@link PdhCounterProperty} and
     *            contains the WMI field (Enum value) and PDH Counter string
     *            (instance and counter)
     * @param perfObject
     *            The PDH object for this counter; all counters on this object
     *            will be refreshed at the same time
     * @param perfWmiClass
     *            The WMI PerfData_RawData_* class corresponding to the PDH
     *            object
     */
    public PerfCounterQuery(Class<T> propertyEnum, String perfObject, String perfWmiClass) {
        this(propertyEnum, perfObject, perfWmiClass, perfObject);
    }

    /**
     * Construct a new object to hold performance counter data source and
     * results
     * 
     * @param propertyEnum
     *            An enum which implements {@link PdhCounterProperty} and
     *            contains the WMI field (Enum value) and PDH Counter string
     *            (instance and counter)
     * @param perfObject
     *            The PDH object for this counter; all counters on this object
     *            will be refreshed at the same time
     * @param perfWmiClass
     *            The WMI PerfData_RawData_* class corresponding to the PDH
     *            object
     * @param queryKey
     *            An optional key for PDH counter updates; defaults to the PDH
     *            object name
     */
    public PerfCounterQuery(Class<T> propertyEnum, String perfObject, String perfWmiClass, String queryKey) {
        if (PdhCounterProperty.class.isAssignableFrom(propertyEnum.getDeclaringClass())) {
            throw new IllegalArgumentException(
                    propertyEnum.getDeclaringClass().getName() + " must implement PdhCounterProperty.");
        }
        this.propertyEnum = propertyEnum;
        this.perfObject = perfObject;
        this.perfWmiClass = perfWmiClass;
        this.queryKey = queryKey;
        this.pdhQueryHandler = PerfCounterQueryHandler.getInstance();
        this.wmiQueryHandler = WmiQueryHandler.createInstance();
        // Start off with PDH as source; if query here fails we will permanently
        // fall back to WMI
        this.source = CounterDataSource.PDH;
    }

    /**
     * Set the Data Source for these counters
     * 
     * @param source
     *            The source of data
     * @return Whether the data source was successfully set
     */
    public boolean setDataSource(CounterDataSource source) {
        this.source = source;
        switch (source) {
        case PDH:
            LOG.debug("Attempting to set PDH Data Source.");
            unInitWmiCounters();
            return initPdhCounters();
        case WMI:
            LOG.debug("Attempting to set WMI Data Source.");
            unInitPdhCounters();
            initWmiCounters();
            return true;
        default:
            // This should never happen unless you've added a new source and
            // forgot to add a case for it
            throw new IllegalArgumentException("Invalid Data Source specified.");
        }
    }

    /**
     * Initialize PDH counters for this data source. Adds necessary counters to
     * a PDH Query.
     * 
     * @return True if the counters were successfully added.
     */
    protected boolean initPdhCounters() {
        this.counterMap = new EnumMap<>(propertyEnum);
        for (T prop : propertyEnum.getEnumConstants()) {
            PerfCounter counter = PerfDataUtil.createCounter(perfObject, ((PdhCounterProperty) prop).getInstance(),
                    ((PdhCounterProperty) prop).getCounter());
            counterMap.put(prop, counter);
            if (!pdhQueryHandler.addCounterToQuery(counter, this.queryKey)) {
                unInitPdhCounters();
                return false;
            }
        }
        return true;
    }

    /**
     * Uninitialize PDH counters for this data source. Removes necessary
     * counters from the PDH Query, releasing their handles.
     */
    protected void unInitPdhCounters() {
        pdhQueryHandler.removeAllCountersFromQuery(this.queryKey);
        this.counterMap = null;
    }

    /**
     * Initialize the WMI query object needed to retrieve counters for this data
     * source.
     */
    protected void initWmiCounters() {
        this.counterQuery = new WmiQuery<>(perfWmiClass, propertyEnum);
    }

    /**
     * Uninitializes the WMI query object needed to retrieve counters for this
     * data source, allowing it to be garbage collected.
     */
    protected void unInitWmiCounters() {
        this.counterQuery = null;
    }

    /**
     * Query the current data source (PDH or WMI) for the Performance Counter
     * values corresponding to the property enum.
     * 
     * @return A map of the values by the counter enum.
     */
    public Map<T, Long> queryValues() {
        EnumMap<T, Long> valueMap = new EnumMap<>(propertyEnum);
        T[] props = this.propertyEnum.getEnumConstants();
        if (source.equals(CounterDataSource.PDH)) {
            // Set up the query and counter handles, and query
            if (initPdhCounters() && queryPdh(valueMap, props)) {
                // If both init and query return true, then valueMap contains
                // the results. Release the handles.
                unInitPdhCounters();
            } else {
                // If either init or query failed, switch to WMI
                setDataSource(CounterDataSource.WMI);
            }
        }
        if (source.equals(CounterDataSource.WMI)) {
            queryWmi(valueMap, props);
        }
        return valueMap;
    }

    private boolean queryPdh(Map<T, Long> valueMap, T[] props) {
        if (counterMap != null && 0 < pdhQueryHandler.updateQuery(this.queryKey)) {
            for (T prop : props) {
                valueMap.put(prop, pdhQueryHandler.queryCounter(counterMap.get(prop)));
            }
            return true;
        }
        // Zero timestamp means update failed after muliple
        // attempts; fallback to WMI
        return false;
    }

    private void queryWmi(Map<T, Long> valueMap, T[] props) {
        WmiResult<T> result = wmiQueryHandler.queryWMI(this.counterQuery);
        if (result.getResultCount() > 0) {
            for (T prop : props) {
                switch (result.getVtType(prop)) {
                case Variant.VT_I2:
                    valueMap.put(prop, Long.valueOf(WmiUtil.getUint16(result, prop, 0)));
                    break;
                case Variant.VT_I4:
                    valueMap.put(prop, WmiUtil.getUint32asLong(result, prop, 0));
                    break;
                case Variant.VT_BSTR:
                    valueMap.put(prop, WmiUtil.getUint64(result, prop, 0));
                    break;
                default:
                    throw new ClassCastException("Unimplemented VT Type Mapping.");
                }
            }
        }
    }

    /**
     * Source of performance counter data.
     */
    public enum CounterDataSource {
        /**
         * Performance Counter data will be pulled from a PDH Counter
         */
        PDH,
        /**
         * Performance Counter data will be pulled from a WMI PerfData_RawData_*
         * table
         */
        WMI;
    }

    /**
     * Contract for Counter Property Enums
     */
    public interface PdhCounterProperty {
        /**
         * @return Returns the instance.
         */
        String getInstance();

        /**
         * @return Returns the counter.
         */
        String getCounter();
    }
}
