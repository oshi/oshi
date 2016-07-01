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
package oshi.hardware;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import oshi.SystemInfo;

/**
 * Test Sensors
 */
public class SensorsTest {
    /**
     * Test sensors
     */
    @Test
    public void testSensors() {
        SystemInfo si = new SystemInfo();
        Sensors s = si.getHardware().getSensors();
        assertTrue(s.getCpuTemperature() >= 0d && s.getCpuTemperature() <= 100d);
        int[] speeds = s.getFanSpeeds();
        for (int fan = 0; fan < speeds.length; fan++) {
            assertTrue(speeds[fan] >= 0);
        }
        assertTrue(s.getCpuVoltage() >= 0);
    }
}
