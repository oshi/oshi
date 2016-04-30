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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworks;

/**
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class MacNetworks extends AbstractNetworks {

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNetworkStats(NetworkIF netIF) {
        // TODO: set network stats on Mac
        netIF.setBytesSent(0);
        netIF.setBytesRecv(0);
        netIF.setPacketsSent(0);
        netIF.setPacketsRecv(0);
        netIF.setSpeed(0);
    }
}
