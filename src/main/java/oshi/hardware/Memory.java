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
 * Memory refers to the state information of a computing system, as it is kept
 * active in some physical structure. The term "memory" is used for the
 * information in physical systems which are fast (ie. RAM), as a distinction
 * from physical systems which are slow to access (ie. data storage). By design,
 * the term "memory" refers to temporary state devices, whereas the term
 * "storage" is reserved for permanent data.
 * 
 * @author dblock[at]dblock[dot]org
 */
public interface Memory {
	/**
	 * Total memory.
	 * 
	 * @return Total number of bytes.
	 */
	long getTotal();

	/**
	 * Currently available.
	 * 
	 * @return Available number of bytes.
	 */
	long getAvailable();
}
