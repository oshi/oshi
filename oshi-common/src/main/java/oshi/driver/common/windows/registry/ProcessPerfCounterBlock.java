/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import oshi.annotation.concurrent.Immutable;

/**
 * Encapsulates process performance data from the registry performance counter block.
 */
@Immutable
public final class ProcessPerfCounterBlock {
    private final String name;
    private final int parentProcessID;
    private final int priority;
    private final long privateWorkingSetSize;
    private final long workingSetSize;
    private final long startTime;
    private final long upTime;
    private final long bytesRead;
    private final long bytesWritten;
    private final long pageFaults;

    public ProcessPerfCounterBlock(String name, int parentProcessID, int priority, long privateWorkingSetSize,
            long workingSetSize, long startTime, long upTime, long bytesRead, long bytesWritten, long pageFaults) {
        this.name = name;
        this.parentProcessID = parentProcessID;
        this.priority = priority;
        this.privateWorkingSetSize = privateWorkingSetSize;
        this.workingSetSize = workingSetSize;
        this.startTime = startTime;
        this.upTime = upTime;
        this.bytesRead = bytesRead;
        this.bytesWritten = bytesWritten;
        this.pageFaults = pageFaults;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the parentProcessID
     */
    public int getParentProcessID() {
        return parentProcessID;
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @return the Private Working Set size
     */
    public long getPrivateWorkingSetSize() {
        return privateWorkingSetSize;
    }

    /**
     * @return the Working Set size (RSS)
     */
    public long getWorkingSetSize() {
        return workingSetSize;
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @return the upTime
     */
    public long getUpTime() {
        return upTime;
    }

    /**
     * @return the bytesRead
     */
    public long getBytesRead() {
        return bytesRead;
    }

    /**
     * @return the bytesWritten
     */
    public long getBytesWritten() {
        return bytesWritten;
    }

    /**
     * @return the pageFaults
     */
    public long getPageFaults() {
        return pageFaults;
    }
}
