/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_DATETIME;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_STRING;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT16;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT32;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT64;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_BSTR;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_I4;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.tuples.Pair;

/**
 * Tests the CIM type conversions in the wildcard WMI mapping.
 * <p>
 * A real query returns whichever CIM type the queried performance class happens to use, so on hardware only one of the
 * four conversions is ever reached. Stubbing the result reaches all of them, and the unsupported-type path too.
 */
class PerfCounterWildcardQueryTest {

    /** First constant names the instance; the rest are values, one per CIM type the mapping converts. */
    private enum Prop {
        NAME, U16, U32, U64, WHEN;
    }

    /** A WMI result stub: per-property CIM/VT types and one value per row. */
    private static final class StubResult implements WmiResult<Prop> {
        private final Map<Prop, Integer> cimTypes = new EnumMap<>(Prop.class);
        private final Map<Prop, Integer> vtTypes = new EnumMap<>(Prop.class);
        private final Map<Prop, List<Object>> values = new EnumMap<>(Prop.class);
        private int rows;

        private StubResult put(Prop p, int cim, int vt, Object... rowValues) {
            cimTypes.put(p, cim);
            vtTypes.put(p, vt);
            values.put(p, List.of(rowValues));
            rows = Math.max(rows, rowValues.length);
            return this;
        }

        @Override
        public int getResultCount() {
            return rows;
        }

        @Override
        public @Nullable Object getValue(Prop property, int index) {
            List<Object> v = values.get(property);
            return v == null || index >= v.size() ? null : v.get(index);
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

    // Two instances, one per supported CIM type. WMI reports UINT64 as a string and datetimes in CIM format.
    private static StubResult twoInstances() {
        return new StubResult().put(Prop.NAME, CIM_STRING, VT_BSTR, "cpu0", "cpu1")
                .put(Prop.U16, CIM_UINT16, VT_I4, 16, 17).put(Prop.U32, CIM_UINT32, VT_I4, 32, 33)
                .put(Prop.U64, CIM_UINT64, VT_BSTR, "64", "65")
                .put(Prop.WHEN, CIM_DATETIME, VT_BSTR, "20240115143000.000000+000", "20240115143001.000000+000");
    }

    private static Pair<List<String>, Map<Prop, List<Long>>> map(StubResult result) {
        return PerfCounterWildcardQuery.mapInstancesAndValuesFromResult(Prop.class, result);
    }

    @Test
    void testEachCimTypeIsConverted() {
        Map<Prop, List<Long>> values = map(twoInstances()).getB();
        assertThat("UINT16 widens to long", values.get(Prop.U16), contains(16L, 17L));
        assertThat("UINT32 widens unsigned", values.get(Prop.U32), contains(32L, 33L));
        assertThat("UINT64 is parsed from its string form", values.get(Prop.U64), contains(64L, 65L));
        assertThat("DATETIME becomes epoch milliseconds", values.get(Prop.WHEN),
                contains(1705329000000L, 1705329001000L));
    }

    @Test
    void testFirstPropertyNamesTheInstances() {
        Pair<List<String>, Map<Prop, List<Long>>> mapped = map(twoInstances());
        assertThat(mapped.getA(), contains("cpu0", "cpu1"));
        assertThat("The instance name is not also a value", mapped.getB().containsKey(Prop.NAME), is(false));
    }

    @Test
    void testUnsignedIntegersAreNotSignExtended() {
        // 0xFFFFFFFF arrives as -1 in a 32-bit slot; as an unsigned CIM_UINT32 it must read as 4294967295.
        StubResult result = new StubResult().put(Prop.NAME, CIM_STRING, VT_BSTR, "cpu0")
                .put(Prop.U16, CIM_UINT16, VT_I4, 0).put(Prop.U32, CIM_UINT32, VT_I4, -1)
                .put(Prop.U64, CIM_UINT64, VT_BSTR, "0")
                .put(Prop.WHEN, CIM_DATETIME, VT_BSTR, "20240115143000.000000+000");
        assertThat(map(result).getB().get(Prop.U32), contains(4294967295L));
    }

    @Test
    void testAnUnsupportedCimTypeIsRejected() {
        // CIM_SINT32 is a type the mapping does not convert; it must fail loudly rather than silently drop the value.
        StubResult result = new StubResult().put(Prop.NAME, CIM_STRING, VT_BSTR, "cpu0").put(Prop.U16, 3, VT_I4, 1);
        assertThrows(ClassCastException.class, () -> map(result));
    }

    @Test
    void testNoRowsYieldsNoInstancesAndNoValues() {
        Pair<List<String>, Map<Prop, List<Long>>> mapped = map(new StubResult());
        assertThat(mapped.getA(), is(empty()));
        assertThat(mapped.getB().isEmpty(), is(true));
    }
}
