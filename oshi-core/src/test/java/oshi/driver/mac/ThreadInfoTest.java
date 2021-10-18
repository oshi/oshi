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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import oshi.software.os.OSProcess;
import oshi.util.ExecutingCommand;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

class ThreadInfoTest {

    MockedStatic<ExecutingCommand> executingCommandMockedStatic;

    @BeforeEach
    void setup() {
        executingCommandMockedStatic = Mockito.mockStatic(ExecutingCommand.class);
        List<String> processStatus = new ArrayList<>();
        processStatus.add("sample-user 1 p5 0.0 S 31T 0:00.14 0:00.03 /sample1");
        processStatus.add("sample-user 2    0.0 R 34T 0:00.14 0:01.03 /sample2");
        processStatus.add("sample-user 3    0.0 U 17T 0:00.16 0:02.43 /sample3");
        processStatus.add("sample-user 4    0.0 Z 21T 0:00.17 0:03.03 /sample4");
        processStatus.add("sample-user 5    0.0 T 28T 0:00.18 0:00.33 /sample5");
        processStatus.add("sample-user 6    0.0 I 38T 0:01.14 0:02.03 /sample6");

        executingCommandMockedStatic.when(() -> ExecutingCommand.runNative("ps -awwxM"))
            .thenReturn(processStatus);
    }

    @Test
    void testQueryTaskThreadsSleeping() {
        List<ThreadInfo.ThreadStats> threadStats = ThreadInfo.queryTaskThreads(1);

        assertThat("List size should be 1", threadStats.size() == 1L);
        assertThat("Priority should be 31", threadStats.get(0).getPriority() == 31);
        assertThat("System time should be 140", threadStats.get(0).getSystemTime() == 140);
        assertThat("Up time should be 340000", threadStats.get(0).getUpTime() == 340000);
        assertThat("User time should be 30", threadStats.get(0).getUserTime() == 30);
        assertThat("State should be SLEEPING", threadStats.get(0).getState() == OSProcess.State.SLEEPING);
    }

    @Test
    void testQueryTaskThreadsRunning() {
        List<ThreadInfo.ThreadStats> threadStats = ThreadInfo.queryTaskThreads(2);

        assertThat("List size should be 1", threadStats.size() == 1L);
        assertThat("Priority should be 34", threadStats.get(0).getPriority() == 34);
        assertThat("System time should be 140", threadStats.get(0).getSystemTime() == 140);
        assertThat("Up time should be 2340000", threadStats.get(0).getUpTime() == 2340000);
        assertThat("User time should be 1030", threadStats.get(0).getUserTime() == 1030);
        assertThat("State should be RUNNING", threadStats.get(0).getState() == OSProcess.State.RUNNING);
    }

    @Test
    void testQueryTaskThreadsWaiting() {
        List<ThreadInfo.ThreadStats> threadStats = ThreadInfo.queryTaskThreads(3);

        assertThat("List size should be 1", threadStats.size() == 1L);
        assertThat("Priority should be 17", threadStats.get(0).getPriority() == 17);
        assertThat("System time should be 160", threadStats.get(0).getSystemTime() == 160);
        assertThat("Up time should be 5180000", threadStats.get(0).getUpTime() == 5180000);
        assertThat("User time should be 2430", threadStats.get(0).getUserTime() == 2430);
        assertThat("State should be WAITING", threadStats.get(0).getState() == OSProcess.State.WAITING);
    }

    @Test
    void testQueryTaskThreadsZombie() {
        List<ThreadInfo.ThreadStats> threadStats = ThreadInfo.queryTaskThreads(4);

        assertThat("List size should be 1", threadStats.size() == 1L);
        assertThat("Priority should be 21", threadStats.get(0).getPriority() == 21);
        assertThat("System time should be 170", threadStats.get(0).getSystemTime() == 170);
        assertThat("Up time should be 6400000", threadStats.get(0).getUpTime() == 6400000);
        assertThat("User time should be 3030", threadStats.get(0).getUserTime() == 3030);
        assertThat("State should be ZOMBIE", threadStats.get(0).getState() == OSProcess.State.ZOMBIE);
    }

    @Test
    void testQueryTaskThreadsTerminated() {
        List<ThreadInfo.ThreadStats> threadStats = ThreadInfo.queryTaskThreads(5);

        assertThat("List size should be 1", threadStats.size() == 1L);
        assertThat("Priority should be 28", threadStats.get(0).getPriority() == 28);
        assertThat("System time should be 180", threadStats.get(0).getSystemTime() == 180);
        assertThat("Up time should be 1020000", threadStats.get(0).getUpTime() == 1020000);
        assertThat("User time should be 330", threadStats.get(0).getUserTime() == 330);
        assertThat("State should be STOPPED", threadStats.get(0).getState() == OSProcess.State.STOPPED);
    }

    @Test
    void testQueryTaskThreadsIdle() {
        List<ThreadInfo.ThreadStats> threadStats = ThreadInfo.queryTaskThreads(6);

        assertThat("List size should be 1", threadStats.size() == 1L);
        assertThat("Priority should be 38", threadStats.get(0).getPriority() == 38);
        assertThat("System time should be 1140", threadStats.get(0).getSystemTime() == 1140);
        assertThat("Up time should be 6340000", threadStats.get(0).getUpTime() == 6340000);
        assertThat("User time should be 2030", threadStats.get(0).getUserTime() == 2030);
        assertThat("State should be SLEEPING", threadStats.get(0).getState() == OSProcess.State.SLEEPING);
    }

    @AfterEach
    void tearDown() {
        executingCommandMockedStatic.close();
    }
}
