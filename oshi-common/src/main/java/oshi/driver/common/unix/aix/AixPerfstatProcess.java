/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

import oshi.annotation.concurrent.NotThreadSafe;

/**
 * Backend-neutral carrier for the {@code perfstat_process_t} fields OSHI consumes. Populated by the JNA and FFM
 * {@code PerfstatProcess} drivers so the AIX {@code OperatingSystem} and {@code OSProcess} logic can be shared.
 */
@NotThreadSafe
public final class AixPerfstatProcess {
    /** Process ID. */
    public long pid;
    /** Number of threads. */
    public long num_threads;
    /** Real memory used by the data section, in kilobytes. */
    public long proc_real_mem_data;
    /** Real memory used by the text section, in kilobytes. */
    public long proc_real_mem_text;
    /** Real memory in use, in kilobytes. */
    public long real_inuse;
    /** User-mode CPU time, in milliseconds. */
    public double ucpu_time;
    /** System-mode CPU time, in milliseconds. */
    public double scpu_time;
}
