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
package oshi.software.os;

/**
 * The File System is a storage pool, device, partition, volume, concrete file
 * system or other implementation specific means of file storage. See subclasses
 * for definitions as they apply to specific platforms.
 * 
 * @author widdis[at]gmail[dot]com
 */
public class OSFileStore {
	private String name;

	private String description;

	private long usableSpace;

	private long totalSpace;

	/**
	 * Creates a {@link OSFileStore} with the specified parameters.
	 * 
	 * @param name
	 * @param description
	 * @param usableSpace
	 * @param totalSpace
	 */
	public OSFileStore(String name, String description, long usableSpace,
			long totalSpace) {
		this.setName(name);
		this.setDescription(description);
		this.setUsableSpace(usableSpace);
		this.setTotalSpace(totalSpace);
	}

	/**
	 * Name of the File System
	 * 
	 * @return The file system name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the File System name
	 * 
	 * @param name
	 *            The name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Description of the File System
	 * 
	 * @return The file system description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Sets the File System description
	 * 
	 * @param description
	 *            The description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Usable space on the drive.
	 * 
	 * @return Usable space on the drive (in bytes)
	 */
	public long getUsableSpace() {
		return this.usableSpace;
	}

	/**
	 * Sets usable space on the drive.
	 * 
	 * @param usableSpace
	 *            Bytes of writable space.
	 */
	public void setUsableSpace(long usableSpace) {
		this.usableSpace = usableSpace;
	}

	/**
	 * Total space/capacity of the drive.
	 * 
	 * @return Total capacity of the drive (in bytes)
	 */
	public long getTotalSpace() {
		return this.totalSpace;
	}

	/**
	 * Sets the total space on the drive.
	 * 
	 * @param totalSpace
	 *            Bytes of total space.
	 */
	public void setTotalSpace(long totalSpace) {
		this.totalSpace = totalSpace;
	}
}
