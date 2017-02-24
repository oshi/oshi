/**
 * Oshi (https://github.com/oshi/oshi)
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.json.hardware;

import oshi.json.json.OshiJsonObject;

/**
 * Memory refers to the state information of a computing system, as it is kept
 * active in some physical structure. The term "memory" is used for the
 * information in physical systems which are fast (ie. RAM), as a distinction
 * from physical systems which are slow to access (ie. data storage). By design,
 * the term "memory" refers to temporary state devices, whereas the term
 * "storage" is reserved for permanent data.
 *
 * @author dblock[at]dblock[dot]org
 */
public interface GlobalMemory extends OshiJsonObject {
    /**
     * The amount of actual physical memory, in bytes.
     *
     * @return Total number of bytes.
     */
    long getTotal();

    /**
     * The amount of physical memory currently available, in bytes.
     *
     * @return Available number of bytes.
     */
    long getAvailable();

    /**
     * The current size of the paging/swap file(s), in bytes. If the paging/swap
     * file can be extended, this is a soft limit.
     *
     * @return Total swap in bytes.
     */
    long getSwapTotal();

    /**
     * The current memory committed to the paging/swap file(s), in bytes
     *
     * @return Swap used in bytes
     */
    long getSwapUsed();
}
