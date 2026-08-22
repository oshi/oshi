/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.api.io.TempDir;

import oshi.software.os.OSSession;

/**
 * Tests the systemd session-file fallback shared by the bindings. It reads only files, so a temporary directory stands
 * in for {@code /run/systemd/sessions} and the parse runs without Linux or a running systemd.
 */
class WhoSystemdTest {

    // Writes one session file: systemd session files are KEY=value lines, named by numeric session id.
    private static void session(Path dir, String name, String... keyValues) throws IOException {
        Files.write(dir.resolve(name), List.of(keyValues), StandardCharsets.UTF_8);
    }

    private static List<String> users(List<OSSession> sessions) {
        return sessions.stream().map(OSSession::getUserName).collect(Collectors.toList());
    }

    @Test
    void testReadsUserTerminalAndHost(@TempDir Path dir) throws IOException {
        // REALTIME is microseconds since the epoch, so the reported login time is it divided by 1000
        session(dir, "12", "USER=alice", "TTY=pts/3", "REMOTE_HOST=10.0.0.5", "REALTIME=1600000000000000");
        List<OSSession> sessions = Who.querySystemdFiles(dir.toFile());

        assertThat(sessions, hasSize(1));
        OSSession s = sessions.get(0);
        assertThat(s.getUserName(), is("alice"));
        assertThat(s.getTerminalDevice(), is("pts/3"));
        assertThat(s.getHost(), is("10.0.0.5"));
        assertThat("REALTIME microseconds become milliseconds", s.getLoginTime(), is(1600000000000L));
    }

    @Test
    void testDefaultsTerminalToTheSessionIdAndHostToEmpty(@TempDir Path dir) throws IOException {
        session(dir, "7", "USER=bob", "REALTIME=1600000000000000");
        List<OSSession> sessions = Who.querySystemdFiles(dir.toFile());

        assertThat(sessions, hasSize(1));
        assertThat(sessions.get(0).getTerminalDevice(), is("7"));
        assertThat(sessions.get(0).getHost(), is(""));
    }

    @Test
    void testFallsBackToFileTimeWhenRealtimeIsAbsentOrUnparseable(@TempDir Path dir) throws IOException {
        session(dir, "1", "USER=carol");
        session(dir, "2", "USER=dave", "REALTIME=notanumber");
        List<OSSession> sessions = Who.querySystemdFiles(dir.toFile());

        assertThat(sessions, hasSize(2));
        for (OSSession s : sessions) {
            assertThat("A session with no usable REALTIME still gets the file's timestamp", s.getLoginTime() > 0L,
                    is(true));
        }
    }

    @Test
    void testSkipsFilesWithoutAUser(@TempDir Path dir) throws IOException {
        session(dir, "1", "TTY=pts/0", "REALTIME=1600000000000000");
        session(dir, "2", "USER=", "REALTIME=1600000000000000");
        session(dir, "3", "USER=erin", "REALTIME=1600000000000000");
        assertThat("Only the session naming a user counts", users(Who.querySystemdFiles(dir.toFile())),
                contains("erin"));
    }

    @Test
    void testIgnoresNonNumericFileNames(@TempDir Path dir) throws IOException {
        // systemd names session files by numeric id; the directory also holds other bookkeeping entries
        session(dir, "sessions.state", "USER=root", "REALTIME=1600000000000000");
        session(dir, "c1", "USER=root", "REALTIME=1600000000000000");
        session(dir, "42", "USER=frank", "REALTIME=1600000000000000");
        assertThat(users(Who.querySystemdFiles(dir.toFile())), contains("frank"));
    }

    @Test
    void testAbsentDirectoryYieldsNoSessions(@TempDir Path dir) {
        assertThat(Who.querySystemdFiles(dir.resolve("nope").toFile()), is(empty()));
        assertThat("An empty directory is not an error", Who.querySystemdFiles(dir.toFile()), is(empty()));
    }

    @Test
    void testAFileThatIsADirectoryDoesNotAbortTheScan(@TempDir Path dir) throws IOException {
        // A numerically named subdirectory would fail to read as a properties file; the rest must still be returned.
        Files.createDirectory(dir.resolve("5"));
        session(dir, "6", "USER=grace", "REALTIME=1600000000000000");
        assertThat(users(Who.querySystemdFiles(dir.toFile())), contains("grace"));
    }

    @Test
    void testDefaultDirectoryIsAlwaysSafeToRead() {
        // The no-arg form reads /run/systemd/sessions, which is absent off Linux and populated on a Linux CI runner.
        // Asserting the contents either way would be environment-dependent, so assert only that it never throws and
        // never returns null, which is what every caller relies on.
        ThrowingSupplier<List<OSSession>> read = Who::querySystemdFiles;
        assertThat(assertDoesNotThrow(read), is(notNullValue()));
    }
}
