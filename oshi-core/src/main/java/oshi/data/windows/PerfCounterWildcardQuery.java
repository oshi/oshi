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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.PdhUtil; //NOSONAR
import com.sun.jna.platform.win32.PdhUtil.PdhEnumObjectItems;
import com.sun.jna.platform.win32.PdhUtil.PdhException;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.COM.Wbemcli;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.util.Util;
import oshi.util.platform.windows.PdhUtilXP;
import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.platform.windows.WmiUtil;

public class PerfCounterWildcardQuery<T extends Enum<T>> extends PerfCounterQuery<T> {

    private static final Logger LOG = LoggerFactory.getLogger(PerfCounterWildcardQuery.class);

    private EnumMap<T, List<PerfCounter>> counterListMap = null;
    private List<String> instancesFromLastQuery = new ArrayList<>();
    private final String perfObjectLocalized;
    private final String instanceFilter;

    /**
     * Construct a new object to hold performance counter data source and
     * results
     * 
     * @param propertyEnum
     *            An enum which implements {@link PdhCounterWildcardProperty}
     *            and contains the WMI field (Enum value) and PDH Counter string
     *            (instance or counter).
     *            <P>
     *            The first element of the enum defines the instance filter,
     *            rather than a counter name. This acts as a filter for PDH
     *            instances only and should correlate with a WMI String field
     *            defining the same name. If the instance is null then all
     *            counters will be added to the PDH query, otherwise the PDH
     *            counter will only include instances which are wildcard matches
     *            with the given instance, replacing '?' with a single
     *            character, '*' with any number of characters, and reversing
     *            the test if the first character is '^'. If the counter source
     *            is WMI, the instance filtering has no effect, and it is the
     *            responsibility of the user to add filtering to the
     *            perfWmiClass string using a WHERE clause.
     * @param perfObject
     *            The PDH object for this counter; all counters on this object
     *            will be refreshed at the same time
     * @param perfWmiClass
     *            The WMI PerfData_RawData_* class corresponding to the PDH
     *            object
     */
    public PerfCounterWildcardQuery(Class<T> propertyEnum, String perfObject, String perfWmiClass) {
        this(propertyEnum, perfObject, perfWmiClass, perfObject);
    }

    /**
     * Construct a new object to hold performance counter data source and
     * results
     * 
     * @param propertyEnum
     *            An enum which implements {@link PdhCounterWildcardProperty}
     *            and contains the WMI field (Enum value) and PDH Counter string
     *            (instance or counter).
     *            <P>
     *            The first element of the enum defines the instance filter,
     *            rather than a counter name. This acts as a filter for PDH
     *            instances only and should correlate with a WMI String field
     *            defining the same name. If the instance is null then all
     *            counters will be added to the PDH query, otherwise the PDH
     *            counter will only include instances which are wildcard matches
     *            with the given instance, replacing '?' with a single
     *            character, '*' with any number of characters, and reversing
     *            the test if the first character is '^'. If the counter source
     *            is WMI, the instance filtering has no effect, and it is the
     *            responsibility of the user to add filtering to the
     *            perfWmiClass string using a WHERE clause.
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
    public PerfCounterWildcardQuery(Class<T> propertyEnum, String perfObject, String perfWmiClass, String queryKey) {
        super(propertyEnum, perfObject, perfWmiClass, queryKey);

        if (propertyEnum.getEnumConstants().length < 2) {
            throw new IllegalArgumentException("Enum " + propertyEnum.getName()
                    + " must have at least two elements, an instance filter and a counter.");
        }
        this.instanceFilter = ((PdhCounterWildcardProperty) propertyEnum.getEnumConstants()[0]).getCounter()
                .toLowerCase();
        this.perfObjectLocalized = localize(this.perfObject);
    }

    /**
     * Localize a PerfCounter string. English counter names should normally be
     * in HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows
     * NT\CurrentVersion\Perflib\009\Counter, but language manipulations may
     * delete the 009 index. In this case we can assume English must be the
     * language and continue. We may still fail to match the name if the
     * assumption is wrong but it's better than nothing.
     * 
     * @param perfObject
     *            A String to localize
     * @return The localized string if localization succussful, or the original
     *         string otherwise.
     */
    private static String localize(String perfObject) {
        String localized = null;
        try {
            localized = PdhUtilXP.PdhLookupPerfNameByIndex(null, PdhUtil.PdhLookupPerfIndexByEnglishName(perfObject));
        } catch (Win32Exception e) {
            LOG.error("Unable to locate English counter names in registry Perflib 009. Assuming English counters.");
        }
        if (localized == null || localized.length() == 0) {
            return perfObject;
        }
        LOG.debug("Localized {} to {}", perfObject, localized);
        return localized;
    }

    /**
     * Initialize PDH counters for this data source. Adds necessary counters to
     * a PDH Query.
     * 
     * @return True if the counters were successfully added.
     */
    @Override
    protected boolean initPdhCounters() {
        return fillCounterListMap();
    }

    /**
     * Uninitialize PDH counters for this data source. Removes necessary
     * counters from the PDH Query, releasing their handles.
     */
    @Override
    protected void unInitPdhCounters() {
        pdhQueryHandler.removeAllCountersFromQuery(this.queryKey);
        this.counterListMap = null;
    }

    /**
     * This method is not implemented on this class.
     * 
     * @see #queryValuesWildcard
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
        this.instancesFromLastQuery.clear();
        T[] props = this.propertyEnum.getEnumConstants();
        if (source.equals(CounterDataSource.PDH)) {
            // Set up the query and counter handles, and query
            if (initPdhCounters() && queryPdhWildcard(valueMap, props)) {
                // If both init and query return true, then valueMap contains
                // the results. Release the handles.
                unInitPdhCounters();
            } else {
                // If either init or query failed, switch to WMI
                setDataSource(CounterDataSource.WMI);
            }
        }
        if (source.equals(CounterDataSource.WMI)) {
            queryWmiWildcard(valueMap, props);
        }
        return valueMap;
    }

    private boolean queryPdhWildcard(Map<T, List<Long>> valueMap, T[] props) {
        if (this.counterListMap != null && 0 < pdhQueryHandler.updateQuery(this.queryKey)) {
            for (int i = 1; i < props.length; i++) {
                T prop = props[i];
                List<Long> values = new ArrayList<>();
                for (PerfCounter counter : counterListMap.get(prop)) {
                    values.add(pdhQueryHandler.queryCounter(counter));
                    if (i == 1) {
                        instancesFromLastQuery.add(counter.getInstance());
                    }
                }
                valueMap.put(prop, values);
            }
            return true;
        }
        // Zero timestamp means update failed after multiple attempts; fall back
        // to WMI
        return false;
    }

    private void queryWmiWildcard(Map<T, List<Long>> valueMap, T[] props) {
        WmiResult<T> result = wmiQueryHandler.queryWMI(this.counterQuery);
        if (result.getResultCount() > 0) {
            // First element is instance name
            for (int i = 0; i < result.getResultCount(); i++) {
                instancesFromLastQuery.add(WmiUtil.getString(result, props[0], i));
            }
            // Remaining elements are counters
            for (int p = 1; p < props.length; p++) {
                T prop = props[p];
                List<Long> values = new ArrayList<>();
                for (int i = 0; i < result.getResultCount(); i++) {
                    switch (result.getCIMType(prop)) {
                    case Wbemcli.CIM_UINT16:
                        values.add(Long.valueOf(WmiUtil.getUint16(result, prop, i)));
                        break;
                    case Wbemcli.CIM_UINT32:
                        values.add(WmiUtil.getUint32asLong(result, prop, i));
                        break;
                    case Wbemcli.CIM_UINT64:
                        values.add(WmiUtil.getUint64(result, prop, i));
                        break;
                    default:
                        throw new ClassCastException("Unimplemented CIM Type Mapping.");
                    }
                }
                valueMap.put(prop, values);
            }
        }
    }

    /**
     * List the instances corresponding to the value map lists
     * 
     * @return A list of the in the order they are returned in the value map
     *         query
     */
    public List<String> getInstancesFromLastQuery() {
        return this.instancesFromLastQuery;
    }

    private boolean fillCounterListMap() {
        // Get list of instances
        final PdhEnumObjectItems objectItems;
        try {
            objectItems = PdhUtil.PdhEnumObjectItems(null, null, perfObjectLocalized, 100);
        } catch (PdhException e) {
            return false;
        }
        List<String> instances = objectItems.getInstances();
        // Filter out instances not matching filter
        instances.removeIf(i -> !Util.wildcardMatch(i.toLowerCase(), this.instanceFilter));
        // Track instances not in counter list, to add
        Set<String> instancesToAdd = new HashSet<>(instances);
        // Populate map with instances to add. Skip first counter, which defines
        // instance filter
        this.counterListMap = new EnumMap<>(propertyEnum);
        for (int i = 1; i < propertyEnum.getEnumConstants().length; i++) {
            T prop = propertyEnum.getEnumConstants()[i];
            List<PerfCounter> counterList = new ArrayList<>(instances.size());
            for (String instance : instancesToAdd) {
                PerfCounter counter = PerfDataUtil.createCounter(perfObject, instance,
                        ((PdhCounterWildcardProperty) prop).getCounter());
                if (!pdhQueryHandler.addCounterToQuery(counter, this.queryKey)) {
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
     * Contract for Counter Property Enums
     */
    public interface PdhCounterWildcardProperty {
        /**
         * @return Returns the counter. The first element of the enum will
         *         return the instance filter rather than a counter.
         */
        String getCounter();
    }
}
