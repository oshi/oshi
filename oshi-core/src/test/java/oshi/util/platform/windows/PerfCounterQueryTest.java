/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_DATETIME;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT16;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT32;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT64;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_BSTR;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_I4;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.driver.common.windows.wmi.WmiResult;

/**
 * Tests the CIM type conversions in the WMI value mapping.
 * <p>
 * A real query returns whichever CIM type the queried performance class happens to use, so on hardware only one of the
 * four conversions is ever reached. Stubbing the result reaches all of them, and the unsupported-type path too.
 * <p>
 * Windows only, unlike the wildcard equivalent: {@link PerfCounterQuery} caches
 * {@code VersionHelpers.IsWindowsVistaOrGreater()} in a static field, so the class cannot be loaded on another OS.
 */
@EnabledOnOs(OS.WINDOWS)
class PerfCounterQueryTest {

    private enum Prop {
        U16, U32, U64, WHEN;
    }

    /** A single-row WMI result stub. */
    private static final class StubResult implements WmiResult<Prop> {
        private final Map<Prop, Integer> cimTypes = new EnumMap<>(Prop.class);
        private final Map<Prop, Integer> vtTypes = new EnumMap<>(Prop.class);
        private final Map<Prop, Object> values = new EnumMap<>(Prop.class);
        private int rows;

        private StubResult put(Prop p, int cim, int vt, Object value) {
            cimTypes.put(p, cim);
            vtTypes.put(p, vt);
            values.put(p, value);
            rows = 1;
            return this;
        }

        @Override
        public int getResultCount() {
            return rows;
        }

        @Override
        public @Nullable Object getValue(Prop property, int index) {
            return values.get(property);
        }

        @Override
        public int getVtType(Prop property) {
            return vtTypes.getOrDefault(property, 0);
        }

        @Override
        public int getCIMType(Prop property) {
            return cimTypes.getOrDefault(property, 0);
        }
    }

    // One value per supported CIM type. WMI reports UINT64 as a string and datetimes in CIM format.
    private static StubResult oneRow() {
        return new StubResult().put(Prop.U16, CIM_UINT16, VT_I4, 16).put(Prop.U32, CIM_UINT32, VT_I4, 32)
                .put(Prop.U64, CIM_UINT64, VT_BSTR, "64")
                .put(Prop.WHEN, CIM_DATETIME, VT_BSTR, "20240115143000.000000+000");
    }

    @Test
    void testEachCimTypeIsConverted() {
        Map<Prop, Long> values = PerfCounterQuery.mapValuesFromResult(Prop.class, oneRow());
        assertThat("UINT16 widens to long", values.get(Prop.U16), is(16L));
        assertThat("UINT32 widens unsigned", values.get(Prop.U32), is(32L));
        assertThat("UINT64 is parsed from its string form", values.get(Prop.U64), is(64L));
        assertThat("DATETIME becomes epoch milliseconds", values.get(Prop.WHEN), is(1705329000000L));
    }

    @Test
    void testUnsignedIntegersAreNotSignExtended() {
        // 0xFFFFFFFF arrives as -1 in a 32-bit slot; as an unsigned CIM_UINT32 it must read as 4294967295.
        StubResult result = new StubResult().put(Prop.U16, CIM_UINT16, VT_I4, 0).put(Prop.U32, CIM_UINT32, VT_I4, -1)
                .put(Prop.U64, CIM_UINT64, VT_BSTR, "0")
                .put(Prop.WHEN, CIM_DATETIME, VT_BSTR, "20240115143000.000000+000");
        assertThat(PerfCounterQuery.mapValuesFromResult(Prop.class, result).get(Prop.U32), is(4294967295L));
    }

    @Test
    void testAnUnsupportedCimTypeIsRejected() {
        // CIM_SINT32 is a type the mapping does not convert; it must fail loudly rather than silently drop the value.
        StubResult result = new StubResult().put(Prop.U16, 3, VT_I4, 1);
        assertThrows(ClassCastException.class, () -> PerfCounterQuery.mapValuesFromResult(Prop.class, result));
    }

    @Test
    void testNoRowsYieldsAnEmptyMap() {
        assertThat(PerfCounterQuery.mapValuesFromResult(Prop.class, new StubResult()).isEmpty(), is(true));
    }

    /**
     * The properties of {@code Win32_OperatingSystem} that this mapping converts, one per supported CIM type. Chosen
     * because a single real class covers all four, so no string property is present to hit the unsupported-type throw.
     */
    private enum OsProp {
        OSTYPE, NUMBEROFPROCESSES, FREEPHYSICALMEMORY, LASTBOOTUPTIME;
    }

    /**
     * Runs the real query, rather than a stub, against a class that genuinely holds all four types.
     * <p>
     * The stubbed tests above encode assumptions about how WMI represents each type -- that a UINT64 arrives as a
     * string in a BSTR, that a DATETIME arrives in CIM format. The production code assumes the same, so those tests and
     * the code could agree with each other and both be wrong. This one takes the assumption out of the loop.
     */
    @Test
    void testAgainstRealWmiDataCoveringEveryType() {
        Map<OsProp, Long> values = PerfCounterQuery.queryValuesFromWMI(OsProp.class, "Win32_OperatingSystem");
        assertThat("Win32_OperatingSystem should always return a row", values.keySet(),
                containsInAnyOrder(OsProp.values()));
        assertThat("OSType is a small enumeration value", values.get(OsProp.OSTYPE), is(greaterThan(0L)));
        assertThat("A running system has processes", values.get(OsProp.NUMBEROFPROCESSES), is(greaterThan(0L)));
        assertThat("Free memory is reported in kilobytes", values.get(OsProp.FREEPHYSICALMEMORY), is(greaterThan(0L)));
        Long boot = values.get(OsProp.LASTBOOTUPTIME);
        assertNotNull(boot);
        assertThat("Boot time is a plausible epoch millisecond value in the past", boot,
                is(both(greaterThan(946684800000L)).and(lessThanOrEqualTo(System.currentTimeMillis()))));
    }
}
