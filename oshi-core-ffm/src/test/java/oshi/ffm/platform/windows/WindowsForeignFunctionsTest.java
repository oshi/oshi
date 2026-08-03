/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EnabledForJreRange(min = JRE.JAVA_25)
@EnabledOnOs(OS.WINDOWS)
class WindowsForeignFunctionsTest {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsForeignFunctionsTest.class);

    @Test
    void testSucceededOrLogPassesThrough() {
        assertThat("A successful call is reported as successful",
                WindowsForeignFunctions.succeededOrLog(true, LOG, "SucceedingFunction"), is(true));
        assertThat("A failed call is reported as failed and logs GetLastError",
                WindowsForeignFunctions.succeededOrLog(false, LOG, "FailingFunction"), is(false));
    }

    @Test
    void testIsSuccess() {
        assertThat("A zero BOOL is false", WindowsForeignFunctions.isSuccess(0), is(false));
        assertThat("A nonzero BOOL is true", WindowsForeignFunctions.isSuccess(1), is(true));
    }
}
