/*
 * MIT License
 *
 * Copyright (c) 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class FileSystemUtilTest {

    /**
     * If no configuration is provided (in oshi.properties) then file store is included by default.
     */
    @Test
    void testIsFileStoreIncludedByDefault() {
        assertThat("file store included by default", !FileSystemUtil.isFileStoreExcluded("/any-path", "/any-volume",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
    }

    /**
     * Test path includes and excludes.
     */
    @Test
    void testIsFileStoreExcludedSimple() {
        List<PathMatcher> pathExcludes = FileSystemUtil.parseFileSystemConfig("excluded-path");
        List<PathMatcher> pathIncludes = FileSystemUtil.parseFileSystemConfig("included-path");
        List<PathMatcher> volumeExcludes = FileSystemUtil.parseFileSystemConfig("excluded-volume");
        List<PathMatcher> volumeIncludes = FileSystemUtil.parseFileSystemConfig("included-volume");

        assertThat("excluded excluded-path", FileSystemUtil.isFileStoreExcluded("excluded-path", "", pathIncludes,
                pathExcludes, volumeIncludes, volumeExcludes));
        assertThat("included included-path", !FileSystemUtil.isFileStoreExcluded("included-path", "", pathIncludes,
                pathExcludes, volumeIncludes, volumeExcludes));

        assertThat("excluded excluded-volume", FileSystemUtil.isFileStoreExcluded("", "excluded-volume", pathIncludes,
                pathExcludes, volumeIncludes, volumeExcludes));
        assertThat("included included-volume", !FileSystemUtil.isFileStoreExcluded("", "included-volume", pathIncludes,
                pathExcludes, volumeIncludes, volumeExcludes));
    }

    /**
     * Test that includes has priority over excludes.
     */
    @Test
    void testIsFileStoreExcludedPriority() {
        List<PathMatcher> pathExcludes = FileSystemUtil.parseFileSystemConfig("path,path-excluded");
        List<PathMatcher> pathIncludes = FileSystemUtil.parseFileSystemConfig("path");
        List<PathMatcher> volumeExcludes = FileSystemUtil.parseFileSystemConfig("volume,volume-excluded");
        List<PathMatcher> volumeIncludes = FileSystemUtil.parseFileSystemConfig("volume");

        assertThat("excluded path-exclude", FileSystemUtil.isFileStoreExcluded("path-excluded", "", pathIncludes,
                pathExcludes, volumeIncludes, volumeExcludes));
        // "path" is both included and excluded and since included has priority, it
        // should be included
        assertThat("included path", !FileSystemUtil.isFileStoreExcluded("path", "", pathIncludes, pathExcludes,
                volumeIncludes, volumeExcludes));

        assertThat("excluded volume-excluded", FileSystemUtil.isFileStoreExcluded("", "volume-excluded", pathIncludes,
                pathExcludes, volumeIncludes, volumeExcludes));
        // "volume" is both included and excluded and since included has priority, it
        // should be included
        assertThat("included volume", !FileSystemUtil.isFileStoreExcluded("", "volume", pathIncludes, pathExcludes,
                volumeIncludes, volumeExcludes));
    }

    @Test
    void testParseFileSystemConfigSimple() {
        List<PathMatcher> matchers = FileSystemUtil.parseFileSystemConfig("simple-path");
        assertThat("simple-path is matched", FileSystemUtil.matches(Paths.get("simple-path"), matchers));
        assertThat("other-path is not matched", !FileSystemUtil.matches(Paths.get("other-path"), matchers));
    }

    @Test
    void testWithGlobPrefix() {
        List<PathMatcher> matchers = FileSystemUtil.parseFileSystemConfig("glob:simple-path");
        assertThat("simple-path is matched", FileSystemUtil.matches(Paths.get("simple-path"), matchers));
        assertThat("other-path is not matched", !FileSystemUtil.matches(Paths.get("other-path"), matchers));
    }

    @Test
    void testWithMoreItems() {
        List<PathMatcher> matchers = FileSystemUtil.parseFileSystemConfig("simple-path1,simple-path2,simple-path3");
        assertThat("simple-path1 is matched", FileSystemUtil.matches(Paths.get("simple-path1"), matchers));
        assertThat("simple-path2 is matched", FileSystemUtil.matches(Paths.get("simple-path2"), matchers));
        assertThat("simple-path3 is matched", FileSystemUtil.matches(Paths.get("simple-path3"), matchers));
        assertThat("other-path is not matched", !FileSystemUtil.matches(Paths.get("other-path"), matchers));
    }

    @Test
    void testWithMultiDirPattern() {
        List<PathMatcher> matchers = FileSystemUtil.parseFileSystemConfig("**/complex-path");
        assertThat("/complex-path is matched", FileSystemUtil.matches(Paths.get("/complex-path"), matchers));
        assertThat("/var/complex-path is matched", FileSystemUtil.matches(Paths.get("/var/complex-path"), matchers));
        assertThat("other-path is not matched", !FileSystemUtil.matches(Paths.get("other-path"), matchers));
    }

    @Test
    void testWithSuffixPattern() {
        List<PathMatcher> matchers = FileSystemUtil.parseFileSystemConfig("suffix-path*");
        assertThat("suffix-path is matched", FileSystemUtil.matches(Paths.get("suffix-path"), matchers));
        assertThat("suffix-path/ is matched", FileSystemUtil.matches(Paths.get("suffix-path/"), matchers));
        assertThat("suffix-path1 is matched", FileSystemUtil.matches(Paths.get("suffix-path1"), matchers));
        assertThat("suffix-path/a is not matched", !FileSystemUtil.matches(Paths.get("suffix-path/a"), matchers));
        assertThat("suffix-path/a/b/c is not matched",
                !FileSystemUtil.matches(Paths.get("suffix-path/a/b/c"), matchers));
        assertThat("123-suffix-path is not matched", !FileSystemUtil.matches(Paths.get("123-suffix-path"), matchers));
    }

    @Test
    void testWithSuffixMultiDirPattern() {
        List<PathMatcher> matchers = FileSystemUtil.parseFileSystemConfig("suffix-path/**");
        assertThat("suffix-path/ is not matched", !FileSystemUtil.matches(Paths.get("suffix-path/"), matchers));
        assertThat("suffix-path/a is matched", FileSystemUtil.matches(Paths.get("suffix-path/a"), matchers));
        assertThat("suffix-path/a/b/c is matched", FileSystemUtil.matches(Paths.get("suffix-path/a/b/c"), matchers));
        assertThat("123-suffix-path is not matched", !FileSystemUtil.matches(Paths.get("123-suffix-path"), matchers));
    }
}
