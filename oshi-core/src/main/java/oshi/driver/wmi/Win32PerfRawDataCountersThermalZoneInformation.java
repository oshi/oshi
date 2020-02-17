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
package oshi.driver.wmi;

import java.util.List;
import java.util.Map;

import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;

public class Win32PerfRawDataCountersThermalZoneInformation {

    private static final String THERMAL_ZONE_INFORMATION_WHERE_NAME_LIKE_CPU = "Win32_PerfRawData_Counters_ThermalZoneInformation WHERE Name LIKE \"%cpu%\"";
    private static final String THERMAL_ZONE_INFORMATION = "Thermal Zone Information";

    /*
     * Thermal Zone Temperature
     */
    public enum ThermalZoneProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME("*cpu*"),
        // Remaining elements define counters
        TEMPERATURE("Temperature");

        private final String counter;

        ThermalZoneProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    /**
     * Returns thermal zone temperatures.
     *
     * @return Thermal zone names and corresponding temperatures of the thermal
     *         zone, in degrees Kelvin.
     */
    public Map<ThermalZoneProperty, List<Long>> queryThermalZoneTemps() {
        PerfCounterWildcardQuery<ThermalZoneProperty> thermalZonePerfCounters = new PerfCounterWildcardQuery<>(
                ThermalZoneProperty.class, THERMAL_ZONE_INFORMATION, THERMAL_ZONE_INFORMATION_WHERE_NAME_LIKE_CPU);
        return thermalZonePerfCounters.queryValuesWildcard();
    }
}
