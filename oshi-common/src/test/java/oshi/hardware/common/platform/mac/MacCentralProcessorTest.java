/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MacCentralProcessorTest {

    @Test
    void testParseX86FeatureFlagsWithFeatures() {
        Map<String, String> strings = new HashMap<>();
        strings.put("machdep.cpu.brand_string", "Test CPU");
        strings.put("machdep.cpu.features", "FPU VME SSE SSE2 SSE3");
        strings.put("machdep.cpu.extfeatures", "LAHF EM64T");
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(strings);

        List<String> flags = cpu.parseX86FeatureFlags();
        assertThat(flags, hasSize(2));
        assertThat(flags.get(0), is("machdep.cpu.features: FPU VME SSE SSE2 SSE3"));
        assertThat(flags.get(1), is("machdep.cpu.extfeatures: LAHF EM64T"));
    }

    @Test
    void testParseX86FeatureFlagsEmpty() {
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>());
        List<String> flags = cpu.parseX86FeatureFlags();
        assertThat(flags, is(empty()));
    }

    /**
     * Minimal stub providing sysctl values without native calls.
     */
    static class StubMacCentralProcessor extends MacCentralProcessor {

        private final Map<String, Integer> sysctlInts = new HashMap<>();
        private final Map<String, Long> sysctlLongs = new HashMap<>();
        private final Map<String, String> sysctlStrings;

        StubMacCentralProcessor(Map<String, String> extraStrings) {
            Map<String, String> defaults = new HashMap<>();
            defaults.put("machdep.cpu.brand_string", "Test CPU");
            defaults.putAll(extraStrings);
            this.sysctlStrings = defaults;
            sysctlInts.put("hw.logicalcpu", 1);
            sysctlInts.put("hw.physicalcpu", 1);
            sysctlInts.put("hw.packages", 1);
            sysctlInts.put("hw.cpu64bit_capable", 1);
        }

        @Override
        protected int sysctlInt(String name, int def) {
            return sysctlInts == null ? def : sysctlInts.getOrDefault(name, def);
        }

        @Override
        protected int sysctlIntNoWarn(String name, int def) {
            return sysctlInts == null ? def : sysctlInts.getOrDefault(name, def);
        }

        @Override
        protected long sysctlLong(String name, long def) {
            return sysctlLongs == null ? def : sysctlLongs.getOrDefault(name, def);
        }

        @Override
        protected String sysctlString(String name, String def) {
            return sysctlStrings == null ? def : sysctlStrings.getOrDefault(name, def);
        }

        @Override
        protected String sysctlStringNoWarn(String name, String def) {
            return sysctlStrings == null ? def : sysctlStrings.getOrDefault(name, def);
        }

        @Override
        protected String platformExpert() {
            return "Apple";
        }

        @Override
        protected Map<Integer, String> queryCompatibleStrings() {
            return new HashMap<>();
        }

        @Override
        protected void calculateNominalFrequencies() {
        }

        @Override
        protected long[] querySystemCpuLoadTicks() {
            return new long[8];
        }

        @Override
        protected long[][] queryProcessorCpuLoadTicks() {
            return new long[1][8];
        }

        @Override
        public long queryMaxFreq() {
            return 0L;
        }

        @Override
        public long[] queryCurrentFreq() {
            return new long[] { 0L };
        }

        @Override
        public long queryContextSwitches() {
            return 0L;
        }

        @Override
        public long queryInterrupts() {
            return 0L;
        }

        @Override
        public double[] getSystemLoadAverage(int nelem) {
            return new double[nelem];
        }
    }
}
