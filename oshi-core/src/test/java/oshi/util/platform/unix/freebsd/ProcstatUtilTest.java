/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;
import oshi.util.PlatformEnum;

/**
 * Tests for {@link ProcstatUtil}. Pure parsing methods are covered with sample {@code procstat -f} output; the
 * integration tests are gated to FreeBSD.
 */
class ProcstatUtilTest {

    // FreeBSD `procstat -f <pid>` columns (10 total):
    // PID COMM FD T V FLAGS REF OFFSET PRO NAME

    @Test
    void testParseCwdMapCollectsCwdRows() {
        List<String> procstat = Arrays.asList("PID COMM             FD T V FLAGS    REF  OFFSET PRO NAME",
                "1234 bash            text v r r---- 1 0 - /usr/local/bin/bash",
                "1234 bash             cwd v d r---- 1 0 - /home/dan",
                "5678 sshd             cwd v d r---- 1 0 - /var/empty",
                "1234 bash               0 t s rw--- 1 0 - /dev/tty");
        Map<Integer, String> map = ProcstatUtil.parseCwdMap(procstat);
        assertThat(map.size(), is(2));
        assertThat(map.get(1234), is("/home/dan"));
        assertThat(map.get(5678), is("/var/empty"));
    }

    @Test
    void testParseCwdMapNoCwd() {
        assertThat(
                ProcstatUtil.parseCwdMap(
                        Collections.singletonList("PID COMM             FD T V FLAGS    REF  OFFSET PRO NAME")),
                is(anEmptyMap()));
    }

    @Test
    void testParseCwdReturnsFirstCwdPath() {
        List<String> procstat = Arrays.asList("PID COMM             FD T V FLAGS    REF  OFFSET PRO NAME",
                "1234 bash             cwd v d r---- 1 0 - /home/dan",
                "1234 bash               0 t s rw--- 1 0 - /dev/tty");
        assertThat(ProcstatUtil.parseCwd(procstat), is("/home/dan"));
    }

    @Test
    void testParseCwdEmptyWhenNoCwd() {
        assertThat(ProcstatUtil.parseCwd(Collections.<String>emptyList()), is(emptyString()));
    }

    @Test
    void testParseOpenFilesExcludesVdAndDash() {
        // The 5th column (index 4) is the file type: V (vnode), d (cwd/root/text), - (jail/text/etc.) are excluded.
        // Rows with t (terminal), s (socket), p (pipe), k (kqueue), etc., count.
        List<String> procstat = Arrays.asList("PID COMM             FD T V FLAGS    REF  OFFSET PRO NAME",
                "1234 bash             cwd v d r---- 1 0 - /home/dan", // excluded (d)
                "1234 bash            text v V r---- 1 0 - /usr/bin/bash", // excluded (V)
                "1234 bash             jail v - r---- 1 0 - /", // excluded (-)
                "1234 bash               0 t s rw--- 1 0 - /dev/tty", // counted
                "1234 bash               1 t s rw--- 1 0 - /dev/tty", // counted
                "1234 bash               3 p s rw--- 1 0 - -"); // counted
        // Header has only 9 tokens before "NAME" but split with limit 10 should keep "NAME" as the last; however the
        // header row's 5th column ("V") matches "Vd-" so the header is naturally excluded.
        assertThat(ProcstatUtil.parseOpenFiles(procstat), is(3L));
    }

    @Test
    void testParseOpenFilesEmpty() {
        assertThat(ProcstatUtil.parseOpenFiles(Collections.<String>emptyList()), is(0L));
    }

    @Test
    void testProcstatLive() {
        if (PlatformEnum.getCurrentPlatform().equals(PlatformEnum.FREEBSD)) {
            int pid = new SystemInfo().getOperatingSystem().getProcessId();
            assertThat("Open files must be nonnegative", ProcstatUtil.getOpenFiles(pid), is(greaterThanOrEqualTo(0L)));
            assertThat("CwdMap should have at least one element", ProcstatUtil.getCwdMap(-1), is(not(anEmptyMap())));
            assertThat("CwdMap with pid should have at least one element", ProcstatUtil.getCwdMap(pid),
                    is(not(anEmptyMap())));
            assertThat("Cwd should be nonempty", ProcstatUtil.getCwd(pid), is(not(emptyString())));
        }
    }
}
