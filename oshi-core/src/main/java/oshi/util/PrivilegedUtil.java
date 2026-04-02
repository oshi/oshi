/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oshi.util.GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST;
import static oshi.util.GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST;
import static oshi.util.GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.io.IOException;
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

    private static volatile Supplier<Set<String>> commandAllowlist = memoize(PrivilegedUtil::queryCommandAllowlist,
            defaultExpiration());
    private static volatile Supplier<Set<String>> fileAllowlist = memoize(PrivilegedUtil::queryFileAllowlist,
            defaultExpiration());
    private static volatile Supplier<String> prefix = memoize(PrivilegedUtil::queryPrefix, defaultExpiration());

    private PrivilegedUtil() {
    }

    /**
     * Parses a comma-separated allowlist configuration string into a Set.
     *
     * @param allowlistConfig Comma-separated list of allowed entries
     * @return A Set of trimmed allowlist entries, or empty set if input is null/empty
     */
    static Set<String> parseAllowlist(String allowlistConfig) {
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
            return false;
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

    private static String queryPrefix() {
        switch (SystemInfo.getCurrentPlatform()) {
            case LINUX:
                return GlobalConfig.get(OSHI_OS_LINUX_PRIVILEGED_PREFIX, "");
            default:
                return "";
        }
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
        return commandAllowlist.get();
    }

    /**
     * Gets the file allowlist, refreshing periodically from config.
     *
     * @return The current file allowlist
     */
    public static Set<String> getFileAllowlist() {
        return fileAllowlist.get();
    }

    /**
     * Gets the configured sudo command prefix for the current platform.
     *
     * @return The prefix string, or empty string if not configured or not supported on this platform
     */
    public static String getPrefix() {
        return prefix.get();
    }

    /**
     * Reinitializes the memoized suppliers. Only intended for use in tests to ensure config changes are picked up
     * immediately after {@link GlobalConfig#clear()}.
     */
    static void clearCaches() {
        commandAllowlist = memoize(PrivilegedUtil::queryCommandAllowlist, defaultExpiration());
        fileAllowlist = memoize(PrivilegedUtil::queryFileAllowlist, defaultExpiration());
        prefix = memoize(PrivilegedUtil::queryPrefix, defaultExpiration());
    }

    /**
     * Reads a file with privileged fallback. First attempts normal read, then falls back to sudo cat if the file exists
     * but is not readable and the file is in the allowlist.
     *
     * @param filePath The file to read
     * @return A list of Strings representing each line of the file, or empty list if unreadable
     */
    public static List<String> readFilePrivileged(String filePath) {
        // If already elevated or file is not in the allowlist, delegate directly to FileUtil
        if (UserGroupInfo.isElevated() || !isFileAllowed(filePath, getFileAllowlist())) {
            return FileUtil.readFile(filePath, false);
        }

        // Not elevated and file is in the allowlist: attempt privileged read via prefix + cat
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }
        // If directly readable or no prefix configured, no need for privilege escalation
        if (Files.isReadable(path) || getPrefix().isEmpty()) {
            return FileUtil.readFile(filePath, false);
        }

        return ExecutingCommand.runNative(buildCatCommand(filePath));
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
        // If already elevated or file is not in the allowlist, delegate directly to FileUtil
        if (UserGroupInfo.isElevated() || !isFileAllowed(filePath, getFileAllowlist())) {
            return FileUtil.readAllBytes(filePath, reportError);
        }

        // Not elevated and file is in the allowlist: attempt privileged read via prefix + cat
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return new byte[0];
        }
        // If directly readable or no prefix configured, no need for privilege escalation
        if (Files.isReadable(path) || getPrefix().isEmpty()) {
            return FileUtil.readAllBytes(filePath, reportError);
        }

        // Spawn process and read raw bytes to preserve binary content
        String[] cmdArray = buildCatCommand(filePath);
        try {
            Process p = Runtime.getRuntime().exec(cmdArray);
            try {
                byte[] stdout = FileUtil.readAllBytes(p.getInputStream());
                byte[] stderr = FileUtil.readAllBytes(p.getErrorStream());
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    return stdout;
                }
                if (reportError) {
                    LOG.error("Privileged cat exited with code {}: {}", exitCode, new String(stderr, UTF_8).trim());
                } else {
                    LOG.debug("Privileged cat exited with code {}: {}", exitCode, new String(stderr, UTF_8).trim());
                }
            } finally {
                ExecutingCommand.destroyProcess(p);
            }
        } catch (SecurityException | IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.debug("Failed to execute privileged cat command: {}", e.getMessage());
        }
        return new byte[0];
    }

    private static String[] buildCatCommand(String filePath) {
        List<String> cmdList = new ArrayList<>();
        String prefix = getPrefix();
        if (!prefix.isEmpty()) {
            cmdList.addAll(Arrays.asList(prefix.split("\\s+")));
        }
        cmdList.add("cat");
        cmdList.add(filePath);
        String[] cmdArray = cmdList.toArray(new String[0]);
        LOG.debug("Attempting privileged file read: {}", Arrays.toString(cmdArray));
        return cmdArray;
    }
}
