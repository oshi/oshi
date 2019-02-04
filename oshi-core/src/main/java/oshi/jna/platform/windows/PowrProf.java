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
import com.sun.jna.platform.win32.NTStatus;

/**
 * Power profile stats. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface PowrProf extends Library {
    PowrProf INSTANCE = Native.load("PowrProf", PowrProf.class);

    /**
     * Indicates power level information.
     */
    public interface POWER_INFORMATION_LEVEL {
        int LAST_SLEEP_TIME = 15;
        int LAST_WAKE_TIME = 14;
        int PROCESSOR_INFORMATION = 11;
        int SYSTEM_BATTERY_STATE = 5;
        int SYSTEM_EXECUTION_STATE = 16;
        int SYSTEM_POWER_CAPABILITIES = 4;
        int SYSTEM_POWER_INFORMATION = 12;
        int SYSTEM_POWER_POLICY_AC = 0;
        int SYSTEM_POWER_POLICY_CURRENT = 8;
        int SYSTEM_POWER_POLICY_DC = 1;
        int SYSTEM_RESERVE_HIBER_FILE = 10;
    }

    /**
     * Contains information about the current state of the system battery.
     */
    @FieldOrder({ "acOnLine", "batteryPresent", "charging", "discharging", "spare1", "tag", "maxCapacity",
            "remainingCapacity", "rate", "estimatedTime", "defaultAlert1", "defaultAlert2" })
    class SystemBatteryState extends Structure {
        public byte acOnLine;
        public byte batteryPresent;
        public byte charging;
        public byte discharging;
        public byte[] spare1 = new byte[3];
        public byte tag;
        public int maxCapacity;
        public int remainingCapacity;
        public int rate;
        public int estimatedTime;
        public int defaultAlert1;
        public int defaultAlert2;

        public SystemBatteryState(Pointer p) {
            super(p);
            read();
        }

        public SystemBatteryState() {
            super();
        }
    }

    /**
     * Contains information about a processor.
     */
    @FieldOrder({ "number", "maxMhz", "currentMhz", "mhzLimit", "maxIdleState", "currentIdleState" })
    class ProcessorPowerInformation extends Structure {
        public int number;
        public int maxMhz;
        public int currentMhz;
        public int mhzLimit;
        public int maxIdleState;
        public int currentIdleState;

        public ProcessorPowerInformation(Pointer p) {
            super(p);
            read();
        }

        public ProcessorPowerInformation() {
            super();
        }
    }

    /**
     * Sets or retrieves power information.
     * <p>
     * Changes made to the current system power policy using
     * {@link CallNtPowerInformation} are immediate, but they are not
     * persistent; that is, the changes are not stored as part of a power
     * scheme. Any changes to system power policy made with
     * {@link CallNtPowerInformation} may be overwritten by changes to a policy
     * scheme made by the user in the Power Options control panel program, or by
     * subsequent calls to {@code WritePwrScheme}, {@code SetActivePwrScheme},
     * or other power scheme functions.
     * 
     * @param informationLevel
     *            The information level requested. This value indicates the
     *            specific power information to be set or retrieved. This
     *            parameter must be one of the following
     *            {@link POWER_INFORMATION_LEVEL} enumeration type values:
     *            {@link POWER_INFORMATION_LEVEL#LAST_SLEEP_TIME},
     *            {@link POWER_INFORMATION_LEVEL#LAST_WAKE_TIME},
     *            {@link POWER_INFORMATION_LEVEL#PROCESSOR_INFORMATION},
     *            {@link POWER_INFORMATION_LEVEL#SYSTEM_BATTERY_STATE},
     *            {@link POWER_INFORMATION_LEVEL#SYSTEM_EXECUTION_STATE},
     *            {@link POWER_INFORMATION_LEVEL#SYSTEM_POWER_CAPABILITIES},
     *            {@link POWER_INFORMATION_LEVEL#SYSTEM_POWER_INFORMATION},
     *            {@link POWER_INFORMATION_LEVEL#SYSTEM_POWER_POLICY_AC},
     *            {@link POWER_INFORMATION_LEVEL#SYSTEM_POWER_POLICY_CURRENT},
     *            {@link POWER_INFORMATION_LEVEL#SYSTEM_POWER_POLICY_DC}, or
     *            {@link POWER_INFORMATION_LEVEL#SYSTEM_RESERVE_HIBER_FILE}.
     * @param lpInputBuffer
     *            A pointer to an optional input buffer. The data type of this
     *            buffer depends on the information level requested in the
     *            {@code informationLevel} parameter.
     * @param nInputBufferSize
     *            The size of the input buffer, in bytes.
     * @param lpOutputBuffer
     *            A pointer to an optional output buffer. The data type of this
     *            buffer depends on the information level requested in the
     *            {@code informationLevel} parameter. If the buffer is too small
     *            to contain the information, the function returns
     *            {@link NTStatus#STATUS_BUFFER_TOO_SMALL}.
     * @param nOutputBufferSize
     *            The size of the output buffer, in bytes. Depending on the
     *            information level requested, this may be a variably sized
     *            buffer.
     * @return If the function succeeds, the return value is
     *         {@link NTStatus#STATUS_SUCCESS}. If the function fails, the
     *         return value can be one the following status codes:
     *         {@link NTStatus#STATUS_BUFFER_TOO_SMALL} if the output buffer is
     *         of insufficient size to contain the data to be returned.
     *         {@code NTStatus#STATUS_ACCESS_DENIED} TODO if the caller had
     *         insufficient access rights to perform the requested action.
     */
    int CallNtPowerInformation(int informationLevel, Pointer lpInputBuffer, int nInputBufferSize,
            Pointer lpOutputBuffer, int nOutputBufferSize);
}
