/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
 * Utility to query Open Hardware Monitor WMI data for Hardware
 */
@ThreadSafe
public final class OhmHardware {

    private static final String HARDWARE = "Hardware";

    /**
     * HW Identifier Property
     */
    public enum IdentifierProperty {
        IDENTIFIER;
    }

    private OhmHardware() {
    }

    /**
     * Queries the hardware identifiers for a monitored type.
     *
     * @param h
     *            An instantiated {@link WmiQueryHandler}. User should have already
     *            initialized COM.
     * @param typeToQuery
     *            which type to filter based on
     * @param typeName
     *            the name of the type
     * @return The sensor value.
     */
    public static WmiResult<IdentifierProperty> queryHwIdentifier(WmiQueryHandler h, String typeToQuery,
            String typeName) {
        StringBuilder sb = new StringBuilder(HARDWARE);
        sb.append(" WHERE ").append(typeToQuery).append("Type=\"").append(typeName).append('\"');
        WmiQuery<IdentifierProperty> cpuIdentifierQuery = new WmiQuery<>(WmiUtil.OHM_NAMESPACE, sb.toString(),
                IdentifierProperty.class);
        return h.queryWMI(cpuIdentifierQuery, false);
    }
}
