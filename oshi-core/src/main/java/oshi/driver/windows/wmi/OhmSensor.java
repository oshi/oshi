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
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery; //NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Utility to query Open Hardware Monitor WMI data for Sensors
 */
@ThreadSafe
public final class OhmSensor {

    private static final String SENSOR = "Sensor";

    /**
     * Sensor value property
     */
    public enum ValueProperty {
        VALUE;
    }

    private OhmSensor() {
    }

    /**
     * Queries the sensor value of an hardware identifier and sensor type.
     *
     * @param identifier
     *            The identifier whose value to query.
     * @param sensorType
     *            The type of sensor to query.
     * @return The sensor value.
     */
    public static WmiResult<ValueProperty> querySensorValue(String identifier, String sensorType) {
        StringBuilder sb = new StringBuilder(SENSOR);
        sb.append(" WHERE Parent = \"").append(identifier);
        sb.append("\" AND SensorType=\"").append(sensorType).append('\"');
        WmiQuery<ValueProperty> ohmSensorQuery = new WmiQuery<>(WmiUtil.OHM_NAMESPACE, sb.toString(),
                ValueProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(ohmSensorQuery);
    }
}
