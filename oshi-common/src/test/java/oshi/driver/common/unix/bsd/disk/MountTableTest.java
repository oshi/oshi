/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.bsd.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Pair;

class MountTableTest {

    private static void assertEntry(MountTable.Entry e, String volume, String uuid, String path, String type,
            String options) {
        assertThat(e.getVolume(), is(volume));
        assertThat(e.getUuid(), is(uuid));
        assertThat(e.getPath(), is(path));
        assertThat(e.getType(), is(type));
        assertThat(e.getOptions(), is(options));
    }

    @Test
    void testParseFreeBsdLibxo() {
        // mount -p --libxo json on FreeBSD 16.0-CURRENT (cfarm430), trimmed, with an invented mount point holding
        // spaces long enough that mount -p would separate it from the type with a single space, and an escaped quote
        List<String> json = List.of("{\"mount\": {\"fstab\": [{\"device\":\"/dev/da0p2\",\"mntpoint\":\"/\","
                + "\"fstype\":\"ufs\",\"opts\":\"rw,noatime\",\"dump\":1,\"pass\":1}, "
                + "{\"device\":\"tmpfs\",\"mntpoint\":\"/tmp\",\"fstype\":\"tmpfs\",\"opts\":\"rw\",\"dump\":0,"
                + "\"pass\":0}, {\"device\":\"/dev/da1p1\",\"mntpoint\":\"/mnt/My External \\\"Backup\\\" Drive\","
                + "\"fstype\":\"ufs\",\"opts\":\"rw\",\"dump\":2,\"pass\":2}]}}");
        List<MountTable.Entry> entries = MountTable.parseFreeBsdLibxo(json);
        assertThat(entries, hasSize(3));
        assertEntry(entries.get(0), "/dev/da0p2", "", "/", "ufs", "rw,noatime");
        assertEntry(entries.get(1), "tmpfs", "", "/tmp", "tmpfs", "rw");
        assertEntry(entries.get(2), "/dev/da1p1", "", "/mnt/My External \"Backup\" Drive", "ufs", "rw");
    }

    @Test
    void testParseFreeBsdLibxoEscapes() {
        // Each JSON escape a path may carry: a backslash, a tab, a slash and a unicode escape, and one ending the value
        String path = "/mnt/a\\\\b\\tc\\/d\\u00e9\\u0041";
        List<String> json = List.of("{\"mount\": {\"fstab\": [{\"device\":\"/dev/da1p1\",\"mntpoint\":\"" + path
                + "\",\"fstype\":\"ufs\",\"opts\":\"rw\",\"dump\":2,\"pass\":2}]}}");
        List<MountTable.Entry> entries = MountTable.parseFreeBsdLibxo(json);
        assertThat(entries, hasSize(1));
        assertEntry(entries.get(0), "/dev/da1p1", "", "/mnt/a\\b\tc/déA", "ufs", "rw");
    }

    @Test
    void testParseFreeBsdLibxoUnsupported() {
        // DragonFly's mount has no libxo, and prints usage instead
        assertThat(MountTable.parseFreeBsdLibxo(List.of("mount: illegal option -- -",
                "usage: mount [-adfpruvw] [-F fstab] [-o options] [-t ufs | external_type]")), is(empty()));
        assertThat(MountTable.parseFreeBsdLibxo(Collections.emptyList()), is(empty()));
    }

    @Test
    void testParseFstab() {
        // mount -p on DragonFly 6.4.2 in CI, as root, with a mount point holding spaces: one tab between fields, and
        // a space between the type and the options
        List<MountTable.Entry> dragonfly = MountTable.parseFstab(List.of("serno/QM00001.s1d\t/\thammer2 rw\t1 1",
                "tmpfs\t/mnt/plain\ttmpfs rw\t0 0", "tmpfs\t/mnt/My External Backup Drive\ttmpfs rw\t0 0"));
        assertThat(dragonfly, hasSize(3));
        assertEntry(dragonfly.get(0), "serno/QM00001.s1d", "", "/", "hammer2", "rw");
        assertEntry(dragonfly.get(2), "tmpfs", "", "/mnt/My External Backup Drive", "tmpfs", "rw");
        // FreeBSD pads with tabs, the type and options apart
        List<MountTable.Entry> freebsd = MountTable.parseFstab(
                List.of("/dev/da0p2\t\t/\t\t\tufs\trw,noatime \t1 1", "devfs\t\t\t/dev\t\t\tdevfs\trw\t\t0 0"));
        assertThat(freebsd, hasSize(2));
        assertEntry(freebsd.get(0), "/dev/da0p2", "", "/", "ufs", "rw,noatime");
        assertEntry(freebsd.get(1), "devfs", "", "/dev", "devfs", "rw");
        // FreeBSD 15.1 in CI, as root: a mount point of 24 characters or more is followed by a single space
        List<MountTable.Entry> longPath = MountTable
                .parseFstab(List.of("/dev/md0\t\t/mnt/My External Backup Drive ufs\trw\t\t2 2"));
        assertThat(longPath, hasSize(1));
        assertEntry(longPath.get(0), "/dev/md0", "", "/mnt/My External Backup Drive", "ufs", "rw");
        // The same, with options long enough that FreeBSD follows them with a space before the dump field
        List<MountTable.Entry> longOpts = MountTable
                .parseFstab(List.of("/dev/md0\t\t/mnt/My External Backup Drive ufs\trw,noatime \t2 2"));
        assertEntry(longOpts.get(0), "/dev/md0", "", "/mnt/My External Backup Drive", "ufs", "rw,noatime");
        // No tabs at all falls back to whitespace
        List<MountTable.Entry> spaced = MountTable.parseFstab(List.of("/dev/md0 /mnt/a ufs rw 2 2"));
        assertEntry(spaced.get(0), "/dev/md0", "", "/mnt/a", "ufs", "rw");
    }

    @Test
    void testParseNetBsd() {
        // mount on NetBSD 10.1 (cfarm428), with an invented mount point holding spaces
        List<String> lines = List.of("/dev/dk1 on / type ffs (noatime, local)", "tmpfs on /tmp type tmpfs (local)",
                "tmpfs on /var/shm type tmpfs (local)", "/dev/dk2 on /home/my files type ffs (noatime, local)");
        List<MountTable.Entry> entries = MountTable.parseNetBsd(lines);
        assertThat(entries, hasSize(4));
        assertEntry(entries.get(0), "/dev/dk1", "", "/", "ffs", "(noatime, local)");
        assertEntry(entries.get(2), "tmpfs", "", "/var/shm", "tmpfs", "(local)");
        assertEntry(entries.get(3), "/dev/dk2", "", "/home/my files", "ffs", "(noatime, local)");
    }

    @Test
    void testParseOpenBsdVerbose() {
        // mount -v on OpenBSD 7.9 (cfarm220), then two file systems with no disk and so no DUID, which a split on
        // whitespace read with "on" as the identifier and "type" as the path
        List<String> lines = List.of(
                "/dev/sd0a (ed2d8ecd4d5492ee.a) on / type ffs (rw, local, ctime=Wed Oct  7 22:17:15 2026)",
                "/dev/sd0k (ed2d8ecd4d5492ee.k) on /home/my files type ffs (rw, local, nodev, nosuid)",
                "tmpfs on /tmp type tmpfs (rw, local, nodev, nosuid)",
                "server:/export on /mnt/nfs type nfs (rw, v3, udp, timeo=100, retrans=101)");
        List<MountTable.Entry> entries = MountTable.parseOpenBsdVerbose(lines);
        assertThat(entries, hasSize(4));
        assertEntry(entries.get(0), "/dev/sd0a", "ed2d8ecd4d5492ee.a", "/", "ffs",
                "(rw, local, ctime=Wed Oct  7 22:17:15 2026)");
        assertEntry(entries.get(1), "/dev/sd0k", "ed2d8ecd4d5492ee.k", "/home/my files", "ffs",
                "(rw, local, nodev, nosuid)");
        assertEntry(entries.get(2), "tmpfs", "", "/tmp", "tmpfs", "(rw, local, nodev, nosuid)");
        assertEntry(entries.get(3), "server:/export", "", "/mnt/nfs", "nfs", "(rw, v3, udp, timeo=100, retrans=101)");
    }

    @Test
    void testParseDfInodes() {
        // df -i from FreeBSD 16.0-CURRENT, NetBSD 10.1 and OpenBSD 7.9, header lines included. FreeBSD prints - for
        // the percentage of a file system without inodes. NetBSD mounts tmpfs twice, which a key by volume collapsed
        List<String> lines = """
                Filesystem  1K-blocks      Used     Avail Capacity iused     ifree %iused  Mounted on
                /dev/da0p2   80987124  57459148  17049008    77%  958125   9538641    9%   /
                devfs               1         0         1     0%       0         0     -   /dev
                Filesystem     512-blocks         Used        Avail %Cap      iUsed     iAvail %iCap Mounted on
                tmpfs             8375792        37504      8338288   0%        481    4187421    0% /tmp
                tmpfs             8375792            0      8375792   0%          1    4187901    0% /var/shm
                Filesystem  512-blocks      Used     Avail Capacity  iused    ifree %iused  Mounted on
                /dev/sd0k    619226672 377471440 210793904    65%  4246584 15494086   22%   /home
                /dev/sd0h     40614428  14885588 -23698120   100%   154725  2489113    6%   /usr/local stuff
                """.lines().toList();
        Pair<Map<String, Long>, Map<String, Long>> inodes = MountTable.parseDfInodes(lines);
        Map<String, Long> free = inodes.getA();
        Map<String, Long> total = inodes.getB();
        assertThat(free, is(aMapWithSize(6)));
        assertThat(free.get("/"), is(9538641L));
        assertThat(total.get("/"), is(958125L + 9538641L));
        assertThat(total.get("/dev"), is(0L));
        assertThat(free.get("/tmp"), is(4187421L));
        assertThat(free.get("/var/shm"), is(4187901L));
        assertThat(total.get("/home"), is(4246584L + 15494086L));
        // Negative avail, a full file system, and a mount point holding a space
        assertThat(free.get("/usr/local stuff"), is(2489113L));
        assertThat(MountTable.parseDfInodes(Collections.emptyList()).getA(), is(anEmptyMap()));
    }
}
