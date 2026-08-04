/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Pair;

/**
 * Tests the {@code KERN_PROCARGS2} buffer parser.
 */
class MacOSProcessTest {

    private static final String EXEC = "/usr/bin/foo";

    /**
     * Builds a {@code KERN_PROCARGS2} buffer: a little-endian argument count, the null-terminated exec path, then the
     * null-terminated entries.
     */
    private static byte[] procargs(int nargs, String... entries) {
        return procargsPadded(nargs, 0, entries);
    }

    /**
     * As {@link #procargs}, with {@code padding} extra null bytes between the exec path and the first entry, as the
     * kernel emits for alignment.
     */
    private static byte[] procargsPadded(int nargs, int padding, String... entries) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(nargs & 0xff);
        out.write(nargs >> 8 & 0xff);
        out.write(nargs >> 16 & 0xff);
        out.write(nargs >> 24 & 0xff);
        writeCString(out, EXEC);
        for (int i = 0; i < padding; i++) {
            out.write(0);
        }
        for (String entry : entries) {
            writeCString(out, entry);
        }
        return out.toByteArray();
    }

    private static void writeCString(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.write(bytes, 0, bytes.length);
        out.write(0);
    }

    @Test
    void testAsciiArgsAndEnv() {
        byte[] buf = procargs(3, EXEC, "-c", "value", "HOME=/Users/me", "LANG=en_US.UTF-8");
        Pair<List<String>, Map<String, String>> result = MacOSProcess.parseProcArgs(buf, buf.length);
        assertThat("arguments", result.getA(), contains(EXEC, "-c", "value"));
        assertThat("HOME", result.getB(), hasEntry("HOME", "/Users/me"));
        assertThat("LANG", result.getB(), hasEntry("LANG", "en_US.UTF-8"));
    }

    /**
     * The regression this parser exists for: an entry containing multi-byte characters is longer in bytes than in
     * chars, so advancing the offset by the decoded String's length lands mid-character and corrupts every entry after
     * it.
     */
    @Test
    void testMultiByteArgsDoNotShiftLaterEntries() {
        byte[] buf = procargs(4, EXEC, "日本語", "café", "plain-ascii-tail", "TZ=Asia/Tokyo", "GREET=こんにちは");
        Pair<List<String>, Map<String, String>> result = MacOSProcess.parseProcArgs(buf, buf.length);
        assertThat("arguments", result.getA(), contains(EXEC, "日本語", "café", "plain-ascii-tail"));
        assertThat("TZ", result.getB(), hasEntry("TZ", "Asia/Tokyo"));
        assertThat("multi-byte value", result.getB(), hasEntry("GREET", "こんにちは"));
    }

    @Test
    void testNullPaddingBetweenSections() {
        byte[] buf = procargsPadded(2, 3, EXEC, "arg1", "", "", "FOO=bar");
        Pair<List<String>, Map<String, String>> result = MacOSProcess.parseProcArgs(buf, buf.length);
        assertThat("arguments", result.getA(), contains(EXEC, "arg1"));
        assertThat("FOO", result.getB(), hasEntry("FOO", "bar"));
    }

    /**
     * An empty string is a legal argv entry. Treating its terminator as padding would both drop it and shift an
     * environment entry into the argument list.
     */
    @Test
    void testEmptyArgumentIsPreserved() {
        byte[] buf = procargsPadded(4, 2, EXEC, "first", "", "third", "FOO=bar");
        Pair<List<String>, Map<String, String>> result = MacOSProcess.parseProcArgs(buf, buf.length);
        assertThat("arguments", result.getA(), contains(EXEC, "first", "", "third"));
        assertThat("environment", result.getB(), is(aMapWithSize(1)));
        assertThat("FOO", result.getB(), hasEntry("FOO", "bar"));
    }

    /**
     * Processes with several thousand arguments are ordinary (a shell glob or a compiler invocation), so the argument
     * count is bounded only by the buffer that has to hold them.
     */
    @Test
    void testManyArguments() {
        String[] entries = new String[2000];
        entries[0] = EXEC;
        for (int i = 1; i < 1999; i++) {
            entries[i] = "arg" + i;
        }
        entries[1999] = "FOO=bar";
        byte[] buf = procargs(1999, entries);
        Pair<List<String>, Map<String, String>> result = MacOSProcess.parseProcArgs(buf, buf.length);
        assertThat("argument count", result.getA(), hasSize(1999));
        assertThat("first argument", result.getA().get(0), is(EXEC));
        assertThat("last argument", result.getA().get(1998), is("arg1998"));
        assertThat("FOO", result.getB(), hasEntry("FOO", "bar"));
    }

    @Test
    void testEntryWithoutEqualsIsNotEnvironment() {
        byte[] buf = procargs(1, EXEC, "=novalue", "notanassignment", "OK=yes");
        Pair<List<String>, Map<String, String>> result = MacOSProcess.parseProcArgs(buf, buf.length);
        assertThat("arguments", result.getA(), contains(EXEC));
        assertThat("environment", result.getB(), is(aMapWithSize(1)));
        assertThat("environment", result.getB(), hasEntry("OK", "yes"));
    }

    @Test
    void testSizeLimitsParsing() {
        byte[] buf = procargs(3, EXEC, "kept", "cut");
        // The sysctl-reported size bounds the parse: entries past it are not read even though the array holds them
        Pair<List<String>, Map<String, String>> result = MacOSProcess.parseProcArgs(buf, buf.length - 4);
        assertThat("arguments", result.getA(), contains(EXEC, "kept"));
    }

    @Test
    void testUnusableBuffers() {
        byte[] tooShort = { 1, 0, 0 };
        assertThat("short buffer args", MacOSProcess.parseProcArgs(tooShort, tooShort.length).getA(), is(empty()));

        byte[] zeroArgs = procargs(0, "ignored");
        assertThat("zero nargs args", MacOSProcess.parseProcArgs(zeroArgs, zeroArgs.length).getA(), is(empty()));
        assertThat("zero nargs env", MacOSProcess.parseProcArgs(zeroArgs, zeroArgs.length).getB(), is(anEmptyMap()));

        // More arguments than the remaining bytes could hold, even at one null terminator each
        byte[] impossibleArgs = procargs(1025, "ignored");
        assertThat("impossible nargs", MacOSProcess.parseProcArgs(impossibleArgs, impossibleArgs.length).getA(),
                is(empty()));

        byte[] negativeArgs = procargs(-1, "ignored");
        assertThat("negative nargs", MacOSProcess.parseProcArgs(negativeArgs, negativeArgs.length).getA(), is(empty()));
    }
}
