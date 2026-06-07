/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

import java.nio.ByteBuffer;

import oshi.util.FileUtil;

/**
 * Pure-Java mirror of the AIX {@code lwpsinfo_t} struct populated from {@code /proc/<pid>/lwp/<tid>/lwpsinfo}.
 * <p>
 * Fields and ordering match {@code <sys/procfs.h>}.
 */
public class AixLwpsInfo {

    /** Length of {@code pr_clname} (printable character representing pr_policy). */
    public static final int PRCLSZ = 8;

    public long pr_lwpid; // thread id
    public long pr_addr; // internal address of thread
    public long pr_wchan; // wait addr for sleeping thread
    public int pr_flag; // thread flags
    public byte pr_wtype; // type of thread wait
    public byte pr_state; // numeric scheduling state
    public byte pr_sname; // printable character representing pr_state
    public byte pr_nice; // nice for cpu usage
    public int pr_pri; // priority, high value = high priority
    public int pr_policy; // scheduling policy
    public byte[] pr_clname = new byte[PRCLSZ]; // printable character representing pr_policy
    public int pr_onpro; // processor on which thread last ran
    public int pr_bindpro; // processor to which thread is bound

    public AixLwpsInfo(ByteBuffer buff) {
        this.pr_lwpid = FileUtil.readLongFromBuffer(buff);
        this.pr_addr = FileUtil.readLongFromBuffer(buff);
        this.pr_wchan = FileUtil.readLongFromBuffer(buff);
        this.pr_flag = FileUtil.readIntFromBuffer(buff);
        this.pr_wtype = FileUtil.readByteFromBuffer(buff);
        this.pr_state = FileUtil.readByteFromBuffer(buff);
        this.pr_sname = FileUtil.readByteFromBuffer(buff);
        this.pr_nice = FileUtil.readByteFromBuffer(buff);
        this.pr_pri = FileUtil.readIntFromBuffer(buff);
        this.pr_policy = FileUtil.readIntFromBuffer(buff);
        FileUtil.readByteArrayFromBuffer(buff, this.pr_clname);
        this.pr_onpro = FileUtil.readIntFromBuffer(buff);
        this.pr_bindpro = FileUtil.readIntFromBuffer(buff);
    }
}
