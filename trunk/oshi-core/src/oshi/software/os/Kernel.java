/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os;

import oshi.hardware.Memory;

/**
 * On the definition of "kernel", Jochen Liedtke said that the word is "traditionally used to denote the 
 * part of the operating system that is mandatory and common to all other software."
 * @author dblock[at]dblock[dot]org
 */
public interface Kernel {
	/**
	 * Get processes.
	 * @return
	 *  A list of operating system processes.
	 */
	Process[] getProcesses();
	
	/**
	 * Get memory.
	 * @return
	 *  Operating system memory.
	 */
	Memory getMemory();
}
