/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * A class for executing on the command line and returning the result of execution.
 * <p>
 * A command named without a path that cannot be started from the {@code PATH} is looked for in the {@code sbin}
 * directories, where many of the system tools OSHI runs live. A non-login shell, a cron job or a service manager may
 * leave those directories off the {@code PATH} that OSHI inherits; NetBSD's {@code sshd}, for one, gives a non-login
 * session only {@code /usr/bin:/bin:/usr/pkg/bin:/usr/local/bin}, without the {@code /sbin/sysctl} that most of its
 * values come from.
 */
@ThreadSafe
public final class ExecutingCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutingCommand.class);

    private static final String[] DEFAULT_ENV = getDefaultEnv();

    /**
     * Where to look, in order, for a command that cannot be started from the {@code PATH}.
     */
    private static final List<String> SBIN_DIRS = Collections
            .unmodifiableList(Arrays.asList("/sbin", "/usr/sbin", "/usr/local/sbin", "/usr/pkg/sbin"));

    /**
     * Commands found in {@link #SBIN_DIRS} after failing to start by name, keyed by that name, so that a later run
     * starts them by path without failing first.
     */
    private static final Map<String, String> SBIN_PATHS = new ConcurrentHashMap<>();

    private ExecutingCommand() {
    }

    private static String[] getDefaultEnv() {
        if (PlatformEnum.getCurrentPlatform() == PlatformEnum.WINDOWS) {
            return new String[] { "LANGUAGE=C" };
        } else {
            return new String[] { "LC_ALL=C" };
        }
    }

    /**
     * Executes a command on the native command line and returns the result. This is a convenience method to call
     * {@link java.lang.Runtime#exec(String)} and capture the resulting output in a list of Strings. On Windows,
     * built-in commands not associated with an executable program may require {@code cmd.exe /c} to be prepended to the
     * command.
     *
     * @param cmdToRun Command to run
     * @return A list of Strings representing the result of the command, or empty string if the command failed
     */
    public static List<String> runNative(String cmdToRun) {
        String[] cmd = cmdToRun.split(" ");
        return runNative(cmd);
    }

    /**
     * Executes a command on the native command line and returns the result line by line. This is a convenience method
     * to call {@link java.lang.Runtime#exec(String[])} and capture the resulting output in a list of Strings. On
     * Windows, built-in commands not associated with an executable program may require the strings {@code cmd.exe} and
     * {@code /c} to be prepended to the array.
     *
     * @param cmdToRunWithArgs Command to run and args, in an array
     * @return A list of Strings representing the result of the command, or empty string if the command failed
     */
    public static List<String> runNative(String[] cmdToRunWithArgs) {
        return runNative(cmdToRunWithArgs, DEFAULT_ENV);
    }

    /**
     * Executes a command on the native command line and returns the result line by line. This is a convenience method
     * to call {@link java.lang.Runtime#exec(String[])} and capture the resulting output in a list of Strings. On
     * Windows, built-in commands not associated with an executable program may require the strings {@code cmd.exe} and
     * {@code /c} to be prepended to the array.
     *
     * @param cmdToRunWithArgs Command to run and args, in an array
     * @param envp             array of strings, each element of which has environment variable settings in the format
     *                         name=value, or null if the subprocess should inherit the environment of the current
     *                         process.
     * @return A list of Strings representing the result of the command, or empty string if the command failed
     */
    public static List<String> runNative(String[] cmdToRunWithArgs, String @Nullable [] envp) {
        Process p = null;
        try {
            p = exec(cmdToRunWithArgs, envp);
            return getProcessOutput(p, cmdToRunWithArgs);
        } catch (SecurityException | IOException e) {
            LOG.trace("Couldn't run command {}", Arrays.toString(cmdToRunWithArgs), e);
        } finally {
            // Ensure all resources are released
            if (p != null) {
                destroyProcess(p);
            }
        }
        return Collections.emptyList();
    }

    private static Process exec(String[] cmd, String @Nullable [] envp) throws IOException {
        if (cmd.length == 0) {
            return Runtime.getRuntime().exec(cmd, envp);
        }
        String known = SBIN_PATHS.get(cmd[0]);
        if (known != null) {
            return Runtime.getRuntime().exec(withExecutable(cmd, known), envp);
        }
        try {
            return Runtime.getRuntime().exec(cmd, envp);
        } catch (IOException e) {
            String found = PlatformEnum.getCurrentPlatform() == PlatformEnum.WINDOWS ? null
                    : findExecutable(SBIN_DIRS, cmd[0]);
            if (found == null) {
                throw e;
            }
            SBIN_PATHS.put(cmd[0], found);
            return Runtime.getRuntime().exec(withExecutable(cmd, found), envp);
        }
    }

    /**
     * Finds an executable file of the given name in the given directories.
     *
     * @param dirs the directories to search, in order
     * @param name the command name, which must not contain a path separator
     * @return the path of the first executable file found, or {@code null} if the name contains a separator or no
     *         directory holds one
     */
    static @Nullable String findExecutable(List<String> dirs, String name) {
        if (name.isEmpty() || name.indexOf('/') >= 0) {
            return null;
        }
        for (String dir : dirs) {
            File file = new File(dir, name);
            if (file.isFile() && file.canExecute()) {
                return file.getPath();
            }
        }
        return null;
    }

    private static String[] withExecutable(String[] cmd, String path) {
        String[] resolved = cmd.clone();
        resolved[0] = path;
        return resolved;
    }

    private static List<String> getProcessOutput(Process p, String[] cmd) {
        ArrayList<String> sa = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sa.add(line);
            }
            p.waitFor();
        } catch (IOException e) {
            LOG.trace("Problem reading output from {}", Arrays.toString(cmd), e);
        } catch (InterruptedException ie) {
            LOG.trace("Interrupted while reading output from {}", Arrays.toString(cmd), ie);
            Thread.currentThread().interrupt();
        }
        return sa;
    }

    static void destroyProcess(Process p) {
        // Windows and Solaris don't close descriptors on destroy,
        // so we must handle separately
        if (PlatformEnum.getCurrentPlatform() == PlatformEnum.WINDOWS
                || PlatformEnum.getCurrentPlatform() == PlatformEnum.SOLARIS) {
            ExceptionUtil.runSilently(() -> p.getOutputStream().close());
            ExceptionUtil.runSilently(() -> p.getInputStream().close());
            ExceptionUtil.runSilently(() -> p.getErrorStream().close());
        }
        p.destroy();
    }

    /**
     * Return first line of response for selected command.
     *
     * @param cmd2launch String command to be launched
     * @return String or empty string if command failed
     */
    public static String getFirstAnswer(String cmd2launch) {
        return getAnswerAt(cmd2launch, 0);
    }

    /**
     * Return response on selected line index (0-based) after running selected command.
     *
     * @param cmd2launch String command to be launched
     * @param answerIdx  int index of line in response of the command
     * @return String whole line in response or empty string if invalid index or running of command fails
     */
    public static String getAnswerAt(String cmd2launch, int answerIdx) {
        List<String> sa = ExecutingCommand.runNative(cmd2launch);

        if (answerIdx >= 0 && answerIdx < sa.size()) {
            return sa.get(answerIdx);
        }
        return "";
    }

    /**
     * Executes a command that may require elevated privileges. If already running as root or no sudo prefix is
     * configured, delegates directly to {@link #runNative(String)}. If a sudo prefix is configured and the command is
     * in the allowlist, prepends the prefix before execution. If the command is not in the allowlist, attempts to run
     * without the prefix (the command may still succeed if permissions allow).
     *
     * @param cmdToRun Command to run
     * @return A list of Strings representing the result of the command, or empty list if the command failed
     */
    public static List<String> runPrivilegedNative(String cmdToRun) {
        // If already elevated or no sudo prefix configured, run directly
        String prefix = PrivilegedUtil.getPrefix();
        if (UserGroupInfo.isElevated() || prefix.isEmpty()) {
            return runNative(cmdToRun);
        }

        // Check if command is in allowlist
        if (!PrivilegedUtil.isCommandAllowed(cmdToRun, PrivilegedUtil.getCommandAllowlist())) {
            LOG.debug("Command not in allowlist, running without prefix: {}", cmdToRun);
            return runNative(cmdToRun);
        }

        // Prepend prefix and execute
        String privilegedCmd = prefix + " " + cmdToRun;
        LOG.debug("Executing privileged command: {}", privilegedCmd);
        return runNative(privilegedCmd);
    }

    /**
     * Return first line of response for selected privileged command.
     *
     * @param cmd2launch String command to be launched with privilege escalation if needed
     * @return String or empty string if command failed or privilege requirements were not met
     */
    public static String getFirstPrivilegedAnswer(String cmd2launch) {
        List<String> sa = runPrivilegedNative(cmd2launch);
        if (sa.isEmpty()) {
            return "";
        }
        return sa.get(0);
    }

}
