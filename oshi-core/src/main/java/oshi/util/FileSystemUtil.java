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
package oshi.util;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class FileSystemUtil {

    private static final String GLOB_PREFIX = "glob:";

    private FileSystemUtil() {
    }

    /**
     * Evaluates if file store (identified by {@code path} and {@code volume}) should be excluded or not
     * based on configuration {@code pathIncludes, pathExcludes, volumeIncludes, volumeExcludes}.
     *
     * Inclusion has priority over exclusion. If no exclusion/inclusion pattern is specified, then filestore
     * is not excluded.
     *
     * @param path
     *          Mountpoint of filestore.
     * @param volume
     *          Filestore volume.
     * @param pathIncludes
     *          List of patterns for path inclusions.
     * @param pathExcludes
     *          List of patterns for path exclusions.
     * @param volumeIncludes
     *          List of patterns for volume inclusions.
     * @param volumeExcludes
     *          List of patterns for volume exclusions.
     * @return {@code true} if file store should be excluded or {@code false} otherwise.
     */
    public static boolean isFileStoreExcluded(String path, String volume,
                                              List<PathMatcher> pathIncludes, List<PathMatcher> pathExcludes,
                                              List<PathMatcher> volumeIncludes, List<PathMatcher> volumeExcludes) {
        Path p = Paths.get(path);
        Path v = Paths.get(volume);
        if (matches(p, pathIncludes)) {
            return false;
        }
        if (matches(v, volumeIncludes)) {
            return false;
        }
        if (matches(p, pathExcludes)) {
            return true;
        }
        //noinspection RedundantIfStatement
        if (matches(v, volumeExcludes)) {
            return true;
        }
        return false;
    }

    /**
     * Parse file system include/exclude line.
     *
     * @param config
     *          The config line to be parsed.
     * @return  List of PathMatchers to be used to match filestore volume and path.
     */
    public static List<PathMatcher> parseFileSystemConfig(String config) {
        FileSystem fs = FileSystems.getDefault();
        List<PathMatcher> patterns = new ArrayList<>();
        for (String item : config.split(",")) {
            if (item.length() > 0) {
                // Must add glob: prefix, more details in https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-
                if (!item.startsWith(GLOB_PREFIX)) {
                    item = GLOB_PREFIX + item;
                }
                patterns.add(fs.getPathMatcher(item));
            }
        }
        return patterns;
    }


    /**
     * Checks if {@code text} matches any of @param patterns}.
     *
     * @param text     The text to be matched.
     * @param patterns List of patterns.
     * @return {@code true} if given text matches at least one glob pattern or {@code false} otherwise.
     * @see <a href="https://en.wikipedia.org/wiki/Glob_(programming)">Wikipedia  - glob (programming)</a>
     */
    public static boolean matches(Path text, List<PathMatcher> patterns) {
        for (PathMatcher pattern : patterns) {
            if (pattern.matches(text)) {
                return true;
            }
        }
        return false;
    }

}

