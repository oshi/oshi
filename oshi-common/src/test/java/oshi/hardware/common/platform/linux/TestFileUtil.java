/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared test utility for writing files in synthetic sysfs trees.
 */
final class TestFileUtil {

    private TestFileUtil() {
    }

    /**
     * Writes a string to a file, creating parent directories as needed.
     *
     * @param path    the file path
     * @param content the content to write
     * @throws IOException if writing fails
     */
    static void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
