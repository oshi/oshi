/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os;

/**
 * An operating system (OS) is the software on a computer that manages the way different programs 
 * use its hardware, and regulates the ways that a user controls the computer.
 * @author dblock[at]dblock[dot]org
 */
public interface OperatingSystem {
	
	/**
	 * Operating systme family.
	 * @return
	 *  String.
	 */
	public String getFamily();
	
	/**
	 * Manufacturer.
	 * @return
	 *  String.
	 */
	public String getManufacturer();

	/**
	 * Operating system version.
	 * @return
	 *  Version.
	 */
	OperatingSystemVersion getVersion();
	
	/**
	 * The Kernel manages processes, memory, devices and system calls.
	 * @return
	 *  A Kernel object.
	 */
	Kernel getKernel();	
}
