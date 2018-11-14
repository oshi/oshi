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
package oshi.json.hardware;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import oshi.json.SystemInfo;

/**
 * Tests Computer System
 */
public class ComputerSystemTest {

    /**
     * Test Computer System
     */
    @Test
    public void testComputerSystem() {
        SystemInfo si = new SystemInfo();
        ComputerSystem cs = si.getHardware().getComputerSystem();
        assertNotNull(cs.getManufacturer());
        assertNotNull(cs.getModel());
        assertNotNull(cs.getSerialNumber());

        Firmware fw = cs.getFirmware();
        assertNotNull(fw);
        assertNotNull(fw.getManufacturer());
        assertNotNull(fw.getName());
        assertNotNull(fw.getDescription());
        assertNotNull(fw.getVersion());
        assertNotNull(fw.getReleaseDate());

        Baseboard bb = cs.getBaseboard();
        assertNotNull(bb);
        assertNotNull(bb.getManufacturer());
        assertNotNull(bb.getModel());
        assertNotNull(bb.getVersion());
        assertNotNull(bb.getSerialNumber());
    }
}
