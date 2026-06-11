/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.solaris;

import java.nio.ByteBuffer;

import oshi.util.FileUtil;

/**
 * Pure-Java mirror of the Solaris {@code prusage_t} struct populated from {@code /proc/<pid>/usage} or
 * {@code /proc/<pid>/lwp/<tid>/usage}.
 * <p>
 * Fields and ordering match {@code <sys/procfs.h>}. Trailing fields not used by OSHI are skipped.
 */
public class SolarisPrUsage {

    public int pr_lwpid;
    public int pr_count;
    public SolarisTimestruc pr_tstamp;
    public SolarisTimestruc pr_create;
    public SolarisTimestruc pr_term;
    public SolarisTimestruc pr_rtime;
    public SolarisTimestruc pr_utime;
    public SolarisTimestruc pr_stime;
    public SolarisTimestruc pr_ttime;
    public SolarisTimestruc pr_tftime;
    public SolarisTimestruc pr_dftime;
    public SolarisTimestruc pr_kftime;
    public SolarisTimestruc pr_ltime;
    public SolarisTimestruc pr_slptime;
    public SolarisTimestruc pr_wtime;
    public SolarisTimestruc pr_stoptime;
    public long pr_minf;
    public long pr_majf;
    public long pr_vctx;
    public long pr_ictx;
    public long pr_ioch; // chars read and written

    public SolarisPrUsage(ByteBuffer buff) {
        this.pr_lwpid = FileUtil.readIntFromBuffer(buff);
        this.pr_count = FileUtil.readIntFromBuffer(buff);
        this.pr_tstamp = new SolarisTimestruc(buff);
        this.pr_create = new SolarisTimestruc(buff);
        this.pr_term = new SolarisTimestruc(buff);
        this.pr_rtime = new SolarisTimestruc(buff);
        this.pr_utime = new SolarisTimestruc(buff);
        this.pr_stime = new SolarisTimestruc(buff);
        this.pr_ttime = new SolarisTimestruc(buff);
        this.pr_tftime = new SolarisTimestruc(buff);
        this.pr_dftime = new SolarisTimestruc(buff);
        this.pr_kftime = new SolarisTimestruc(buff);
        this.pr_ltime = new SolarisTimestruc(buff);
        this.pr_slptime = new SolarisTimestruc(buff);
        this.pr_wtime = new SolarisTimestruc(buff);
        this.pr_stoptime = new SolarisTimestruc(buff);
        // 6 filler Timestrucs (96 bytes)
        for (int i = 0; i < 6; i++) {
            FileUtil.readLongFromBuffer(buff);
            FileUtil.readLongFromBuffer(buff);
        }
        this.pr_minf = FileUtil.readLongFromBuffer(buff);
        this.pr_majf = FileUtil.readLongFromBuffer(buff);
        FileUtil.readLongFromBuffer(buff); // pr_nswap
        FileUtil.readLongFromBuffer(buff); // pr_inblk
        FileUtil.readLongFromBuffer(buff); // pr_oublk
        FileUtil.readLongFromBuffer(buff); // pr_msnd
        FileUtil.readLongFromBuffer(buff); // pr_mrcv
        FileUtil.readLongFromBuffer(buff); // pr_sigs
        this.pr_vctx = FileUtil.readLongFromBuffer(buff);
        this.pr_ictx = FileUtil.readLongFromBuffer(buff);
        FileUtil.readLongFromBuffer(buff); // pr_sysc
        this.pr_ioch = FileUtil.readLongFromBuffer(buff);
        // 10 fillers — ignored
    }
}
