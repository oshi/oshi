/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import java.util.concurrent.atomic.AtomicBoolean;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GpuStats;
import oshi.hardware.GpuTicks;

/**
 * A no-op {@link GpuStats} implementation returned by platforms that do not support a native stats session. All metric
 * methods return sentinel values ({@code -1} or a zero-tick snapshot). {@link #close()} is idempotent.
 */
@ThreadSafe
public final class NoOpGpuStats implements GpuStats {

    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Override
    public void close() {
        closed.set(true);
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public GpuTicks getGpuTicks() {
        checkOpen();
        return new DefaultGpuTicks(System.nanoTime() / 100L, 0L);
    }

    @Override
    public double getGpuUtilization() {
        checkOpen();
        return -1d;
    }

    @Override
    public long getVramUsed() {
        checkOpen();
        return -1L;
    }

    @Override
    public long getSharedMemoryUsed() {
        checkOpen();
        return -1L;
    }

    @Override
    public double getTemperature() {
        checkOpen();
        return -1d;
    }

    @Override
    public double getPowerDraw() {
        checkOpen();
        return -1d;
    }

    @Override
    public long getCoreClockMhz() {
        checkOpen();
        return -1L;
    }

    @Override
    public long getMemoryClockMhz() {
        checkOpen();
        return -1L;
    }

    @Override
    public double getFanSpeedPercent() {
        checkOpen();
        return -1d;
    }

    private void checkOpen() {
        if (closed.get()) {
            throw new IllegalStateException("GpuStats session is closed");
        }
    }
}
