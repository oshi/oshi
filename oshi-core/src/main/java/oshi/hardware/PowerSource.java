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
package oshi.hardware;

import java.io.Serializable;

/**
 * The Power Source is one or more batteries with some capacity, and some state
 * of charge/discharge
 *
 * @author widdis[at]gmail[dot]com
 * @author ethanjaszewski[at]yahoo[dot]com
 */
public interface PowerSource extends Serializable {
    /**
     * Name of the power source (e.g., InternalBattery-0)
     *
     * @return The power source name
     */
    String getName();

    /**
     * Remaining capacity as a fraction of max capacity.
     *
     * @return A value between 0.0 (fully drained) and 1.0 (fully charged)
     */
    double getRemainingCapacity();

    /**
     * Estimated time remaining on the power source, in seconds.
     *
     * @return If positive, seconds remaining. If negative, -1.0 (calculating)
     *         or -2.0 (unlimited)
     */
    double getTimeRemaining();
    
    /**
     * Power source health as a fraction of the battery's designed capacity.
     * 
     * @return A value between 0.0 (no capacity) and 1.0 (at designed capacity)
     */
    double getHealth();
    
    /**
     * Maximum charge of the power source, in milliwatt hours.
     * 
     * @return If positive, milliwatt hours of charge. If negative, the maximum charge is unknown.
     */
    long getMaximumCharge();
    
    /**
     * Remaining charge of the power source, in milliwatt hours.
     * 
     * @return If positive, milliwatt hours of charge. If negative, the Remaining charge is unknown. 
     */
    long getRemainingCharge();
    
    /**
     * Current power supplied by the power source, in milliwatts.
     * @return If positive, milliwats of power. If negative, the power is unknown.
     */
    long getPower();
}
