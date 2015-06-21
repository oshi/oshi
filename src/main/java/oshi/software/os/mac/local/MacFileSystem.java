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
package oshi.software.os.mac.local;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileSystemView;

import oshi.software.os.OSFileStore;

/**
 * The Mac File System contains {@link OSFileStore}s which are a storage pool,
 * device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Mac OS X, these are found in the /Volumes
 * directory.
 * 
 * @author widdis[at]gmail[dot]com
 */
public class MacFileSystem {
	// Regexp matcher for /dev/disk1 etc.
	private static final Pattern localDisk = Pattern.compile("/dev/disk\\d");

	/**
	 * Gets File System Information.
	 * 
	 * @return An array of {@link OSFileStore} objects representing mounted
	 *         volumes. May return disconnected volumes with
	 *         {@link OSFileStore#getTotalSpace()} = 0.
	 * @throws IOException
	 */
	public static OSFileStore[] getFileStores() {
		List<OSFileStore> fsList = new ArrayList<>();
		FileSystemView fsv = FileSystemView.getFileSystemView();
		// Mac file systems are mounted in /Volumes
		File volumes = new File("/Volumes");
		if (volumes != null && volumes.listFiles() != null)
			for (File f : volumes.listFiles()) {
				// Everyone hates DS Store
				if (f.getName().endsWith(".DS_Store"))
					continue;
				String name = fsv.getSystemDisplayName(f);
				String description = "Volume";
				try {
					if (f.getCanonicalPath().equals("/"))
						name = name + " (/)";
					FileStore fs = Files.getFileStore(f.toPath());
					if (localDisk.matcher(fs.name()).matches())
						description = "Local Disk";
					if (fs.name().startsWith("localhost:")
							|| fs.name().startsWith("//"))
						description = "Network Drive";
				} catch (IOException e) {
					continue;
				}
				fsList.add(new OSFileStore(name, description, f
						.getUsableSpace(), f.getTotalSpace()));
			}
		return fsList.toArray(new OSFileStore[fsList.size()]);
	}
}
