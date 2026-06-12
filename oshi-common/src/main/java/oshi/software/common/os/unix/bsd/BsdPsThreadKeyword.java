/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

/**
 * The union of {@code ps} thread (per-LWP) output columns used by the BSD-family {@code OSThread} implementations
 * (FreeBSD, OpenBSD, DragonFly, NetBSD). Each platform declares its own ordered subset (see {@code PS_THREAD_KEYWORDS}
 * on the per-platform {@code OSProcess} base) and parses {@code ps} output positionally against that subset. The
 * constant order here is not significant; only the per-platform subset order is.
 */
public enum BsdPsThreadKeyword {
    TDNAME, LWP, TID, LID, STATE, ETIME, ETIMES, CPUTIME, SYSTIME, TIME, TDADDR, NIVCSW, NVCSW, MAJFLT, MINFLT, PRI,
    ARGS;
}
