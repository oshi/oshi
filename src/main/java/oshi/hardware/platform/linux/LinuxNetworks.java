/*
 * Copyright (c) 2016 com.github.dblock.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * enrico[dot]bianchi[at]gmail[dot]com
 *    com.github.dblock - initial API and implementation and/or initial documentation
 */
package oshi.hardware.platform.linux;

import oshi.hardware.common.AbstractNetworks;
import oshi.hardware.stores.HWNetworkStore;

/**
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class LinuxNetworks extends AbstractNetworks {

    @Override
    public HWNetworkStore[] getNetworks() {
        // TODO: implement network interfaces extraction
        return new HWNetworkStore[]{};
    }
}
