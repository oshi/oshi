/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os;

/**
 * An operating system (OS) is the software on a computer that manages the way
 * different programs use its hardware, and regulates the ways that a user
 * controls the computer.
 * 
 * @author dblock[at]dblock[dot]org
 */
public interface OperatingSystem {

	/**
	 * Operating system family.
	 * 
	 * @return String.
	 */
	public String getFamily();

	/**
	 * Manufacturer.
	 * 
	 * @return String.
	 */
	public String getManufacturer();

	/**
	 * Operating system version.
	 * 
	 * @return Version.
	 */
	OperatingSystemVersion getVersion();
}
