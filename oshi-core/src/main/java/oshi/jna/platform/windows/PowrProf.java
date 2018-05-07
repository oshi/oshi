/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.jna.platform.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Power profile stats. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface PowrProf extends Library {
    PowrProf INSTANCE = Native.loadLibrary("PowrProf", PowrProf.class);

    int SYSTEM_BATTERY_STATE = 5;

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

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "acOnLine", "batteryPresent", "charging", "discharging", "spare1",
                    "maxCapacity", "remainingCapacity", "rate", "estimatedTime", "defaultAlert1", "defaultAlert2" });
        }
    }

    int CallNtPowerInformation(int informationLevel, Pointer lpInputBuffer, NativeLong nInputBufferSize,
            Structure lpOutputBuffer, NativeLong nOutputBufferSize);
}
