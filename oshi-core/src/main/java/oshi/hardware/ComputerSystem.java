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
package oshi.hardware;

/**
 * ComputerSystem Object.
 * 
 * <P>
 * This represents the physical hardware, such as a motherboard, logic board,
 * etc.
 * 
 * @author SchiTho1 @ Securiton AG
 */
public interface ComputerSystem {
    /**
     * Get the computer system manufacturer.
     * 
     * @return The manufacturer.
     */
    String getManufacturer();

    /**
     * Get the computer system model.
     * 
     * @return The model.
     */
    String getModel();

    /**
     * Get the computer system serial number
     * 
     * @return The serial number.
     */
    String getSerialNumber();
}
