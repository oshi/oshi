/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.solaris;

import java.nio.ByteBuffer;

import oshi.util.FileUtil;

/**
 * Pure-Java mirror of the Solaris {@code psinfo_t} struct populated from {@code /proc/<pid>/psinfo}.
 * <p>
 * Fields and ordering match {@code <sys/procfs.h>}.
 */
public class SolarisPsInfo {

    public int pr_flag;
    public int pr_nlwp;
    public int pr_pid;
    public int pr_ppid;
    public int pr_pgid;
    public int pr_sid;
    public int pr_uid;
    public int pr_euid;
    public int pr_gid;
    public int pr_egid;
    public long pr_addr; // user-space pointer as a raw address
    public long pr_size; // Kbytes
    public long pr_rssize;
    public long pr_rssizepriv;
    public long pr_ttydev;
    public short pr_pctcpu;
    public short pr_pctmem;
    public SolarisTimestruc pr_start;
    public SolarisTimestruc pr_time;
    public SolarisTimestruc pr_ctime;
    public byte[] pr_fname = new byte[16];
    public byte[] pr_psargs = new byte[80];
    public int pr_wstat;
    public int pr_argc;
    public long pr_argv; // user-space pointer
    public long pr_envp; // user-space pointer
    public byte pr_dmodel; // 1=32-bit, 2=64-bit
    public int pr_taskid;
    public int pr_projid;
    public int pr_nzomb;
    public int pr_poolid;
    public int pr_zoneid;
    public int pr_contract;
    public SolarisLwpsInfo pr_lwp;

    public SolarisPsInfo(ByteBuffer buff) {
        this.pr_flag = FileUtil.readIntFromBuffer(buff);
        this.pr_nlwp = FileUtil.readIntFromBuffer(buff);
        this.pr_pid = FileUtil.readIntFromBuffer(buff);
        this.pr_ppid = FileUtil.readIntFromBuffer(buff);
        this.pr_pgid = FileUtil.readIntFromBuffer(buff);
        this.pr_sid = FileUtil.readIntFromBuffer(buff);
        this.pr_uid = FileUtil.readIntFromBuffer(buff);
        this.pr_euid = FileUtil.readIntFromBuffer(buff);
        this.pr_gid = FileUtil.readIntFromBuffer(buff);
        this.pr_egid = FileUtil.readIntFromBuffer(buff);
        this.pr_addr = FileUtil.readLongFromBuffer(buff);
        this.pr_size = FileUtil.readLongFromBuffer(buff);
        this.pr_rssize = FileUtil.readLongFromBuffer(buff);
        this.pr_rssizepriv = FileUtil.readLongFromBuffer(buff);
        this.pr_ttydev = FileUtil.readLongFromBuffer(buff);
        this.pr_pctcpu = FileUtil.readShortFromBuffer(buff);
        this.pr_pctmem = FileUtil.readShortFromBuffer(buff);
        // Align to 8 bytes for following Timestrucs (pad 4 bytes after the two shorts)
        FileUtil.readIntFromBuffer(buff);
        this.pr_start = new SolarisTimestruc(buff);
        this.pr_time = new SolarisTimestruc(buff);
        this.pr_ctime = new SolarisTimestruc(buff);
        FileUtil.readByteArrayFromBuffer(buff, this.pr_fname);
        FileUtil.readByteArrayFromBuffer(buff, this.pr_psargs);
        this.pr_wstat = FileUtil.readIntFromBuffer(buff);
        this.pr_argc = FileUtil.readIntFromBuffer(buff);
        this.pr_argv = FileUtil.readLongFromBuffer(buff);
        this.pr_envp = FileUtil.readLongFromBuffer(buff);
        this.pr_dmodel = FileUtil.readByteFromBuffer(buff);
        // 3-byte pad (pr_pad2)
        FileUtil.readByteFromBuffer(buff);
        FileUtil.readByteFromBuffer(buff);
        FileUtil.readByteFromBuffer(buff);
        this.pr_taskid = FileUtil.readIntFromBuffer(buff);
        this.pr_projid = FileUtil.readIntFromBuffer(buff);
        this.pr_nzomb = FileUtil.readIntFromBuffer(buff);
        this.pr_poolid = FileUtil.readIntFromBuffer(buff);
        this.pr_zoneid = FileUtil.readIntFromBuffer(buff);
        this.pr_contract = FileUtil.readIntFromBuffer(buff);
        FileUtil.readIntFromBuffer(buff); // pr_filler
        this.pr_lwp = new SolarisLwpsInfo(buff);
    }
}
