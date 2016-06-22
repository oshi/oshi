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
package oshi.util;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

/**
 * The Class FileUtilTest.
 */
public class FileUtilTest {

    /** The thisclass. */
    private static String THISCLASS = "src/test/java/oshi/util/FileUtilTest.java";

    /**
     * Test read file.
     */
    @Test
    public void testReadFile() {
        List<String> thisFile = null;
        thisFile = FileUtil.readFile(THISCLASS);
        // Comment ONE line
        int lineOne = 0;
        // Comment TWO line
        int lineTwo = 0;
        for (int i = 0; i < thisFile.size(); i++) {
            String line = thisFile.get(i);
            if (lineOne == 0 && line.contains("Comment ONE line")) {
                lineOne = i;
                continue;
            }
            if (lineTwo == 0 && line.contains("Comment TWO line")) {
                lineTwo = i;
                break;
            }
        }
        assertEquals(2, lineTwo - lineOne);
    }
}
