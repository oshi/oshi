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

import java.nio.file.PathMatcher;
import java.util.List;

import oshi.SystemInfo;
import oshi.util.FileSystemUtil;
import oshi.util.GlobalConfig;

public abstract class AbstractUnixFileSystem extends AbstractFileSystem {

    private static final String CONFIG_NAME_PREFIX = "oshi.os." + SystemInfo.getCurrentPlatform().name().toLowerCase()
            + ".filesystem.";
    private static final List<PathMatcher> FS_PATH_INCLUDES = parseFSConfig(CONFIG_NAME_PREFIX + "path.includes");
    private static final List<PathMatcher> FS_PATH_EXCLUDES = parseFSConfig(CONFIG_NAME_PREFIX + "path.excludes");
    private static final List<PathMatcher> FS_VOLUME_INCLUDES = parseFSConfig(CONFIG_NAME_PREFIX + "volume.includes");
    private static final List<PathMatcher> FS_VOLUME_EXCLUDES = parseFSConfig(CONFIG_NAME_PREFIX + "volume.excludes");

    protected static boolean isFileStoreExcluded(String path, String volume) {
        return FileSystemUtil.isFileStoreExcluded(path, volume, FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES,
                FS_VOLUME_EXCLUDES);
    }

    // Package private for testing
    static List<PathMatcher> parseFSConfig(String configPropertyName) {
        String config = GlobalConfig.get(configPropertyName, "");
        // DEBUG
        System.out.println("Initializing " + configPropertyName + " with config: " + config);
        return FileSystemUtil.parseFileSystemConfig(config);
    }
}
