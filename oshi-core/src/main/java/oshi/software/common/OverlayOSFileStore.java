/**
 * MIT License
 * <p>
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.common;

import oshi.software.os.linux.LinuxOSFileStore;
import oshi.software.os.mac.MacOSFileStore;
import oshi.software.os.unix.aix.AixOSFileStore;
import oshi.software.os.unix.freebsd.FreeBsdOSFileStore;
import oshi.software.os.unix.solaris.SolarisOSFileStore;
import oshi.software.os.windows.WindowsOSFileStore;

/**
 * OverlayFileSystem implementation.
 */
public class OverlayOSFileStore implements OSFileStoreInterface {

    @Override
    public String getName() {
        return "overlay";
    }

    @Override
    public LinuxOSFileStore getLinuxOSFileStore() {
        // TODO
        return null;
    }

    @Override
    public AixOSFileStore getAixOSFileStore() {
        // TODO
        return null;
    }

    @Override
    public FreeBsdOSFileStore getFreeBsdOSFileStore() {
        // TODO
        return null;
    }

    @Override
    public MacOSFileStore getMacOSFileStore() {
        // TODO
        return null;
    }

    @Override
    public SolarisOSFileStore getSolarisOSFileStore() {
        // TODO
        return null;
    }

    @Override
    public WindowsOSFileStore getWindowsOSFileStore() {
        // TODO
        return null;
    }

}
