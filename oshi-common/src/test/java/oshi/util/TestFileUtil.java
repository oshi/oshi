/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared test utility for writing files in synthetic sysfs trees.
 */
public final class TestFileUtil {

    private TestFileUtil() {
    }

    /**
     * Writes a string to a file using UTF-8 encoding, creating parent directories as needed.
     *
     * @param path    the file path
     * @param content the content to write
     * @throws IOException if writing fails
     */
    public static void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
