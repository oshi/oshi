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
        assertTrue("File system open file descriptors should be 0 or higher",
                filesystem.getOpenFileDescriptors() >= 0L);
        assertTrue("File system max open file descriptors should be 0 or higher",
                filesystem.getMaxFileDescriptors() >= 0L);
        for (OSFileStore store : filesystem.getFileStores()) {
            assertNotNull("File store name shouldn't be null", store.getName());
            assertNotNull("File store volume shouldn't be null", store.getVolume());
            assertNotNull("File store label shouldn't be null", store.getLabel());
            assertNotNull("File store logical volume shouldn't be null", store.getLogicalVolume());
            assertNotNull("File store description shouldn't be null", store.getDescription());
            assertNotNull("File store type shouldn't be null", store.getType());
            assertFalse("File store options shouldn't be empty", store.getOptions().isEmpty());
            assertNotNull("File store mount shouldn't be null", store.getMount());
            assertNotNull("File store UUID shouldn't be null", store.getUUID());
            assertTrue("File store total space should be 0 or higher", store.getTotalSpace() >= 0);
            assertTrue("File store usable space should be 0 or higher", store.getUsableSpace() >= 0);
            assertTrue("File store free space should be 0 or higher", store.getFreeSpace() >= 0);
            if (SystemInfo.getCurrentPlatformEnum() != PlatformEnum.WINDOWS) {
                assertTrue("Number of free inodes should be 0 or higher on non-Windows systems",
                        store.getFreeInodes() >= 0);
                assertTrue(
                        "Total number of inodes should be greater than or equal to number of free inodes on non-Windows systems",
                        store.getTotalInodes() >= store.getFreeInodes());
            }
            if (!store.getDescription().equals("Network drive")) {
                assertTrue(
                        "File store's usable space should be less than or equal to the total space on non-network drives",
                        store.getUsableSpace() <= store.getTotalSpace());
            }
        }
    }
}
