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
package oshi.util.platform.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.IOKit.MachPort;

/**
 * Provides utilities for IOKit
 * 
 * @author widdis[at]gmail[dot]com
 */
public class IOKitUtil {
    private static final Logger LOG = LoggerFactory.getLogger(IOKitUtil.class);

    /**
     * Opens an IOService matching the given name
     * 
     * @param serviceName
     *            The service name to match
     * @return an IOService if successful, 0 if failed
     */
    public static int getMatchingService(String serviceName) {
        MachPort masterPort = new MachPort();

        int result = IOKit.INSTANCE.IOMasterPort(0, masterPort);
        if (result != 0) {
            LOG.error(String.format("Error: IOMasterPort() = %08x", result));
            return 0;
        }

        int service = IOKit.INSTANCE.IOServiceGetMatchingService(masterPort.getValue(),
                IOKit.INSTANCE.IOServiceMatching(serviceName));
        if (service == 0) {
            LOG.error("No service found: {}", serviceName);
        }
        return service;
    }
}