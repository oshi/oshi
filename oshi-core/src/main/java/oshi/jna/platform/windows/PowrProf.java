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
package oshi.jna.platform.windows;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * Power profile stats. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface PowrProf extends Library {
    PowrProf INSTANCE = Native.load("PowrProf", PowrProf.class);

    int SYSTEM_BATTERY_STATE = 5;
    int PROCESSOR_INFORMATION = 11;

    @FieldOrder({ "acOnLine", "batteryPresent", "charging", "discharging", "spare1", "maxCapacity", "remainingCapacity",
            "rate", "estimatedTime", "defaultAlert1", "defaultAlert2" })
    class SystemBatteryState extends Structure {
        public byte acOnLine; // boolean
        public byte batteryPresent; // boolean
        public byte charging; // boolean
        public byte discharging; // boolean
        public byte[] spare1 = new byte[4]; // unused
        public int maxCapacity; // unsigned 32 bit
        public int remainingCapacity; // unsigned 32 bit
        public int rate; // signed 32 bit
        public int estimatedTime; // signed 32 bit
        public int defaultAlert1; // unsigned 32 bit
        public int defaultAlert2; // unsigned 32 bit
    }

    @FieldOrder({ "Number", "MaxMhz", "CurrentMhz", "MhzLimit", "MaxIdleState", "CurrentIdleState" })
    class ProcessorPowerInformation extends Structure {
        public int Number; // unsigned 32 bit
        public int MaxMhz; // unsigned 32 bit
        public int CurrentMhz; // unsigned 32 bit
        public int MhzLimit; // unsigned 32 bit
        public int MaxIdleState; // unsigned 32 bit
        public int CurrentIdleState; // unsigned 32 bit
    }

    int CallNtPowerInformation(int informationLevel, Pointer lpInputBuffer, int nInputBufferSize,
            Structure lpOutputBuffer, int nOutputBufferSize);
}
