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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(nargs & 0xff);
        out.write(nargs >> 8 & 0xff);
        out.write(nargs >> 16 & 0xff);
        out.write(nargs >> 24 & 0xff);
        writeCString(out, EXEC);
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
    void testNullPaddingBetweenEntries() {
        byte[] buf = procargs(2, EXEC, "arg1", "", "", "FOO=bar");
        Pair<List<String>, Map<String, String>> result = MacOSProcess.parseProcArgs(buf, buf.length);
        assertThat("arguments", result.getA(), contains(EXEC, "arg1"));
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

        byte[] tooManyArgs = procargs(1025, "ignored");
        assertThat("oversized nargs", MacOSProcess.parseProcArgs(tooManyArgs, tooManyArgs.length).getA(), is(empty()));

        byte[] negativeArgs = procargs(-1, "ignored");
        assertThat("negative nargs", MacOSProcess.parseProcArgs(negativeArgs, negativeArgs.length).getA(), is(empty()));
    }
}
