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
package oshi.software.os.windows.nt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

import oshi.software.os.OSFileStore;

/**
 * The Windows File System contains {@link OSFileStore}s which are a storage
 * pool, device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Windows, these are represented by a drive
 * letter, e.g., "A:\" and "C:\"
 * 
 * @author widdis[at]gmail[dot]com
 */
public class WindowsFileSystem {
	/**
	 * Gets File System Information.
	 * 
	 * @return An array of {@link OSFileStore} objects representing mounted
	 *         volumes. May return disconnected volumes with
	 *         {@link OSFileStore#getTotalSpace()} = 0.
	 */
	public static OSFileStore[] getFileStores() {
		// File.listRoots() has more information for Windows
		// than FileSystem.getDefalut().getFileStores()
		File[] roots = File.listRoots();
		FileSystemView fsv = FileSystemView.getFileSystemView();
		List<OSFileStore> fsList = new ArrayList<>();
		for (File f : roots) {
			fsList.add(new OSFileStore(fsv.getSystemDisplayName(f), fsv
					.getSystemTypeDescription(f), f.getUsableSpace(), f
					.getTotalSpace()));
		}
		return fsList.toArray(new OSFileStore[fsList.size()]);
	}
}
