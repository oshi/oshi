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
package oshi.software.os.linux.proc;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

import oshi.software.os.OSFileStore;

/**
 * The Mac File System contains {@link OSFileStore}s which are a storage pool,
 * device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Linux, these are found in the /proc/mount
 * filesystem, excluding temporary and kernel mounts.
 * 
 * @author widdis[at]gmail[dot]com
 */
public class LinuxFileSystem {
	/**
	 * Gets File System Information.
	 * 
	 * @return An array of {@link FileStore} objects representing mounted
	 *         volumes. May return disconnected volumes with
	 *         {@link OSFileStore#getTotalSpace()} = 0.
	 */
	public static OSFileStore[] getFileStores() {
		List<OSFileStore> fsList = new ArrayList<>();
		for (FileStore store : FileSystems.getDefault().getFileStores()) {
			// FileStore toString starts with path, then a space, then name in
			// parentheses e.g., "/ (/dev/sda1)" and "/proc (proc)"
			String path = store.toString().replace(" (" + store.name() + ")",
					"");
			// Exclude special directories
			if (path.startsWith("/proc") || path.startsWith("/sys")
					|| path.startsWith("/run") || path.equals("/dev")
					|| path.equals("/dev/pts"))
				continue;
			String name = store.name();
			if (path.equals("/"))
				name = "/";
			String description = "Mount Point";
			if (store.name().startsWith("/dev"))
				description = "Local Disk";
			try {
				fsList.add(new OSFileStore(name, description, store
						.getUsableSpace(), store.getTotalSpace()));
			} catch (IOException e) {
				// get*Space() may fail for ejected CD-ROM, etc.
				continue;
			}
		}
		return fsList.toArray(new OSFileStore[fsList.size()]);
	}
}
