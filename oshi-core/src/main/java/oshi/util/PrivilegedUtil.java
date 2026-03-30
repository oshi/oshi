/*
 * Copyright 2016-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static oshi.util.GlobalConfig.OSHI_SUDOCOMMAND_ALLOWLIST;
import static oshi.util.GlobalConfig.OSHI_SUDOCOMMAND_FILE_ALLOWLIST;
import static oshi.util.GlobalConfig.OSHI_SUDOCOMMAND_PREFIX;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility class for privileged command execution and file reading.
 * Provides methods to execute commands and read files with optional
 * privilege escalation via sudo when running as a non-root user.
 */
@ThreadSafe
public final class PrivilegedUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PrivilegedUtil.class);

    private static volatile Set<String> cachedCommandAllowlist;
    private static volatile Set<String> cachedFileAllowlist;
    private static volatile String lastCommandAllowlistConfig = "";
    private static volatile String lastFileAllowlistConfig = "";

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
        Set<String> allowlist = new HashSet<>();
        for (String entry : allowlistConfig.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                allowlist.add(trimmed);
            }
        }
        return allowlist;
    }

    /**
     * Checks if a command is allowed for privileged execution.
     * Extracts the command name from the full command string and matches
     * against the allowlist. Supports both bare names and full paths.
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
        String cmdName = cmdPath;
        int lastSlash = cmdPath.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < cmdPath.length() - 1) {
            cmdName = cmdPath.substring(lastSlash + 1);
        }

        // Check if command matches any entry in allowlist
        for (String allowed : allowlist) {
            // Direct match with full path or command as specified
            if (allowed.equals(cmdPath) || allowed.equals(cmdName)) {
                return true;
            }
            // If allowlist entry is a path, extract its name and compare
            int allowedLastSlash = allowed.lastIndexOf('/');
            if (allowedLastSlash >= 0 && allowedLastSlash < allowed.length() - 1) {
                String allowedName = allowed.substring(allowedLastSlash + 1);
                if (allowedName.equals(cmdName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a file path is allowed for privileged reading.
     * Supports pattern matching with %d as a PID placeholder.
     *
     * @param filePath  The file path to check
     * @param allowlist Set of allowed file paths or patterns
     * @return true if the file path matches an entry in the allowlist, false otherwise
     */
    public static boolean isFileAllowed(String filePath, Set<String> allowlist) {
        if (filePath == null || filePath.trim().isEmpty() || allowlist == null || allowlist.isEmpty()) {
            return false;
        }

        for (String allowed : allowlist) {
            if (allowed.equals(filePath)) {
                return true;
            }
            // Handle %d placeholder for PID matching
            if (allowed.contains("%d")) {
                // Convert pattern like /proc/%d/io to regex /proc/\d+/io
                String regex = Pattern.quote(allowed).replace("%d", "\\E\\d+\\Q");
                // Clean up empty quote sections
                regex = regex.replace("\\Q\\E", "");
                if (filePath.matches(regex)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the cached command allowlist, parsing from config if needed.
     *
     * @return The current command allowlist
     */
    public static Set<String> getCommandAllowlist() {
        String currentConfig = GlobalConfig.get(OSHI_SUDOCOMMAND_ALLOWLIST, "");
        if (cachedCommandAllowlist == null || !currentConfig.equals(lastCommandAllowlistConfig)) {
            synchronized (PrivilegedUtil.class) {
                if (cachedCommandAllowlist == null || !currentConfig.equals(lastCommandAllowlistConfig)) {
                    cachedCommandAllowlist = parseAllowlist(currentConfig);
                    lastCommandAllowlistConfig = currentConfig;
                }
            }
        }
        return cachedCommandAllowlist;
    }

    /**
     * Gets the cached file allowlist, parsing from config if needed.
     *
     * @return The current file allowlist
     */
    public static Set<String> getFileAllowlist() {
        String currentConfig = GlobalConfig.get(OSHI_SUDOCOMMAND_FILE_ALLOWLIST, "");
        if (cachedFileAllowlist == null || !currentConfig.equals(lastFileAllowlistConfig)) {
            synchronized (PrivilegedUtil.class) {
                if (cachedFileAllowlist == null || !currentConfig.equals(lastFileAllowlistConfig)) {
                    cachedFileAllowlist = parseAllowlist(currentConfig);
                    lastFileAllowlistConfig = currentConfig;
                }
            }
        }
        return cachedFileAllowlist;
    }

    /**
     * Gets the configured sudo command prefix.
     *
     * @return The prefix string, or empty string if not configured
     */
    public static String getPrefix() {
        return GlobalConfig.get(OSHI_SUDOCOMMAND_PREFIX, "");
    }

    /**
     * Reads a file with privileged fallback. First attempts normal read,
     * then falls back to sudo cat if the file exists but is not readable
     * and the file is in the allowlist.
     *
     * @param filePath The file to read
     * @return A list of Strings representing each line of the file, or empty list if unreadable
     */
    public static List<String> readFilePrivileged(String filePath) {
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

        // File exists but not readable - try privileged read
        String prefix = getPrefix();
        if (prefix.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> fileAllowlist = getFileAllowlist();
        if (!isFileAllowed(filePath, fileAllowlist)) {
            LOG.debug("File {} not in privileged allowlist", filePath);
            return Collections.emptyList();
        }

        // Execute sudo cat
        String command = prefix + " cat " + filePath;
        LOG.debug("Attempting privileged file read: {}", command);
        return ExecutingCommand.runNative(command);
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
    public static java.util.Map<String, String> getKeyValueMapFromFilePrivileged(String filePath, String separator) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        List<String> lines = readFilePrivileged(filePath);
        for (String line : lines) {
            String[] parts = line.split(separator);
            if (parts.length == 2) {
                map.put(parts[0], parts[1].trim());
            }
        }
        return map;
    }

    /**
     * Reads all bytes from a file with privileged fallback.
     *
     * @param filePath The file to read
     * @return A byte array representing the file contents, or empty array if unreadable
     */
    public static byte[] readAllBytesPrivileged(String filePath) {
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

        // File exists but not readable - try privileged read
        String prefix = getPrefix();
        if (prefix.isEmpty()) {
            return new byte[0];
        }

        Set<String> fileAllowlist = getFileAllowlist();
        if (!isFileAllowed(filePath, fileAllowlist)) {
            LOG.debug("File {} not in privileged allowlist", filePath);
            return new byte[0];
        }

        // Execute sudo cat and convert output to bytes
        String command = prefix + " cat " + filePath;
        LOG.debug("Attempting privileged file read: {}", command);
        List<String> lines = ExecutingCommand.runNative(command);
        if (lines.isEmpty()) {
            return new byte[0];
        }

        // Join lines with newlines and convert to bytes
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
