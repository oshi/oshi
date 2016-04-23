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
package oshi.hardware;

import oshi.hardware.common.HWDiskStore;
import oshi.json.OshiJsonObject;

/**
 * Disks refers to hard drives installed in the machine.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public interface Disks extends OshiJsonObject {

    /**
     * Get hard drives on this machine
     * 
     * @return Array of {@link HWDiskStore} objects
     */
    public HWDiskStore[] getDisks();
}
