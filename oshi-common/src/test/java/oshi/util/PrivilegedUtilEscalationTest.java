/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;

/**
 * Tests the privileged escalation paths in PrivilegedUtil that require a configured prefix and a non-readable file.
 */
@Execution(SAME_THREAD)
@EnabledOnOs(OS.LINUX)
class PrivilegedUtilEscalationTest {

    @BeforeEach
    void setUp() {
        GlobalConfig.clear();
        PrivilegedUtil.clearCaches();
    }

    @AfterEach
    void tearDown() {
        GlobalConfig.clear();
        PrivilegedUtil.clearCaches();
    }

    @Test
    void testReadFilePrivilegedWithAllowlistAndPrefix(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test-data.txt");
        Files.write(testFile, "line1\nline2\n".getBytes(StandardCharsets.UTF_8));

        String filePath = testFile.toString();
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, filePath);
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "");
        PrivilegedUtil.clearCaches();

        List<String> result = PrivilegedUtil.readFilePrivileged(filePath);
        assertThat("Should read file content", result, is(not(empty())));
        assertThat(result.get(0), is("line1"));
    }

    @Test
    void testReadFilePrivilegedNonExistentWithAllowlist(@TempDir Path tempDir) {
        String filePath = tempDir.resolve("nonexistent.txt").toString();
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, filePath);
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "sudo -n");
        PrivilegedUtil.clearCaches();

        List<String> result = PrivilegedUtil.readFilePrivileged(filePath);
        assertThat("Non-existent file returns empty", result, is(empty()));
    }

    @Test
    void testReadFilePrivilegedWithPrefixAndReadableFile(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("readable.txt");
        Files.write(testFile, "content\n".getBytes(StandardCharsets.UTF_8));

        String filePath = testFile.toString();
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, filePath);
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "sudo -n");
        PrivilegedUtil.clearCaches();

        List<String> result = PrivilegedUtil.readFilePrivileged(filePath);
        assertThat(result, is(not(empty())));
        assertThat(result.get(0), is("content"));
    }

    @Test
    void testReadFilePrivilegedEscalationPath(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("restricted.txt");
        Files.write(testFile, "secret\n".getBytes(StandardCharsets.UTF_8));
        assumeTrue(testFile.toFile().setReadable(false) && !Files.isReadable(testFile),
                "Could not make file unreadable (possibly running as root)");

        try {
            String filePath = testFile.toString();
            GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, filePath);
            GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "/bin/true &&");
            PrivilegedUtil.clearCaches();

            // Exercises the buildCatCommand + runNative path
            List<String> result = PrivilegedUtil.readFilePrivileged(filePath);
            assertThat(result != null, is(true));
        } finally {
            testFile.toFile().setReadable(true);
        }
    }

    @Test
    void testReadAllBytesPrivilegedNonExistentWithAllowlist(@TempDir Path tempDir) {
        String filePath = tempDir.resolve("nonexistent.bin").toString();
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, filePath);
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "sudo -n");
        PrivilegedUtil.clearCaches();

        byte[] result = PrivilegedUtil.readAllBytesPrivileged(filePath, false);
        assertThat("Non-existent file returns empty array", result.length, is(0));
    }

    @Test
    void testReadAllBytesPrivilegedReadableWithAllowlist(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("readable.bin");
        Files.write(testFile, new byte[] { 1, 2, 3, 4 });

        String filePath = testFile.toString();
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, filePath);
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "sudo -n");
        PrivilegedUtil.clearCaches();

        byte[] result = PrivilegedUtil.readAllBytesPrivileged(filePath, false);
        assertThat(result.length, is(4));
    }

    @Test
    void testReadAllBytesPrivilegedEscalationPath(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("restricted.bin");
        Files.write(testFile, new byte[] { 10, 20, 30 });
        assumeTrue(testFile.toFile().setReadable(false) && !Files.isReadable(testFile),
                "Could not make file unreadable (possibly running as root)");

        try {
            String filePath = testFile.toString();
            GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, filePath);
            GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "/bin/true &&");
            PrivilegedUtil.clearCaches();

            byte[] result = PrivilegedUtil.readAllBytesPrivileged(filePath, true);
            assertThat(result != null, is(true));
        } finally {
            testFile.toFile().setReadable(true);
        }
    }

    @Test
    void testReadAllBytesPrivilegedEscalationWithReportError(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("restricted2.bin");
        Files.write(testFile, new byte[] { 10, 20, 30 });
        assumeTrue(testFile.toFile().setReadable(false) && !Files.isReadable(testFile),
                "Could not make file unreadable (possibly running as root)");

        try {
            String filePath = testFile.toString();
            GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, filePath);
            GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "/bin/false");
            PrivilegedUtil.clearCaches();

            byte[] result = PrivilegedUtil.readAllBytesPrivileged(filePath, true);
            assertThat(result.length, is(0));

            byte[] result2 = PrivilegedUtil.readAllBytesPrivileged(filePath, false);
            assertThat(result2.length, is(0));
        } finally {
            testFile.toFile().setReadable(true);
        }
    }

    @Test
    void testGetKeyValueMapFromFilePrivileged(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("kvmap.txt");
        Files.write(testFile, "key1=value1\nkey2=value2\n".getBytes(StandardCharsets.UTF_8));

        String filePath = testFile.toString();
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, filePath);
        PrivilegedUtil.clearCaches();

        Map<String, String> result = PrivilegedUtil.getKeyValueMapFromFilePrivileged(filePath, "=");
        assertThat(result, aMapWithSize(2));
        assertThat(result, hasEntry("key1", "value1"));
        assertThat(result, hasEntry("key2", "value2"));
    }

    @Test
    void testGetStringFromFilePrivilegedWithAllowlist(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("single-line.txt");
        Files.write(testFile, "hello world\n".getBytes(StandardCharsets.UTF_8));

        String filePath = testFile.toString();
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, filePath);
        PrivilegedUtil.clearCaches();

        String result = PrivilegedUtil.getStringFromFilePrivileged(filePath);
        assertThat(result, is("hello world"));
    }

    @Test
    void testGetCommandAllowlistWithConfig() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST, "dmidecode,lshw");
        PrivilegedUtil.clearCaches();

        assertThat(PrivilegedUtil.getCommandAllowlist().size(), is(2));
    }

    @Test
    void testGetFileAllowlistWithConfig() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, "/proc/*/io,/sys/class/dmi/**");
        PrivilegedUtil.clearCaches();

        assertThat(PrivilegedUtil.getFileAllowlist().size(), is(2));
    }

    @Test
    void testIsFileAllowedWithDoubleStarGlob() {
        Set<String> allowlist = new HashSet<>();
        allowlist.add("/sys/class/dmi/**");

        assertThat(PrivilegedUtil.isFileAllowed("/sys/class/dmi/id/product_serial", allowlist), is(true));
        assertThat(PrivilegedUtil.isFileAllowed("/sys/class/dmi/id/product_uuid", allowlist), is(true));
        assertThat(PrivilegedUtil.isFileAllowed("/sys/class/other/file", allowlist), is(false));
    }

    @Test
    void testReadFilePrivilegedNotInAllowlist(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("not-allowed.txt");
        Files.write(testFile, "data\n".getBytes(StandardCharsets.UTF_8));

        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, "/some/other/path");
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "sudo -n");
        PrivilegedUtil.clearCaches();

        List<String> result = PrivilegedUtil.readFilePrivileged(testFile.toString());
        assertThat(result, is(not(empty())));
        assertThat(result.get(0), is("data"));
    }
}
