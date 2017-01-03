/**
 * Oshi (https://github.com/dblock/oshi)
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.mac;

import oshi.software.os.NetworkParams;

public class MacNetworkParams implements NetworkParams{
    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostName() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDnsServers() {
        return new String[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv4DefaultGateway() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv6DefaultGateway() {
        return "";
    }
}
