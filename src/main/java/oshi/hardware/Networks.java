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
 * enrico[dot]bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware;

import oshi.hardware.stores.HWNetworkStore;
import oshi.json.OshiJsonObject;

/**
 * Networks refers to network interfaces installed in the machine.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public interface Networks extends OshiJsonObject {

    /**
     * Get network interfaces on this machine
     * 
     * @return Array of {@link HWNetworkStore} objects
     */
    HWNetworkStore[] getNetworks();
}
