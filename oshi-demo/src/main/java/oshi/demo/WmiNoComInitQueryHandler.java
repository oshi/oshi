/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.demo;

import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.COM.COMException; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.Wbemcli;
import com.sun.jna.platform.win32.COM.WbemcliUtil;

import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Query handler class that overrides WMI query method assuming COM is already
 * initialized by the user.
 */
public class WmiNoComInitQueryHandler extends WmiQueryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WmiNoComInitQueryHandler.class);

    @Override
    public <T extends Enum<T>> WbemcliUtil.WmiResult<T> queryWMI(WbemcliUtil.WmiQuery<T> query) {

        WbemcliUtil.WmiResult<T> result = WbemcliUtil.INSTANCE.new WmiResult<>(query.getPropertyEnum());
        if (failedWmiClassNames.contains(query.getWmiClassName())) {
            return result;
        }
        try {
            result = query.execute(wmiTimeout);
        } catch (COMException e) {
            // Ignore any exceptions with OpenHardwareMonitor
            if (!WmiUtil.OHM_NAMESPACE.equals(query.getNameSpace())) {
                final int hresult = e.getHresult() == null ? -1 : e.getHresult().intValue();
                switch (hresult) {
                case Wbemcli.WBEM_E_INVALID_NAMESPACE:
                    LOG.warn("COM exception: Invalid Namespace {}", query.getNameSpace());
                    break;
                case Wbemcli.WBEM_E_INVALID_CLASS:
                    LOG.warn("COM exception: Invalid Class {}", query.getWmiClassName());
                    break;
                case Wbemcli.WBEM_E_INVALID_QUERY:
                    LOG.warn("COM exception: Invalid Query: {}", WmiUtil.queryToString(query));
                    break;
                default:
                    handleComException(query, e);
                    break;
                }
                failedWmiClassNames.add(query.getWmiClassName());
            }
        } catch (TimeoutException e) {
            LOG.error("WMI query timed out after {} ms: {}", wmiTimeout, WmiUtil.queryToString(query));
        }
        return result;
    }
}