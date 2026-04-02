/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static oshi.util.GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST;
import static oshi.util.GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST;
import static oshi.util.GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility class for privileged command execution and file reading. Provides methods to execute commands and read files
 * with optional privilege escalation via sudo when running as a non-root user.
 */
@ThreadSafe
public final class PrivilegedUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PrivilegedUtil.class);

    private static final Supplier<Set<String>> COMMAND_ALLOWLIST = memoize(PrivilegedUtil::queryCommandAllowlist,
            defaultExpiration());
    private static final Supplier<Set<String>> FILE_ALLOWLIST = memoize(PrivilegedUtil::queryFileAllowlist,
            defaultExpiration());

    private PrivilegedUtil() {
    }

    /**
     * Parses a comma-separated allowlist configuration string into a Set.
     *
     * @param allowlistConfig Comma-separated list of allowed entries
     * @return A Set of trimmed allowlist entries, or empty set if input is null/empty
     */
    public static Set<String> parseAllowlist(String allowlistConfig) {
        if (allowlistConfig == null || allowlistConfig.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(allowlistConfig.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Checks if a command is allowed for privileged execution. Extracts the command name from the full command string
     * and matches against the allowlist. Supports both bare names and full paths.
     *
     * @param command   The full command string (e.g., "dmidecode -t system")
     * @param allowlist Set of allowed command names or paths
     * @return true if the command is in the allowlist, false otherwise
     */
    public static boolean isCommandAllowed(String command, Set<String> allowlist) {
        if (command == null || command.trim().isEmpty() || allowlist == null || allowlist.isEmpty()) {
            return false;
        }

        // Extract the command name (first token)
        String[] tokens = command.trim().split("\\s+");
        if (tokens.length == 0) {
            return false;
        }
        String cmdPath = tokens[0];

        // Extract bare command name from path
        String cmdName = FileUtil.getFileName(cmdPath);
        if (cmdName.isEmpty()) {
            cmdName = cmdPath;
        }

        // Check if command matches any entry in allowlist
        for (String allowed : allowlist) {
            // Direct match with full path or command as specified
            if (allowed.equals(cmdPath) || allowed.equals(cmdName)) {
                return true;
            }
            // If allowlist entry is a path, extract its name and compare
            String allowedName = FileUtil.getFileName(allowed);
            if (!allowedName.isEmpty() && allowedName.equals(cmdName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a file path is allowed for privileged reading. Supports Java glob pattern matching.
     *
     * @param filePath  The file path to check
     * @param allowlist Set of allowed file paths or glob patterns
     * @return true if the file path matches an entry in the allowlist, false otherwise
     */
    public static boolean isFileAllowed(String filePath, Set<String> allowlist) {
        if (filePath == null || filePath.trim().isEmpty() || allowlist == null || allowlist.isEmpty()) {
            return false;
        }
        Path path = Paths.get(filePath);
        for (String allowed : allowlist) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + allowed);
            if (matcher.matches(path)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> queryCommandAllowlist() {
        switch (SystemInfo.getCurrentPlatform()) {
            case LINUX:
                return parseAllowlist(GlobalConfig.get(OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST, ""));
            default:
                return Collections.emptySet();
        }
    }

    private static Set<String> queryFileAllowlist() {
        switch (SystemInfo.getCurrentPlatform()) {
            case LINUX:
                return parseAllowlist(GlobalConfig.get(OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, ""));
            default:
                return Collections.emptySet();
        }
    }

    /**
     * Gets the command allowlist, refreshing periodically from config.
     *
     * @return The current command allowlist
     */
    public static Set<String> getCommandAllowlist() {
        return COMMAND_ALLOWLIST.get();
    }

    /**
     * Gets the file allowlist, refreshing periodically from config.
     *
     * @return The current file allowlist
     */
    public static Set<String> getFileAllowlist() {
        return FILE_ALLOWLIST.get();
    }

    /**
     * Gets the configured sudo command prefix for the current platform.
     *
     * @return The prefix string, or empty string if not configured or not supported on this platform
     */
    public static String getPrefix() {
        switch (SystemInfo.getCurrentPlatform()) {
            case LINUX:
                return GlobalConfig.get(OSHI_OS_LINUX_PRIVILEGED_PREFIX, "");
            default:
                return "";
        }
    }

    /**
     * Reads a file with privileged fallback. First attempts normal read, then falls back to sudo cat if the file exists
     * but is not readable and the file is in the allowlist.
     *
     * @param filePath The file to read
     * @return A list of Strings representing each line of the file, or empty list if unreadable
     */
    public static List<String> readFilePrivileged(String filePath) {
        // If already elevated, delegate directly to FileUtil
        if (UserGroupInfo.isElevated()) {
            return FileUtil.readFile(filePath, false);
        }

        // Check if file is in allowlist
        Set<String> fileAllowlist = getFileAllowlist();
        if (!isFileAllowed(filePath, fileAllowlist)) {
            return FileUtil.readFile(filePath, false);
        }

        // First attempt normal read (silent failure)
        List<String> result = FileUtil.readFile(filePath, false);
        if (!result.isEmpty()) {
            return result;
        }

        // Check if file exists but is unreadable
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }

        // Build cat command, with prefix if configured
        List<String> cmdList = new ArrayList<>();
        String prefix = getPrefix();
        if (!prefix.isEmpty()) {
            cmdList.addAll(Arrays.asList(prefix.split("\\s+")));
        }
        cmdList.add("cat");
        cmdList.add(filePath);
        String[] cmdArray = cmdList.toArray(new String[0]);
        LOG.debug("Attempting privileged file read: {}", Arrays.toString(cmdArray));
        return ExecutingCommand.runNative(cmdArray);
    }

    /**
     * Reads a file and returns the first line as a String, with privileged fallback.
     *
     * @param filePath The file to read
     * @return The first line of the file, or empty string if unreadable
     */
    public static String getStringFromFilePrivileged(String filePath) {
        List<String> lines = readFilePrivileged(filePath);
        if (!lines.isEmpty()) {
            return lines.get(0);
        }
        return "";
    }

    /**
     * Reads a file and returns a map of string keys to string values, with privileged fallback.
     *
     * @param filePath  The file to read
     * @param separator Character(s) in each line of the file that separate the key and the value
     * @return The map contained in the file, delimited by the separator, with the value whitespace trimmed
     */
    public static Map<String, String> getKeyValueMapFromFilePrivileged(String filePath, String separator) {
        return ParseUtil.parseStringListToMap(readFilePrivileged(filePath), separator);
    }

    /**
     * Reads all bytes from a file with privileged fallback.
     *
     * @param filePath    The file to read
     * @param reportError Whether to log errors reading the file
     * @return A byte array representing the file contents, or empty array if unreadable
     */
    public static byte[] readAllBytesPrivileged(String filePath, boolean reportError) {
        // If already elevated, delegate directly to FileUtil
        if (UserGroupInfo.isElevated()) {
            return FileUtil.readAllBytes(filePath, reportError);
        }

        // Check if file is in allowlist
        Set<String> fileAllowlist = getFileAllowlist();
        if (!isFileAllowed(filePath, fileAllowlist)) {
            return FileUtil.readAllBytes(filePath, reportError);
        }

        // First attempt normal read
        byte[] result = FileUtil.readAllBytes(filePath, false);
        if (result.length > 0) {
            return result;
        }

        // Check if file exists but is unreadable
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return new byte[0];
        }

        // Build cat command, with prefix if configured
        List<String> cmdList = new ArrayList<>();
        String prefix = getPrefix();
        if (!prefix.isEmpty()) {
            cmdList.addAll(Arrays.asList(prefix.split("\\s+")));
        }
        cmdList.add("cat");
        cmdList.add(filePath);
        String[] cmdArray = cmdList.toArray(new String[0]);
        LOG.debug("Attempting privileged file read: {}", Arrays.toString(cmdArray));

        // Spawn process and read raw bytes to preserve binary content
        return runCatAndReadBytes(cmdArray);
    }

    /**
     * Spawns a process to run a command and reads the raw byte output.
     *
     * @param cmdArray The command to execute
     * @return The raw bytes from the process stdout, or empty array on failure
     */
    private static byte[] runCatAndReadBytes(String[] cmdArray) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmdArray);
            try (java.io.InputStream is = p.getInputStream();
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            }
        } catch (java.io.IOException e) {
            LOG.debug("Failed to execute privileged cat command: {}", e.getMessage());
            return new byte[0];
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }
}
