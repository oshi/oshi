/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor.LogicalProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.CentralProcessor.TickType;

class CentralProcessorTest {

    @Test
    void testLogicalProcessorThreeArg() {
        LogicalProcessor lp = new LogicalProcessor(2, 1, 0);
        assertThat(lp.getProcessorNumber(), is(2));
        assertThat(lp.getPhysicalProcessorNumber(), is(1));
        assertThat(lp.getPhysicalPackageNumber(), is(0));
        assertThat(lp.getNumaNode(), is(0));
        assertThat(lp.getProcessorGroup(), is(0));
        assertThat(lp.toString(), containsString("2"));
    }

    @Test
    void testLogicalProcessorFourArg() {
        LogicalProcessor lp = new LogicalProcessor(3, 1, 0, 1);
        assertThat(lp.getNumaNode(), is(1));
        assertThat(lp.getProcessorGroup(), is(0));
    }

    @Test
    void testLogicalProcessorFiveArg() {
        LogicalProcessor lp = new LogicalProcessor(4, 2, 1, 0, 1);
        assertThat(lp.getProcessorGroup(), is(1));
    }

    @Test
    void testPhysicalProcessorTwoArg() {
        PhysicalProcessor pp = new PhysicalProcessor(0, 1);
        assertThat(pp.getPhysicalPackageNumber(), is(0));
        assertThat(pp.getPhysicalProcessorNumber(), is(1));
        assertThat(pp.getEfficiency(), is(0));
        assertThat(pp.getIdString(), is(""));
        assertThat(pp.toString(), containsString("0/1"));
    }

    @Test
    void testPhysicalProcessorFourArg() {
        PhysicalProcessor pp = new PhysicalProcessor(1, 2, 3, "ARM Cortex-A78");
        assertThat(pp.getEfficiency(), is(3));
        assertThat(pp.getIdString(), is("ARM Cortex-A78"));
    }

    @Test
    void testProcessorCacheGetters() {
        ProcessorCache cache = new ProcessorCache(2, 8, 64, 262144, Type.UNIFIED);
        assertThat(cache.getLevel(), is((byte) 2));
        assertThat(cache.getAssociativity(), is((byte) 8));
        assertThat(cache.getLineSize(), is((short) 64));
        assertThat(cache.getCacheSize(), is(262144));
        assertThat(cache.getType(), is(Type.UNIFIED));
        assertThat(cache.toString(), containsString("L2"));
    }

    @Test
    void testProcessorCacheEqualsAndHashCode() {
        ProcessorCache a = new ProcessorCache(1, 4, 64, 32768, Type.DATA);
        ProcessorCache b = new ProcessorCache(1, 4, 64, 32768, Type.DATA);
        ProcessorCache c = new ProcessorCache(1, 4, 64, 32768, Type.INSTRUCTION);
        assertThat(a, is(b));
        assertThat(a.hashCode(), is(b.hashCode()));
        assertThat(a, is(not(c)));
        assertThat(a, is(not((Object) null)));
        assertThat(a, is(a));
    }

    @Test
    void testProcessorCacheTypeToString() {
        assertThat(Type.UNIFIED.toString(), is("Unified"));
        assertThat(Type.INSTRUCTION.toString(), is("Instruction"));
        assertThat(Type.DATA.toString(), is("Data"));
        assertThat(Type.TRACE.toString(), is("Trace"));
    }

    @Test
    void testProcessorIdentifier() {
        ProcessorIdentifier id = new ProcessorIdentifier("GenuineIntel", "Intel(R) Core(TM) i7 @ 3.60GHz", "6", "158",
                "10", "BFEBFBFF000906EA", true);
        assertThat(id.getVendor(), is("GenuineIntel"));
        assertThat(id.getName(), containsString("Intel"));
        assertThat(id.getFamily(), is("6"));
        assertThat(id.getModel(), is("158"));
        assertThat(id.getStepping(), is("10"));
        assertThat(id.getProcessorID(), is("BFEBFBFF000906EA"));
        assertThat(id.isCpu64bit(), is(true));
        assertThat(id.getVendorFreq(), is(3_600_000_000L));
        assertThat(id.getIdentifier(), containsString("Intel64"));
        assertThat(id.getMicroarchitecture(), is(notNullValue()));
        assertThat(id.toString(), containsString("Intel64"));
    }

    @Test
    void testProcessorIdentifierWithExplicitFreq() {
        ProcessorIdentifier id = new ProcessorIdentifier("AuthenticAMD", "AMD Ryzen 9", "25", "80", "0",
                "AABBCCDD11223344", true, 4_000_000_000L);
        assertThat(id.getVendorFreq(), is(4_000_000_000L));
    }

    @Test
    void testTickTypeIndex() {
        assertThat(TickType.USER.getIndex(), is(0));
        assertThat(TickType.NICE.getIndex(), is(1));
        assertThat(TickType.SYSTEM.getIndex(), is(2));
        assertThat(TickType.IDLE.getIndex(), is(3));
        assertThat(TickType.IOWAIT.getIndex(), is(4));
        assertThat(TickType.IRQ.getIndex(), is(5));
        assertThat(TickType.SOFTIRQ.getIndex(), is(6));
        assertThat(TickType.STEAL.getIndex(), is(7));
    }
}
