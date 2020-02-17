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

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery; //NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.util.platform.windows.WmiQueryHandler;

public class Win32Processor {

    private static final String WIN32_PROCESSOR = "Win32_Processor";

    /**
     * Processor voltage properties.
     */
    public enum VoltProperty {
        CURRENTVOLTAGE, VOLTAGECAPS;
    }

    /**
     * Returns processor voltage.
     *
     * @return Current voltage of the processor. If the eighth bit is set, bits 0-6
     *         contain the voltage multiplied by 10. If the eighth bit is not set,
     *         then the bit setting in VoltageCaps represents the voltage value.
     */
    public WmiResult<VoltProperty> queryVoltage() {
        WmiQuery<VoltProperty> voltQuery = new WmiQuery<>(WIN32_PROCESSOR, VoltProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(voltQuery);
    }
}
