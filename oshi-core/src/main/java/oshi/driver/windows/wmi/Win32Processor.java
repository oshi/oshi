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
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery; //NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_Processor}
 */
@ThreadSafe
public final class Win32Processor {

    private static final String WIN32_PROCESSOR = "Win32_Processor";

    /**
     * Processor voltage properties.
     */
    public enum VoltProperty {
        CURRENTVOLTAGE, VOLTAGECAPS;
    }

    /**
     * Processor ID property
     */
    public enum ProcessorIdProperty {
        PROCESSORID;
    }

    /**
     * Processor bitness property
     */
    public enum BitnessProperty {
        ADDRESSWIDTH;
    }

    private Win32Processor() {
    }

    /**
     * Returns processor voltage.
     *
     * @return Current voltage of the processor. If the eighth bit is set, bits 0-6
     *         contain the voltage multiplied by 10. If the eighth bit is not set,
     *         then the bit setting in VoltageCaps represents the voltage value.
     */
    public static WmiResult<VoltProperty> queryVoltage() {
        WmiQuery<VoltProperty> voltQuery = new WmiQuery<>(WIN32_PROCESSOR, VoltProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(voltQuery);
    }

    /**
     * Returns processor ID.
     *
     * @return Processor information that describes the processor features. For an
     *         x86 class CPU, the field format depends on the processor support of
     *         the CPUID instruction. If the instruction is supported, the property
     *         contains 2 (two) DWORD formatted values. The first is an offset of
     *         08h-0Bh, which is the EAX value that a CPUID instruction returns with
     *         input EAX set to 1. The second is an offset of 0Ch-0Fh, which is the
     *         EDX value that the instruction returns. Only the first two bytes of
     *         the property are significant and contain the contents of the DX
     *         register at CPU reset—all others are set to 0 (zero), and the
     *         contents are in DWORD format.
     */
    public static WmiResult<ProcessorIdProperty> queryProcessorId() {
        WmiQuery<ProcessorIdProperty> idQuery = new WmiQuery<>(WIN32_PROCESSOR, ProcessorIdProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(idQuery);
    }

    /**
     * Returns address width.
     *
     * @return On a 32-bit operating system, the value is 32 and on a 64-bit
     *         operating system it is 64.
     */
    public static WmiResult<BitnessProperty> queryBitness() {
        WmiQuery<BitnessProperty> bitnessQuery = new WmiQuery<>(WIN32_PROCESSOR, BitnessProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(bitnessQuery);
    }
}
