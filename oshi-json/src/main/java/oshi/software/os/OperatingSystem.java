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
package oshi.software.os;

import oshi.json.OshiJsonObject;

/**
 * An operating system (OS) is the software on a computer that manages the way
 * different programs use its hardware, and regulates the ways that a user
 * controls the computer.
 * 
 * @author dblock[at]dblock[dot]org
 */
public interface OperatingSystem extends OshiJsonObject {

    /**
     * Operating system family.
     * 
     * @return String.
     */
    String getFamily();

    /**
     * Manufacturer.
     * 
     * @return String.
     */
    String getManufacturer();

    /**
     * Operating system version.
     * 
     * @return Version.
     */
    OperatingSystemVersion getVersion();
}
