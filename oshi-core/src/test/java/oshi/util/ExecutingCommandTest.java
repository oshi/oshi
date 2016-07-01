/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

/**
 * Test command line and returning the result of execution.
 */
public class ExecutingCommandTest {

    private static final String ECHO = "echo Test";
    private static final String BAD_COMMAND = "noOSshouldHaveACommandNamedThis";

    @Test
    public void testRunNative() {
        ArrayList<String> test = ExecutingCommand.runNative(ECHO);
        assertEquals(1, test.size());
        assertEquals("Test", test.get(0));
        assertEquals("Test", ExecutingCommand.getFirstAnswer(ECHO));

        assertEquals(null, ExecutingCommand.runNative(BAD_COMMAND));
        assertEquals(null, ExecutingCommand.getFirstAnswer(BAD_COMMAND));
    }
}
