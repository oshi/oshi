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

import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;
import oshi.util.FileSystemUtil;
import oshi.util.GlobalConfig;

class AbstractUnixFileSystemTest {

    @Test
    void testParseFSConfigSimple() {
        String platformName = SystemInfo.getCurrentPlatform().name().toLowerCase();
        String configName = "oshi.os." + platformName + ".filesystem.path.excludes";
        GlobalConfig.set(configName, "pattern1,*pattern2,pattern3*,**/pattern4,,");
        List<PathMatcher> patterns = AbstractUnixFileSystem.parseFSConfig(configName);

        assertThat("excluded pattern1", pathExcluded("pattern1", patterns));
        assertThat("excluded pattern2", pathExcluded("pattern2", patterns));
        assertThat("excluded prefix-pattern2", pathExcluded("prefix-pattern2", patterns));
        assertThat("excluded pattern3", pathExcluded("pattern3", patterns));
        assertThat("excluded pattern3-suffix", pathExcluded("pattern3-suffix", patterns));
        assertThat("excluded /pattern4", pathExcluded("/pattern4", patterns));
        assertThat("excluded /var/pattern4", pathExcluded("/var/pattern4", patterns));
    }

    private boolean pathExcluded(String path, List<PathMatcher> pathExclusions) {
        return FileSystemUtil.isFileStoreExcluded(path, "", new ArrayList<>(), pathExclusions, new ArrayList<>(),
                new ArrayList<>());
    }
}
