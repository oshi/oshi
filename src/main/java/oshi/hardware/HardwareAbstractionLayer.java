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

import oshi.software.os.OSFileStore;

/**
 * A hardware abstraction layer. Provides access to hardware items such as
 * processors, memory, battery, and disks.
 * 
 * @author dblock[at]dblock[dot]org
 */
public interface HardwareAbstractionLayer {

	/**
	 * Instantiates an array of {@link Processor} objects. This represents one
	 * or more Logical CPUs.
	 * 
	 * @return An array of {@link Processor} objects.
	 */
	Processor[] getProcessors();

	/**
	 * Instantiates a {@link Memory} object.
	 * 
	 * @return A memory object.
	 */
	Memory getMemory();

	/**
	 * Instantiates an array of {@link PowerSource} objects, representing
	 * batteries, etc.
	 * 
	 * @return An array of PowerSource objects or an empty array if none are
	 *         present.
	 */
	PowerSource[] getPowerSources();

	/**
	 * Instantiates an array of {@link OSFileStore} objects, representing a
	 * storage pool, device, partition, volume, concrete file system or other
	 * implementation specific means of file storage.
	 * 
	 * @return An array of OSFileStore objects or an empty array if none are
	 *         present.
	 */
	OSFileStore[] getFileStores();
}
