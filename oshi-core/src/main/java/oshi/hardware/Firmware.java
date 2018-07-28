/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
package oshi.hardware;

import java.io.Serializable;

/**
 * The Firmware represents the low level BIOS or equivalent
 *
 * @author SchiTho1 [at] Securiton AG
 */
public interface Firmware extends Serializable {

    /**
     * Get the firmware manufacturer.
     * 
     * @return the manufacturer
     */
    String getManufacturer();

    /**
     * Get the firmware name.
     * 
     * @return the name
     */
    String getName();

    /**
     * Get the firmware description.
     * 
     * @return the description
     */
    String getDescription();

    /**
     * Get the firmware version.
     * 
     * @return the version
     */
    String getVersion();

    /**
     * Get the firmware release date.
     * 
     * @return The date in ISO 8601 YYYY-MM-DD format.
     */
    String getReleaseDate();
}
