/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import oshi.PlatformEnum;
import oshi.SystemInfo;

/**
 * Test command line and returning the result of execution.
 */
public class ExecutingCommandTest {

    private static final String ECHO = SystemInfo.getCurrentPlatformEnum().equals(PlatformEnum.WINDOWS)
            ? "cmd.exe /C echo Test" : "echo Test";
    private static final String BAD_COMMAND = "noOSshouldHaveACommandNamedThis";

    @Test
    public void testRunNative() {
        List<String> test = ExecutingCommand.runNative(ECHO);
        assertEquals(1, test.size());
        assertEquals("Test", test.get(0));
        assertEquals("Test", ExecutingCommand.getFirstAnswer(ECHO));

        assertTrue(ExecutingCommand.runNative(BAD_COMMAND).isEmpty());
        assertTrue(ExecutingCommand.getFirstAnswer(BAD_COMMAND).isEmpty());
    }
}
