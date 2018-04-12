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
package oshi.json.software.os;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import oshi.json.SystemInfo;

/**
 * Test network parameters
 */
public class NetworkParamsTest {

    /**
     * Test network parameters
     */
    @Test
    public void testOperatingSystem() {
        SystemInfo si = new SystemInfo();
        NetworkParams params = si.getOperatingSystem().getNetworkParams();
        assertNotNull(params.getHostName());
        assertNotNull(params.getDomainName());
        assertNotNull(params.getDnsServers());
        assertNotNull(params.getIpv4DefaultGateway());
        assertNotNull(params.getIpv6DefaultGateway());
    }
}
