/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.wmi.WmiQuery;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.util.Constants;

class WindowsComputerSystemTest {

    /** WmiQueryExecutor that always returns empty results. */
    private static final WmiQueryExecutor EMPTY_EXECUTOR = new WmiQueryExecutor() {
        @Override
        public <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query) {
            return emptyResult();
        }

        @Override
        public <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query, boolean initCom) {
            return queryWMI(query);
        }

        private <T extends Enum<T>> WmiResult<T> emptyResult() {
            return new WmiResult<T>() {
                @Override
                public int getResultCount() {
                    return 0;
                }

                @Override
                public Object getValue(T property, int index) {
                    return null;
                }

                @Override
                public int getVtType(T property) {
                    return 0;
                }

                @Override
                public int getCIMType(T property) {
                    return 0;
                }
            };
        }
    };

    private static WindowsComputerSystem createWithEmptyExecutor() {
        return new WindowsComputerSystem() {
            @Override
            protected WmiQueryExecutor getWmiQueryExecutor() {
                return EMPTY_EXECUTOR;
            }

            @Override
            public Firmware createFirmware() {
                return null;
            }

            @Override
            public Baseboard createBaseboard() {
                return null;
            }
        };
    }

    @Test
    void testFallbacksWhenWmiReturnsEmpty() {
        WindowsComputerSystem cs = createWithEmptyExecutor();
        assertThat(cs.getManufacturer(), is(Constants.UNKNOWN));
        assertThat(cs.getModel(), is(Constants.UNKNOWN));
        assertThat(cs.getSerialNumber(), is(Constants.UNKNOWN));
        assertThat(cs.getHardwareUUID(), is(Constants.UNKNOWN));
    }

    @Test
    void testQuerySerialFromBiosReturnsNullWhenEmpty() {
        WindowsComputerSystem cs = createWithEmptyExecutor();
        assertThat(cs.querySerialFromBios(), is(nullValue()));
    }
}
