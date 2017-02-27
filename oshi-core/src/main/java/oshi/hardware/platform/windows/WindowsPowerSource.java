/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
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
package oshi.hardware.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.NativeLong;

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.jna.platform.windows.PowrProf;
import oshi.jna.platform.windows.PowrProf.SystemBatteryState;
import oshi.util.FormatUtil;

/**
 * A Power Source
 *
 * @author widdis[at]gmail[dot]com
 */
public class WindowsPowerSource extends AbstractPowerSource {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsPowerSource.class);

    public WindowsPowerSource() {
        super();
        LOG.debug("Initialized WindowsPowerSource");
    }
    
    @Deprecated
    public WindowsPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
        super(newName, newRemainingCapacity, newTimeRemaining);
        LOG.debug("Initialized WindowsPowerSource");
    }

    /**
     * Gets Battery Information.
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static PowerSource[] getPowerSources() {
        // Windows provides a single unnamed battery
        WindowsPowerSource[] psArray = new WindowsPowerSource[1];
        WindowsPowerSource pSource = new WindowsPowerSource();
        pSource.setName("System Battery");
        // Get structure
        SystemBatteryState batteryState = new SystemBatteryState();
        
        if(0 == PowrProf.INSTANCE.CallNtPowerInformation(PowrProf.SYSTEM_BATTERY_STATE, null, new NativeLong(0),
                batteryState, new NativeLong(batteryState.size())) && batteryState.batteryPresent != 0) {
            int timeRemaining = -2; // -1 = unknown, -2 = unlimited
            if (batteryState.acOnLine == 0 && batteryState.charging == 0 && batteryState.discharging > 0) {
                timeRemaining = batteryState.estimatedTime;
            }
            long remainingCapacity = FormatUtil.getUnsignedInt(batteryState.remainingCapacity);
            long power = -FormatUtil.getUnsignedInt(batteryState.rate); // Windows defines discharge as rate < 0, we want opposite.
            double remainingCharge;
            if(timeRemaining < 0)
                remainingCharge = (power * timeRemaining) / 60;
            else
                remainingCharge = 0;
            
            pSource.setRemainingCapacity(remainingCapacity);
            pSource.setTimeRemaining(timeRemaining);
            pSource.setMaximumCharge((long) (remainingCharge / remainingCapacity));
            pSource.setRemainingCharge((long) remainingCharge); 
            pSource.setPower((long) power);
        }
        
        psArray[0] = pSource;
        
        return psArray;
    }
}
