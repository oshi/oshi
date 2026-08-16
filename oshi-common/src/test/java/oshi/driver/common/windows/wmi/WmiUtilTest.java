/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class WmiUtilTest {

    enum TestProp {
        NAME, SIZE, ID;
    }

    @Test
    void testQueryToString() {
        WmiQuery<TestProp> query = new WmiQuery<>("TestClass", TestProp.class);
        assertThat(WmiUtil.queryToString(query), is("SELECT NAME,SIZE,ID FROM TestClass"));
    }

    @Test
    void testQueryToStringWithNamespace() {
        WmiQuery<TestProp> query = new WmiQuery<>("ROOT\\WMI", "TestClass", TestProp.class);
        assertThat(WmiUtil.queryToString(query), is("SELECT NAME,SIZE,ID FROM TestClass"));
    }

    @Test
    void testGetStringValid() {
        WmiResult<TestProp> result = new MockWmiResult<>(WmiConstants.CIM_STRING, WmiConstants.VT_BSTR, "hello");
        assertThat(WmiUtil.getString(result, TestProp.NAME, 0), is("hello"));
    }

    @Test
    void testGetStringNull() {
        WmiResult<TestProp> result = new MockWmiResult<>(WmiConstants.CIM_STRING, WmiConstants.VT_BSTR, null);
        assertThat(WmiUtil.getString(result, TestProp.NAME, 0), is(""));
    }

    @Test
    void testGetStringWrongType() {
        WmiResult<TestProp> result = new MockWmiResult<>(WmiConstants.CIM_UINT32, WmiConstants.VT_I4, 42);
        assertThrows(ClassCastException.class, () -> WmiUtil.getString(result, TestProp.NAME, 0));
    }

    @Test
    void testGetUint32Valid() {
        WmiResult<TestProp> result = new MockWmiResult<>(WmiConstants.CIM_UINT32, WmiConstants.VT_I4, 42);
        assertThat(WmiUtil.getUint32(result, TestProp.SIZE, 0), is(42));
    }

    @Test
    void testGetUint32Null() {
        WmiResult<TestProp> result = new MockWmiResult<>(WmiConstants.CIM_UINT32, WmiConstants.VT_I4, null);
        assertThat(WmiUtil.getUint32(result, TestProp.SIZE, 0), is(0));
    }

    @Test
    void testGetUint64Valid() {
        WmiResult<TestProp> result = new MockWmiResult<>(WmiConstants.CIM_UINT64, WmiConstants.VT_BSTR, "123456789");
        assertThat(WmiUtil.getUint64(result, TestProp.SIZE, 0), is(123456789L));
    }

    @Test
    void testGetUint32asLong() {
        WmiResult<TestProp> result = new MockWmiResult<>(WmiConstants.CIM_UINT32, WmiConstants.VT_I4, -1);
        assertThat(WmiUtil.getUint32asLong(result, TestProp.SIZE, 0), is(0xFFFFFFFFL));
    }

    @Test
    void testGetBooleanValid() {
        WmiResult<TestProp> result = new MockWmiResult<>(WmiConstants.CIM_BOOLEAN, WmiConstants.VT_BOOL, true);
        assertThat(WmiUtil.getBoolean(result, TestProp.ID, 0), is(true));
    }

    @Test
    void testGetBooleanNull() {
        WmiResult<TestProp> result = new MockWmiResult<>(WmiConstants.CIM_BOOLEAN, WmiConstants.VT_BOOL, null);
        assertThat(WmiUtil.getBoolean(result, TestProp.ID, 0), is(false));
    }

    @Test
    void testGetBooleanWrongType() {
        WmiResult<TestProp> result = new MockWmiResult<>(WmiConstants.CIM_UINT32, WmiConstants.VT_I4, 42);
        assertThrows(ClassCastException.class, () -> WmiUtil.getBoolean(result, TestProp.ID, 0));
    }

    @Test
    void testGetFloat() {
        WmiResult<TestProp> result = new MockWmiResult<>(WmiConstants.CIM_REAL32, WmiConstants.VT_R4, 3.14f);
        assertThat(WmiUtil.getFloat(result, TestProp.SIZE, 0), is(3.14f));
    }

    @Test
    // The constructor's parameters are non-null and it enforces that with requireNonNull. This test exists to prove
    // the enforcement, so it has to pass the nulls the type system otherwise rules out.
    @SuppressWarnings("NullAway")
    void testWmiQueryNullValidation() {
        assertThrows(NullPointerException.class, () -> new WmiQuery<>(null, "Class", TestProp.class));
        assertThrows(NullPointerException.class, () -> new WmiQuery<>("NS", null, TestProp.class));
        assertThrows(NullPointerException.class, () -> new WmiQuery<>("NS", "Class", null));
    }

    private static class MockWmiResult<T extends Enum<T>> implements WmiResult<T> {
        private final int cimType;
        private final int vtType;
        private final @Nullable Object value;

        MockWmiResult(int cimType, int vtType, @Nullable Object value) {
            this.cimType = cimType;
            this.vtType = vtType;
            this.value = value;
        }

        @Override
        public int getResultCount() {
            return 1;
        }

        @Override
        public @Nullable Object getValue(T property, int index) {
            return value;
        }

        @Override
        public int getVtType(T property) {
            return vtType;
        }

        @Override
        public int getCIMType(T property) {
            return cimType;
        }
    }
}
