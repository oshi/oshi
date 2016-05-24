/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.windows;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;
import javax.swing.filechooser.FileSystemView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.jna.platform.windows.Kernel32;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;

/**
 * The Windows File System contains {@link OSFileStore}s which are a storage
 * pool, device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Windows, these are represented by a drive
 * letter, e.g., "A:\" and "C:\"
 * 
 * @author widdis[at]gmail[dot]com
 */
public class WindowsFileSystem extends AbstractFileSystem {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsFileSystem.class);

    public WindowsFileSystem() {
        // Set error mode to fail rather than prompt for FLoppy/CD-Rom
        Kernel32.INSTANCE.SetErrorMode(Kernel32.SEM_FAILCRITICALERRORS);
    }

    /**
     * Gets File System Information.
     * 
     * @return An array of {@link OSFileStore} objects representing mounted
     *         volumes. May return disconnected volumes with
     *         {@link OSFileStore#getTotalSpace()} = 0.
     */
    public OSFileStore[] getFileStores() {
        // File.listRoots() has more information for Windows
        // than FileSystem.getDefalut().getFileStores()
        final File[] roots = File.listRoots();
        // Need to call FileSystemView on Swing's Event Dispatch Thread to avoid
        // problems
        SwingWorker<List<OSFileStore>, Void> worker = new SwingWorker<List<OSFileStore>, Void>() {
            @Override
            public List<OSFileStore> doInBackground() {
                FileSystemView fsv = FileSystemView.getFileSystemView();
                List<OSFileStore> fsList = new ArrayList<>();
                for (File f : roots) {
                    String type = "unknown";
                    String path = "unknown";
                    try {
                        // add trailing slash to path if needed
                        path = f.getCanonicalPath();
                        if (path.charAt(path.length() - 1) != '\\') {
                            path = path + '\\';
                        }
                        char[] fstype = new char[16];
                        if (Kernel32.INSTANCE.GetVolumeInformation(path, null, 0, null, null, null, fstype, 16)) {
                            type = new String(fstype).trim();
                        }
                    } catch (IOException e) {
                        LOG.error("Could not get canonical path for {}", f.toString());
                    }
                    fsList.add(new OSFileStore(fsv.getSystemDisplayName(f), path, fsv.getSystemTypeDescription(f), type,
                            f.getUsableSpace(), f.getTotalSpace()));
                }
                return fsList;
            }
        };
        worker.execute();
        List<OSFileStore> fs = new ArrayList<>();
        try {
            // TODO: Consider a timeout version of this method that passes
            // timeout parameters which are used in this get()
            fs = worker.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("", e);
        }
        return fs.toArray(new OSFileStore[fs.size()]);
    }

    @Override
    public long getOpenFileDescriptors() {
        return 0L;
    }

    @Override
    public long getMaxFileDescriptors() {
        return 0L;
    }
}
