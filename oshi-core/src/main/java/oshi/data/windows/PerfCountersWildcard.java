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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.PdhUtil; //NOSONAR
import com.sun.jna.platform.win32.PdhUtil.PdhEnumObjectItems;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.COM.Wbemcli;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.windows.PdhUtilXP;
import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

public class PerfCountersWildcard<T extends Enum<T>> extends PerfCounters<T> {

    private static final Logger LOG = LoggerFactory.getLogger(PerfCountersWildcard.class);

    private EnumMap<T, List<PerfCounter>> counterListMap = null;
    private final String perfObjectLocalized;

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
     *            instances only. If the instance is null then all counters will
     *            be added to the PDH query, otherwise the PDH counter will only
     *            include instances which are wildcard matches with the given
     *            instance, replacing '?' with a single character, '*' with any
     *            number of characters, and reversing the test if the first
     *            character is '^'. If the counter source is WMI, the instance
     *            filtering has no effect, and it is the responsibility of the
     *            user to add filtering to the perfWmiClass string using a WHERE
     *            clause.
     * @param perfObject
     *            The PDH object for this counter; all counters on this object
     *            will be refreshed at the same time
     * @param perfWmiClass
     *            The WMI PerfData_RawData_* class corresponding to the PDH
     *            object
     */
    public PerfCountersWildcard(Class<T> propertyEnum, String perfObject, String perfWmiClass) {
        super(propertyEnum, perfObject, perfWmiClass);

        perfObjectLocalized = localize(this.perfObject);
        // Try PDH first, fallback to WMI
        if (!setDataSource(CounterDataSource.PDH)) {
            LOG.debug("PDH Data Source failed for {}", perfObject);
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
        // Get list of instances
        PdhEnumObjectItems objectItems = PdhUtil.PdhEnumObjectItems(null, null, perfObjectLocalized, 100);
        List<String> instances = objectItems.getInstances();
        // Populate map
        this.counterListMap = new EnumMap<>(propertyEnum);
        for (T prop : propertyEnum.getEnumConstants()) {
            List<PerfCounter> counterList = new ArrayList<>(instances.size());
            for (String instance : instances) {
                // Filter by instance
                if (((PdhCounterProperty) prop).getInstance() != null && !Util.wildcardMatch(instance.toLowerCase(),
                        ((PdhCounterProperty) prop).getInstance().toLowerCase())) {
                    continue;
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
        if (counterListMap != null && !counterListMap.get(props[0]).isEmpty()) {
            List<PerfCounter> counterList = counterListMap.get(props[0]);
            if (counterList != null && 0 < PerfDataUtil.updateQuery(counterList.get(0))) {
                for (T prop : props) {
                    List<Long> values = new ArrayList<>();
                    for (PerfCounter counter : counterListMap.get(prop)) {
                        values.add(PerfDataUtil.queryCounter(counter));
                    }
                    valueMap.put(prop, values);
                }
                return;
            }
        }
        // Zero timestamp means update failed after muliple
        // attempts; fallback to WMI
        setDataSource(CounterDataSource.WMI);
    }

    private void queryWmiWildcard(Map<T, List<Long>> valueMap, T[] props) {
        WmiResult<T> result = WmiQueryHandler.getInstance().queryWMI(this.counterQuery);
        if (result.getResultCount() > 0) {
            for (T prop : props) {
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
                    case Wbemcli.CIM_STRING:
                        String s = WmiUtil.getString(result, prop, i);
                        if (s.length() > 8) {
                            values.add(0L);
                        } else {
                            // Encode ASCII as a long
                            values.add(ParseUtil.strToLong(s, 8));
                        }
                        break;
                    default:
                        throw new ClassCastException("Unimplemented CIM Type Mapping.");
                    }
                }
                valueMap.put(prop, values);
            }
        }
    }
}
