package oshi.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ShellUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ShellUtil.class);

    private static final String SHELL_CMD_LOG = "execute shell {}";


    public static String executeSh(String shCommand) {
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace(SHELL_CMD_LOG, shCommand);
            }

            return execute("/bin/sh", "-c", shCommand);
        } catch (Exception ignored) {

            return "";
        }
    }

    /**
     * Execute the script with a return result
     *
     * @param command
     * @return
     * @throws IOException
     */
    public static String execute(String... command) throws IOException {
        return execute((File) null, command);
    }

    /**
     * Execute the script with a return result
     *
     * @param command
     * @return
     * @throws IOException
     */
    public static String execute(Long timeoutMills, String... command) throws IOException {
        return execute((File) null, timeoutMills, command);
    }

    /**
     * Execute the script with a return result
     *
     * @param workDir
     * @param command
     * @return
     * @throws IOException
     */
    public static String execute(File workDir, String... command) throws IOException {
        return execute(workDir, null, command);
    }

    /**
     * Execute the script with a return result
     *
     * @param workDir
     * @param timeoutMills
     * @param command
     * @return
     * @throws IOException
     */
    public static String execute(File workDir, Long timeoutMills, String... command) throws IOException {
        // Execute the command
        Process process = Runtime.getRuntime().exec(command, null, workDir);

        // Generate a Shell Process object
        ShellProcess shellProcess = new ShellProcess(process);
        if (timeoutMills != null) {
            shellProcess.setTimeoutMills(timeoutMills);
        }

        return getProcessReturnMessage(command, shellProcess);
    }

    /**
     * Obtain the execution result
     *
     * @param shellProcess
     * @return
     * @throws IOException
     */
    public static String getProcessReturnMessage(String[] command, ShellProcess shellProcess)
            throws IOException {

        Process process = shellProcess.getProcess();
        // standardOutputStreams
        BufferedReader stdReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        // errorOutputStream
        BufferedReader errReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));

        // Blocks the child process within the timeout period and waits for the child process to end execution
        boolean processExit = false;
        try {
            processExit = process.waitFor(
                    shellProcess.getTimeoutMills(),
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        boolean isStdOutReady = stdReader.ready();
        boolean isErrOutReady = errReader.ready();

        // If the sub-process does not exit and there is no data in the output stream,
        // it is assumed to be a timeout and processed as a timeout
        if (!processExit && !isStdOutReady && !isErrOutReady) {
            // Destroy the child process
            process.destroy();
            process = null; // help gc
            throw new IllegalStateException("execute command timeout");
        }

        // Both the standard output stream and the abnormal output stream are returned if there is no data
        if (!isStdOutReady && !isErrOutReady) {
            return "";
        }

        // Read the results
        BufferedReader reader = isStdOutReady ? stdReader : errReader;
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }

        String result = builder.toString();

        LOG.info("shell command execute result is: {}", result);

        // Error output stream
        if (!isStdOutReady) {
            LOG.error("shell command result error stream: {}", result);
        }

        return result;
    }


    /**
     * Static inner classes of shell subprocesses
     */
    private static class ShellProcess {

        // The default execution timeout period
        private static final long DEFAULT_TIMEOUT = 50 * 1000L;

        private Process process;

        // Timeout timestamp
        private long timeoutMills;

        public ShellProcess(Process process) {
            this.process = process;
            this.timeoutMills = DEFAULT_TIMEOUT;
        }

        public Process getProcess() {
            return process;
        }

        public void setProcess(Process process) {
            this.process = process;
        }

        public long getTimeoutMills() {
            return timeoutMills;
        }

        public void setTimeoutMills(long timeoutMills) {
            this.timeoutMills = timeoutMills;
        }
    }
}
