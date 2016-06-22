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
package oshi.json.hardware;

import oshi.json.json.OshiJsonObject;

/**
 * {@inheritDoc}
 */
public interface UsbDevice extends oshi.hardware.UsbDevice, OshiJsonObject {
    /**
     * {@inheritDoc}
     */
    String getName();

    /**
     * {@inheritDoc}
     */
    String getVendor();

    /**
     * {@inheritDoc}
     */
    String getVendorId();

    /**
     * {@inheritDoc}
     */
    String getProductId();

    /**
     * {@inheritDoc}
     */
    String getSerialNumber();

    /**
     * {@inheritDoc}
     */
    UsbDevice[] getConnectedDevices();
}
