/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix;

import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Abstract base for the {@code /proc}-filesystem Unix OSProcess implementations (Solaris and AIX). Both read process
 * info from {@code /proc}, enumerate threads from {@code /proc/<pid>/lwp}, count open files from
 * {@code /proc/<pid>/fd}, determine bitness from {@code pflags}, and fall back to a truncated command line. Subclasses
 * supply their native argument/environment read, their {@link OSThread} factory, and their platform-specific
 * {@code updateAttributes()}.
 */
@ThreadSafe
public abstract class AbstractProcOSProcess extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractProcOSProcess.class);

    private final Supplier<Integer> bitness = memoize(this::queryBitness);
    private final Supplier<String> commandLine = memoize(this::queryCommandLine);
    private final Supplier<Pair<List<String>, Map<String, String>>> cmdEnv = memoize(this::queryCommandlineEnvironment);

    // Populated by the subclass updateAttributes()
    protected volatile String commandLineBackup;
    protected volatile String user;
    protected volatile String userID;
    protected volatile String group;
    protected volatile String groupID;
    protected volatile long residentSetSize;
    protected volatile long privateResidentMemory;

    protected AbstractProcOSProcess(int pid) {
        super(pid);
    }

    @Override
    public String getCommandLine() {
        return this.commandLine.get();
    }

    private String queryCommandLine() {
        String cl = String.join(" ", getArguments());
        return cl.isEmpty() ? this.commandLineBackup : cl;
    }

    @Override
    public List<String> getArguments() {
        return cmdEnv.get().getA();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return cmdEnv.get().getB();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        try {
            String cwdLink = "/proc/" + getProcessID() + "/cwd";
            String cwd = new File(cwdLink).getCanonicalPath();
            if (!cwd.equals(cwdLink)) {
                return cwd;
            }
        } catch (IOException e) {
            LOG.trace("Couldn't find cwd for pid {}: {}", getProcessID(), e.getMessage());
        }
        return "";
    }

    @Override
    public String getUser() {
        return this.user;
    }

    @Override
    public String getUserID() {
        return this.userID;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public String getGroupID() {
        return this.groupID;
    }

    @Override
    public long getResidentMemory() {
        return this.residentSetSize;
    }

    @Override
    public long getPrivateResidentMemory() {
        return this.privateResidentMemory;
    }

    @Override
    public long getOpenFiles() {
        try (Stream<Path> fd = Files.list(Paths.get("/proc/" + getProcessID() + "/fd"))) {
            return fd.count();
        } catch (IOException e) {
            return 0L;
        }
    }

    @Override
    public int getBitness() {
        return this.bitness.get();
    }

    private int queryBitness() {
        List<String> pflags = ExecutingCommand.runNative("pflags " + getProcessID());
        for (String line : pflags) {
            if (line.contains("data model")) {
                if (line.contains("LP32")) {
                    return 32;
                } else if (line.contains("LP64")) {
                    return 64;
                }
            }
        }
        return 0;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        // Get process files in proc
        File directory = new File(String.format(Locale.ROOT, "/proc/%d/lwp", getProcessID()));
        File[] numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        if (numericFiles == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(numericFiles).parallel()
                .map(lwpidFile -> createThread(ParseUtil.parseIntOrDefault(lwpidFile.getName(), 0)))
                .filter(VALID_THREAD).collect(Collectors.toList());
    }

    /**
     * Reads this process's argument list and environment.
     *
     * @return a pair of (arg list, env map); either may be empty if it cannot be read
     */
    protected abstract Pair<List<String>, Map<String, String>> queryCommandlineEnvironment();

    /**
     * Creates a platform-specific {@link OSThread} for the given lwpid of this process.
     *
     * @param lwpid the thread (lwp) ID
     * @return a new OSThread
     */
    protected abstract OSThread createThread(int lwpid);
}
