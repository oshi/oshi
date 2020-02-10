/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.common;

import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

public abstract class AbstractFileSystem implements FileSystem {

    private static final String[] NETWORK_FS_TYPES = { "afs", "cifs", "smbfs", "sshfs", "ncpfs", "ncp", "nfs", "nfs4",
            "gfs", "gds2", "glusterfs" };

    @Override
    public OSFileStore[] getFileStores() {
        return getFileStores(false);
    }

    /**
     * Determine whether a file system type is a network file system
     *
     * @param type
     *            The type to test
     * @return {@code true} if the filesystem is a network type, {@code false}
     *         otherwise.
     */
    protected static boolean isNetworkFsType(String type) {
        for (String fsType : NETWORK_FS_TYPES) {
            if (fsType.equals(type)) {
                return true;
            }
        }
        return false;
    }
}
