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
package oshi.software.os;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import oshi.PlatformEnum;
import oshi.SystemInfo;

/**
 * Test File System
 */
public class FileSystemTest {

    /**
     * Test file system.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void testFileSystem() throws IOException {
        SystemInfo si = new SystemInfo();
        FileSystem filesystem = si.getOperatingSystem().getFileSystem();
        assertTrue(filesystem.getOpenFileDescriptors() >= 0L);
        assertTrue(filesystem.getMaxFileDescriptors() >= 0L);
        for (OSFileStore store : filesystem.getFileStores()) {
            assertNotNull(store.getName());
            assertNotNull(store.getVolume());
            assertNotNull(store.getLabel());
            assertNotNull(store.getLogicalVolume());
            assertNotNull(store.getDescription());
            assertNotNull(store.getType());
            assertFalse(store.getOptions().isEmpty());
            assertNotNull(store.getMount());
            assertNotNull(store.getUUID());
            assertTrue(store.getTotalSpace() >= 0);
            assertTrue(store.getUsableSpace() >= 0);
            assertTrue(store.getFreeSpace() >= 0);
            if (SystemInfo.getCurrentPlatformEnum() != PlatformEnum.WINDOWS) {
                assertTrue(store.getFreeInodes() >= 0);
                assertTrue(store.getTotalInodes() >= store.getFreeInodes());
            }
            if (!store.getDescription().equals("Network drive")) {
                assertTrue(store.getUsableSpace() <= store.getTotalSpace());
            }
        }
    }
}
