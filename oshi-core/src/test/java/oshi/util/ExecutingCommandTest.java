/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
            ? "cmd.exe /C echo Test"
            : "echo Test";
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
