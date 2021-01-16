/**
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class FileSystemUtilTest {

    /**
     * If no configuration is provided (in oshi.properties) then file store is
     * included by default.
     */
    @Test
    void testIsFileStoreIncludedByDefault() {
        assertThat("file store included by default", !isFileStoreExcluded("/any-path", "/any-volume", "", "", "", ""));
    }

    /**
     * Test path includes and excludes.
     */
    @Test
    void testIsFileStoreIncludedPathIncludeExclude() {
        assertThat("file store included",
                !isFileStoreExcluded("/some-path", "", "some-path,**/included-dir", "**/*excluded-dir", "", ""));
        assertThat("file store included", !isFileStoreExcluded("/a/b/c/included-dir", "", "some-path,**/included-dir",
                "**/*excluded-dir", "", ""));
        assertThat("file store excluded",
                isFileStoreExcluded("/excluded-dir", "", "some-path,**/included-dir", "**/*excluded-dir", "", ""));
        assertThat("file store excluded", isFileStoreExcluded("/x/y/z/123-excluded-dir", "",
                "some-path,**/included-dir", "**/*excluded-dir", "", ""));
        // if neither includes nor excludes matches then file store is included by
        // default
        assertThat("file store included",
                !isFileStoreExcluded("/other-dir", "", "some-path,**/included-dir", "**/*excluded-dir", "", ""));
    }

    /**
     * Test that includes has priority over excludes.
     */
    @Test
    void testIsFileStoreIncludedPathPriority() {
        assertThat("file store included by default", !isFileStoreExcluded("/any-path", "", "*", "*", "", ""));
    }

    /**
     * Test path includes and excludes.
     */
    @Test
    void testIsFileStoreIncludedVolumeIncludeExclude() {
        assertThat("file store included", !isFileStoreExcluded("", "/any-volume", "", "",
                "some-volume,**/included-volume", "**/*excluded-volume"));
        assertThat("file store included", !isFileStoreExcluded("", "/a/b/c/included-volume", "", "",
                "some-volume,**/included-volume", "**/*excluded-volume"));
        assertThat("file store excluded", isFileStoreExcluded("", "/x/y/z/123-excluded-volume", "", "",
                "some-volume,**/included-volume", "**/*excluded-volume"));
        // if neither includes nor excludes matches then file store is included by
        // default
        assertThat("file store included", !isFileStoreExcluded("", "/other-volume", "", "",
                "some-volume,**/included-volume", "**/*excluded-volume"));
    }

    /**
     * Test that includes has priority over excludes.
     */
    @Test
    void testIsFileStoreIncludedVolumePriority() {
        assertThat("file store included by default", !isFileStoreExcluded("", "/any-volume", "", "", "*", "*"));
    }

    @Test
    void testParseFileSystemConfigSimple() {
        List<PathMatcher> matchers = FileSystemUtil.parseFileSystemConfig("simple-path");
        assertThat("simple-path is matched", FileSystemUtil.matches(Paths.get("simple-path"), matchers));
        assertThat("other-path is not matched", !FileSystemUtil.matches(Paths.get("other-path"), matchers));
    }

    @Test
    void testParseFileSystemConfigWithGlobPrefix() {
        List<PathMatcher> matchers = FileSystemUtil.parseFileSystemConfig("glob:simple-path");
        assertThat("simple-path is matched", FileSystemUtil.matches(Paths.get("simple-path"), matchers));
        assertThat("other-path is not matched", !FileSystemUtil.matches(Paths.get("other-path"), matchers));
    }

    @Test
    void testParseFileSystemConfigWithMoreItems() {
        List<PathMatcher> matchers = FileSystemUtil.parseFileSystemConfig("simple-path1,simple-path2,simple-path3");
        assertThat("simple-path1 is matched", FileSystemUtil.matches(Paths.get("simple-path1"), matchers));
        assertThat("simple-path2 is matched", FileSystemUtil.matches(Paths.get("simple-path2"), matchers));
        assertThat("simple-path3 is matched", FileSystemUtil.matches(Paths.get("simple-path3"), matchers));
        assertThat("other-path is not matched", !FileSystemUtil.matches(Paths.get("other-path"), matchers));
    }

    @Test
    void testParseFileSystemConfigWithMultiDirPattern() {
        List<PathMatcher> matchers = FileSystemUtil.parseFileSystemConfig("**/complex-path");
        assertThat("/complex-path is matched", FileSystemUtil.matches(Paths.get("/complex-path"), matchers));
        assertThat("/var/complex-path is matched", FileSystemUtil.matches(Paths.get("/var/complex-path"), matchers));
        assertThat("other-path is not matched", !FileSystemUtil.matches(Paths.get("other-path"), matchers));
    }

    @Test
    void testParseFileSystemConfigWithSuffixPattern() {
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
    void testParseFileSystemConfigWithSuffixMultiDirPattern() {
        List<PathMatcher> matchers = FileSystemUtil.parseFileSystemConfig("suffix-path/**");
        assertThat("suffix-path/ is not matched", !FileSystemUtil.matches(Paths.get("suffix-path/"), matchers));
        assertThat("suffix-path/a is matched", FileSystemUtil.matches(Paths.get("suffix-path/a"), matchers));
        assertThat("suffix-path/a/b/c is matched", FileSystemUtil.matches(Paths.get("suffix-path/a/b/c"), matchers));
        assertThat("123-suffix-path is not matched", !FileSystemUtil.matches(Paths.get("123-suffix-path"), matchers));
    }

    private boolean isFileStoreExcluded(String path, String volume, String pathIncludes, String pathExcludes,
            String volumeIncludes, String volumeExcludes) {
        return FileSystemUtil.isFileStoreExcluded(path, volume, patternsToMatchers(pathIncludes),
                patternsToMatchers(pathExcludes), patternsToMatchers(volumeIncludes),
                patternsToMatchers(volumeExcludes));

    }

    static List<PathMatcher> patternsToMatchers(String globPatterns) {
        FileSystem fs = FileSystems.getDefault();
        List<PathMatcher> patterns = new ArrayList<>();
        for (String item : globPatterns.split(",")) {
            if (item.length() > 0) {
                patterns.add(fs.getPathMatcher("glob:" + item));
            }
        }
        return patterns;
    }
}