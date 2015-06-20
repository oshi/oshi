/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware;

/**
 * A hardware abstraction layer.
 * 
 * @author dblock[at]dblock[dot]org
 */
public interface HardwareAbstractionLayer {

	/**
	 * Get CPUs.
	 * 
	 * @return An array of Processor objects.
	 */
	Processor[] getProcessors();

	/**
	 * Get Memory information.
	 * 
	 * @return A memory object.
	 */
	Memory getMemory();

	/**
	 * Get Power Source information.
	 * 
	 * @return An array of PowerSource objects.
	 */
	PowerSource[] getPowerSources();
}
