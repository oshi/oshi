/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import java.util.Arrays;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.GlobalConfig;

/**
 * Common methods for filesystem implementations
 */
@ThreadSafe
public abstract class AbstractFileSystem implements FileSystem {

    public static final String OSHI_NETWORK_FILESYSTEM_TYPES = "oshi.network.filesystem.types";
    public static final String OSHI_PSEUDO_FILESYSTEM_TYPES = "oshi.pseudo.filesystem.types";

    /**
     * FileSystem types which are network-based and should be excluded from
     * local-only lists
     */
    protected static final List<String> NETWORK_FS_TYPES = Arrays
            .asList(GlobalConfig.get(OSHI_NETWORK_FILESYSTEM_TYPES, "").split(","));

    protected static final List<String> PSEUDO_FS_TYPES = Arrays
            .asList(GlobalConfig.get(OSHI_PSEUDO_FILESYSTEM_TYPES, "").split(","));

    @Override
    public List<OSFileStore> getFileStores() {
        return getFileStores(false);
    }
}
