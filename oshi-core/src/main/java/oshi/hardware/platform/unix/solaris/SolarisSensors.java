/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;
import java.util.List;

import oshi.hardware.common.AbstractSensors;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

public class SolarisSensors extends AbstractSensors {

    private static final long serialVersionUID = 1L;

    /*-
     * 
     
     #prtpicl -v -c fan
    f2_rs (fan, 4c00000923)
    :_fru_parent   (4c000006ccH)
    :Label         RS
    :SpeedUnit      rpm
    :LowWarningThreshold   0x7d0
    :Speed         0x113b
    :_class        fan
    :name  f2_rs
    f3_rs (fan, 4c0000092a)
    :_fru_parent   (4c000006d1H)
    :Label         RS
    :SpeedUnit      rpm
    :LowWarningThreshold   0x7d0
    :Speed         0xf11
    :_class        fan
    :name  f3_rs
    ...
     
     
     optname="$optname fire_t_core mb_io_t_amb c0_p0_t_core ft0_f0_tach"        ## v440
    optname="$optname cpu0 cpu0-fan int-amb0 int-amb1 dimm-fan"     ## b2k
    optname="$optname cpu0 cpu0-ambient cpu system" ## b1k
     
    cmdfan="/usr/sbin/prtpicl -v -c fan"
    iddsend=":name"
    idkey=":name"
    idval=":Temperature"
    idvalsp=":Speed"
    idvalsu=":SpeedUnit"
    
     prtpicl -c voltage-sensor -v 
    prtpicl -c voltage-indicator -v 
    
    
    prtpicl -c fan-tachometer -v  
    prtpicl -c rpm-sensor -v
    
    
      prtpicl -c temperature-sensor -v
    CPU-sensor (temperature-sensor, 2600000041f)
            :Temperature            74 
    may repeat multiple cpus
    
    
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuTemperature() {
        double maxTemp = 0d;
        ArrayList<String> temps = ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c temperature-sensor");
        if (temps != null) {
            // Return max found temp
            for (String line : temps) {
                if (line.trim().startsWith("Temperature:")) {
                    int temp = ParseUtil.parseLastInt(line, 0);
                    if (temp > maxTemp) {
                        maxTemp = temp;
                    }
                }
            }

        }
        // If it's in millidegrees:
        if (maxTemp > 1000) {
            maxTemp /= 1000;
        }
        return maxTemp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getFanSpeeds() {
        List<Integer> speedList = new ArrayList<>();
        ArrayList<String> speeds = ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c fan");
        if (speeds != null) {
            // Return max found temp
            for (String line : speeds) {
                if (line.trim().startsWith("Speed:")) {
                    speedList.add(ParseUtil.parseLastInt(line, 0));
                }
            }
        }
        int[] fans = new int[speedList.size()];
        for (int i = 0; i < speedList.size(); i++) {
            fans[i] = speedList.get(i);
        }
        return fans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuVoltage() {
        double voltage = 0d;
        ArrayList<String> volts = ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c voltage-sensor");
        // TODO This is entirely a guess!
        if (volts != null) {
            for (String line : volts) {
                if (line.trim().startsWith("Voltage:")) {
                    voltage = ParseUtil.parseDoubleOrDefault(line.replace("Voltage:", "").trim(), 0d);
                    break;
                }
            }

        }
        return voltage;
    }
}
