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

    /**
     * Constructor.
     */
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
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the threadCount.
     *
     * @return the threadCount
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Gets the virtualSize.
     *
     * @return the virtualSize
     */
    public long getVirtualSize() {
        return virtualSize;
    }

    /**
     * Gets the kernelTime.
     *
     * @return the kernelTime
     */
    public long getKernelTime() {
        return kernelTime;
    }

    /**
     * Gets the userTime.
     *
     * @return the userTime
     */
    public long getUserTime() {
        return userTime;
    }

    /**
     * Gets the openFiles.
     *
     * @return the openFiles
     */
    public long getOpenFiles() {
        return openFiles;
    }
}
