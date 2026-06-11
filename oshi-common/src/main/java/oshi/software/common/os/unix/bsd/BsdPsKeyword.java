/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

/**
 * The union of {@code ps} output columns used by the BSD-family {@code OSProcess} implementations (FreeBSD, OpenBSD,
 * DragonFly, NetBSD). Each platform declares its own ordered subset (see {@code PS_KEYWORDS} on the per-platform
 * {@code OSProcess} base) and parses {@code ps} output positionally against that subset. The constant order here is not
 * significant; only the per-platform subset order is.
 */
public enum BsdPsKeyword {
    STATE, PID, PPID, USER, UID, GROUP, GID, RGID, NLWP, PRI, VSZ, RSS, ETIME, ETIMES, CPUTIME, SYSTIME, TIME, COMM,
    UCOMM, MAJFLT, MINFLT, NVCSW, NIVCSW, ARGS, COMMAND;
}
