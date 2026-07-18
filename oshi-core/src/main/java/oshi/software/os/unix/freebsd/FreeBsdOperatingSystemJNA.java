/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import java.util.List;
import java.util.Map;

import com.sun.jna.ptr.NativeLongByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.freebsd.Who;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.jna.platform.unix.FreeBsdLibc.Timeval;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.freebsd.FreeBsdOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSSession;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * JNA-backed FreeBSD operating system.
 */
@ThreadSafe
public class FreeBsdOperatingSystemJNA extends FreeBsdOperatingSystem {

    @Override
    protected String querySysctl(String name, String def) {
        return BsdSysctlUtil.sysctl(name, def);
    }

    @Override
    public FileSystem getFileSystem() {
        return new FreeBsdFileSystemJNA();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new FreeBsdInternetProtocolStatsJNA();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new FreeBsdNetworkParamsJNA();
    }

    @Override
    protected List<OSSession> queryWhoSessions() {
        return Who.queryUtxent();
    }

    @Override
    public int getProcessId() {
        return FreeBsdLibc.INSTANCE.getpid();
    }

    @Override
    public int getThreadId() {
        NativeLongByReference pTid = new NativeLongByReference();
        if (FreeBsdLibc.INSTANCE.thr_self(pTid) < 0) {
            return 0;
        }
        return pTid.getValue().intValue();
    }

    @Override
    protected OSProcess createProcess(int pid, Map<BsdPsKeyword, String> psMap) {
        return new FreeBsdOSProcessJNA(pid, psMap, this);
    }

    @Override
    protected long queryKernBoottimeSeconds() {
        Timeval tv = new Timeval();
        if (!BsdSysctlUtil.sysctl("kern.boottime", tv) || tv.tv_sec == 0) {
            return 0L;
        }
        // tv now points to a 128-bit timeval structure for boot time.
        // First 8 bytes are seconds, second 8 bytes are microseconds (we ignore)
        return tv.tv_sec;
    }
}
