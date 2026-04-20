/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

/**
 * Data holder for Windows Terminal Services (WTS) process information.
 */
public final class WtsInfo {

    private final String name;
    private final String path;
    private final int threadCount;
    private final long virtualSize;
    private final long kernelTime;
    private final long userTime;
    private final long openFiles;

    public WtsInfo(String name, String path, int threadCount, long virtualSize, long kernelTime, long userTime,
            long openFiles) {
        this.name = name;
        this.path = path;
        this.threadCount = threadCount;
        this.virtualSize = virtualSize;
        this.kernelTime = kernelTime;
        this.userTime = userTime;
        this.openFiles = openFiles;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @return the threadCount
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * @return the virtualSize
     */
    public long getVirtualSize() {
        return virtualSize;
    }

    /**
     * @return the kernelTime
     */
    public long getKernelTime() {
        return kernelTime;
    }

    /**
     * @return the userTime
     */
    public long getUserTime() {
        return userTime;
    }

    /**
     * @return the openFiles
     */
    public long getOpenFiles() {
        return openFiles;
    }
}
