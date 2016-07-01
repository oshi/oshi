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
package oshi.software.os;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

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
        OSFileStore[] fs = filesystem.getFileStores();
        for (int f = 0; f < fs.length; f++) {
            assertNotNull(fs[f].getName());
            assertNotNull(fs[f].getVolume());
            assertNotNull(fs[f].getDescription());
            assertNotNull(fs[f].getType());
            assertNotNull(fs[f].getMount());
            assertNotNull(fs[f].getUUID());
            assertTrue(fs[f].getTotalSpace() >= 0);
            assertTrue(fs[f].getUsableSpace() <= fs[f].getTotalSpace());

            fs[f].setName("name");
            fs[f].setVolume("volume");
            fs[f].setDescription("desc");
            fs[f].setType("type");
            fs[f].setMount("mount");
            fs[f].setUUID("uuid");
            fs[f].setTotalSpace(12345L);
            fs[f].setUsableSpace(1234L);

            assertEquals("name", fs[f].getName());
            assertEquals("volume", fs[f].getVolume());
            assertEquals("desc", fs[f].getDescription());
            assertEquals("type", fs[f].getType());
            assertEquals("mount", fs[f].getMount());
            assertEquals("uuid", fs[f].getUUID());
            assertEquals(12345L, fs[f].getTotalSpace());
            assertEquals(1234L, fs[f].getUsableSpace());
        }
    }
}
