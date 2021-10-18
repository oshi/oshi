/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.mac;

import com.sun.jna.NativeLong;
import com.sun.jna.platform.mac.SystemB.Timeval;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import oshi.jna.platform.mac.SystemB;
import oshi.software.os.OSSession;
import oshi.util.ExecutingCommand;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static oshi.jna.platform.unix.CLibrary.LOGIN_PROCESS;
import static oshi.jna.platform.unix.CLibrary.USER_PROCESS;

class WhoTest {

    @Test
    void testQueryUtxentUserProcess() throws Exception {
        SystemB systemB = Mockito.mock(SystemB.class);
        setFinalStatic(Who.class.getDeclaredField("SYS"), systemB);

        SystemB.MacUtmpx macUtmpx = new SystemB.MacUtmpx();
        macUtmpx.ut_user = "user1".getBytes();
        macUtmpx.ut_host = "10.2.2.1".getBytes();
        macUtmpx.ut_type = USER_PROCESS;
        macUtmpx.ut_line = "console".getBytes();

        doNothing().when(systemB).setutxent();
        doNothing().when(systemB).endutxent();
        AtomicBoolean firstCall = new AtomicBoolean(true);

        when(systemB.getutxent()).thenAnswer((invocationOnMock)-> {
            if (firstCall.get()) {
                firstCall.set(false);
                return macUtmpx;
            } else {
                return null;
            }
        });

        List<OSSession> osSessions = Who.queryUtxent();
        assertThat("Size should be 1 ", osSessions.size() == 1);
        assertThat("Os Sessions should contain ", osSessions.get(0).toString().equals("user1, console, No login, (10.2.2.1)"));
    }

    @Test
    void testQueryUtxentLoginProcess() throws Exception {
        SystemB systemB = Mockito.mock(SystemB.class);
        setFinalStatic(Who.class.getDeclaredField("SYS"), systemB);

        SystemB.MacUtmpx macUtmpx = new SystemB.MacUtmpx();
        macUtmpx.ut_user = "user2".getBytes();
        macUtmpx.ut_host = "10.2.2.2".getBytes();
        macUtmpx.ut_type = LOGIN_PROCESS;
        Timeval tval= new Timeval();
        tval.tv_sec = new NativeLong(10);
        tval.tv_usec = 1000;

        macUtmpx.ut_tv = tval;
        macUtmpx.ut_line = "console".getBytes();

        doNothing().when(systemB).setutxent();
        doNothing().when(systemB).endutxent();
        AtomicBoolean firstCall = new AtomicBoolean(true);

        when(systemB.getutxent()).thenAnswer((invocationOnMock)-> {
            if (firstCall.get()) {
                firstCall.set(false);
                return macUtmpx;
            } else {
                return null;
            }
        });

        List<OSSession> osSessions = Who.queryUtxent();
        assertThat("Size should be 1 ", osSessions.size() == 1);
        assertThat("Os Sessions should contain ", osSessions.get(0).toString().equals("user2, console, 1970-01-01 05:30, (10.2.2.2)"));
    }

    @Test
    void testQueryUtxentWho() throws Exception {
        SystemB systemB = Mockito.mock(SystemB.class);
        MockedStatic<ExecutingCommand> executingCommandMockedStatic = Mockito.mockStatic(ExecutingCommand.class);
        setFinalStatic(Who.class.getDeclaredField("SYS"), systemB);

        SystemB.MacUtmpx macUtmpx = new SystemB.MacUtmpx();
        macUtmpx.ut_user = "".getBytes();
        macUtmpx.ut_type = LOGIN_PROCESS;

        doNothing().when(systemB).setutxent();
        doNothing().when(systemB).endutxent();
        AtomicBoolean firstCall = new AtomicBoolean(true);

        when(systemB.getutxent()).thenAnswer((invocationOnMock)-> {
            if (firstCall.get()) {
                firstCall.set(false);
                return macUtmpx;
            } else {
                return null;
            }
        });

        List<String> whoResult = new ArrayList<>();
        whoResult.add("user console  Sep 24 07:48");
        executingCommandMockedStatic.when(() -> ExecutingCommand.runNative("who")).thenReturn(whoResult);

        List<OSSession> osSessions = Who.queryUtxent();
        executingCommandMockedStatic.close();
        assertThat("Size should be 1 ", osSessions.size() == 1);
        assertThat("Os Sessions should contain ", osSessions.get(0).toString().equals("user, console, 2021-09-24 07:48"));
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }
}
