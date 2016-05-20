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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.windows;

import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import java.util.ArrayList;
import java.util.List;

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
 * @author enrico[dot]bianchi[at]gmail[dot]com
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
        List<OSFileStore> fs = new ArrayList<>();
        final int LENGTH = 255;
        char[] volume = new char[LENGTH];

        WinNT.HANDLE hVol = Kernel32.INSTANCE.FindFirstVolume(volume, LENGTH);
        if (hVol == WinNT.INVALID_HANDLE_VALUE) {
            return fs.toArray(new OSFileStore[0]);
        }

        while (true) {
            char[] fstype = new char[16];
            char[] name = new char[LENGTH];
            char[] mount = new char[LENGTH];
            WinNT.LARGE_INTEGER userFreeBytes = new WinNT.LARGE_INTEGER(0L);
            WinNT.LARGE_INTEGER totalBytes = new WinNT.LARGE_INTEGER(0L);
            WinNT.LARGE_INTEGER systemFreeBytes = new WinNT.LARGE_INTEGER(0L);
        
            Kernel32.INSTANCE.GetVolumeInformation(new String(volume).trim(), name, LENGTH, null, null, null, fstype, 16);
            Kernel32.INSTANCE.GetVolumePathNamesForVolumeName(new String(volume).trim(), mount, LENGTH, null);
            Kernel32.INSTANCE.GetDiskFreeSpaceEx(new String(volume).trim(), userFreeBytes, totalBytes, systemFreeBytes);

            fs.add(new OSFileStore(new String(volume).trim(), new String(mount).trim(),
                    new String(name).trim(), new String(fstype).trim(),
                    systemFreeBytes.getValue(), totalBytes.getValue()));

            boolean retVal = Kernel32.INSTANCE.FindNextVolume(hVol, volume, LENGTH);
            if (!retVal) {
                Kernel32.INSTANCE.FindVolumeClose(hVol);
                break;
            }
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
