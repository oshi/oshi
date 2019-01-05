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
package oshi.hardware.platform.unix.freebsd;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import oshi.hardware.Sensors;
import oshi.jna.platform.unix.freebsd.Libc;

public class FreeBsdSensors implements Sensors {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuTemperature() {
        // Try with kldload coretemp
        double sumTemp = 0d;
        int cpu = 0;
        String name = "dev.cpu.%d.temperature";
        while (true) {
            IntByReference size = new IntByReference(Libc.INT_SIZE);
            Pointer p = new Memory(size.getValue());
            if (0 != Libc.INSTANCE.sysctlbyname(String.format(name, cpu), p, size, null, 0)) {
                break;
            }
            sumTemp += p.getInt(0) / 10d - 273.15;
            cpu++;
        }
        if (cpu > 0) {
            return sumTemp / cpu;
        }
        // TODO try other ways here
        return 0d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getFanSpeeds() {
        // TODO try common software
        return new int[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuVoltage() {
        // TODO try common software
        return 0d;
    }
}
