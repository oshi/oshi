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
package oshi.json.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

import oshi.software.os.OperatingSystem.ProcessSort;

/**
 * Test properties loader
 */
public class PropertiesUtilTest {
    private static final String PROPS_FILE = "oshi.json.properties";
    private static String NO_FILE = "does.not.exist";

    /**
     * Test load properties
     */
    @Test
    public void testLoadProperties() {
        Properties props;

        props = PropertiesUtil.loadProperties(NO_FILE);
        assertEquals(0, props.size());

        props = PropertiesUtil.loadProperties(PROPS_FILE);
        assertEquals(5, PropertiesUtil.getIntOrDefault(props, "operatingSystem.processes.limit", 0));
        assertEquals(3, PropertiesUtil.getIntOrDefault(props, "operatingSystem.processes.nokey", 3));

        assertTrue(PropertiesUtil.getBoolean(props, "hardware.usbDevices.tree"));
        assertTrue(PropertiesUtil.getBoolean(props, "hardware.usbDevices.nokey"));

        assertEquals("5", PropertiesUtil.getString(props, "operatingSystem.processes.limit"));
        assertEquals("null", PropertiesUtil.getString(props, "operatingSystem.processes.nokey"));

        assertEquals(ProcessSort.CPU,
                PropertiesUtil.getEnum(props, "operatingSystem.processes.sort", ProcessSort.class));
        assertEquals(null, PropertiesUtil.getEnum(props, "operatingSystem.processes.limit", ProcessSort.class));

    }
}
