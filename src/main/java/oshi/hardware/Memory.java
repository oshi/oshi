/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
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
