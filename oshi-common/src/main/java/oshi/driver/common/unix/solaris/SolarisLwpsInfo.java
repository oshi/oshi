/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.solaris;

import java.nio.ByteBuffer;

import oshi.util.FileUtil;

/**
 * Pure-Java mirror of the Solaris {@code lwpsinfo_t} struct populated from {@code /proc/<pid>/lwp/<tid>/lwpsinfo}.
 * <p>
 * Fields and ordering match {@code <sys/procfs.h>}.
 */
public class SolarisLwpsInfo {

    public int pr_flag;
    public int pr_lwpid;
    public long pr_addr;
    public long pr_wchan;
    public byte pr_stype;
    public byte pr_state;
    public byte pr_sname;
    public byte pr_nice;
    public short pr_syscall;
    public byte pr_oldpri;
    public byte pr_cpu;
    public int pr_pri;
    public short pr_pctcpu;
    public SolarisTimestruc pr_start;
    public SolarisTimestruc pr_time;
    public byte[] pr_clname = new byte[8];
    public byte[] pr_oldname = new byte[16];
    public int pr_onpro;
    public int pr_bindpro;
    public int pr_bindpset;
    public int pr_lgrp;
    public long pr_last_onproc;
    public byte[] pr_name = new byte[32];

    public SolarisLwpsInfo(ByteBuffer buff) {
        this.pr_flag = FileUtil.readIntFromBuffer(buff);
        this.pr_lwpid = FileUtil.readIntFromBuffer(buff);
        this.pr_addr = FileUtil.readLongFromBuffer(buff);
        this.pr_wchan = FileUtil.readLongFromBuffer(buff);
        this.pr_stype = FileUtil.readByteFromBuffer(buff);
        this.pr_state = FileUtil.readByteFromBuffer(buff);
        this.pr_sname = FileUtil.readByteFromBuffer(buff);
        this.pr_nice = FileUtil.readByteFromBuffer(buff);
        this.pr_syscall = FileUtil.readShortFromBuffer(buff);
        this.pr_oldpri = FileUtil.readByteFromBuffer(buff);
        this.pr_cpu = FileUtil.readByteFromBuffer(buff);
        this.pr_pri = FileUtil.readIntFromBuffer(buff);
        this.pr_pctcpu = FileUtil.readShortFromBuffer(buff);
        FileUtil.readShortFromBuffer(buff); // pad
        this.pr_start = new SolarisTimestruc(buff);
        this.pr_time = new SolarisTimestruc(buff);
        FileUtil.readByteArrayFromBuffer(buff, this.pr_clname);
        FileUtil.readByteArrayFromBuffer(buff, this.pr_oldname);
        this.pr_onpro = FileUtil.readIntFromBuffer(buff);
        this.pr_bindpro = FileUtil.readIntFromBuffer(buff);
        this.pr_bindpset = FileUtil.readIntFromBuffer(buff);
        this.pr_lgrp = FileUtil.readIntFromBuffer(buff);
        this.pr_last_onproc = FileUtil.readLongFromBuffer(buff);
        FileUtil.readByteArrayFromBuffer(buff, this.pr_name);
    }
}
