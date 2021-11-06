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
package oshi.driver.mac.disk;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.sun.jna.Platform;
import com.sun.jna.platform.mac.SystemB;

class FsstatTest {

    @Test
    void testQueryFsstat() {
        if (Platform.isMac()) {
            int mockNumfs = 5;
            try (MockedStatic<Fsstat> fsstatMock = Mockito.mockStatic(Fsstat.class)) {
                fsstatMock.when(() -> {
                    Fsstat.queryFsstat(null, 0, 0);
                }).thenReturn(mockNumfs);

                Assert.assertEquals(Fsstat.queryFsstat(null, 0, 0), mockNumfs);
            }
        }
    }

    @Test
    void testGetFileSystems() {
        if (Platform.isMac()) {
            int mockNumfs = 5;
            SystemB.Statfs[] mockFileSystems = new SystemB.Statfs[mockNumfs];
            try (MockedStatic<Fsstat> fsstatMock = Mockito.mockStatic(Fsstat.class)) {
                fsstatMock.when(() -> {
                    Fsstat.getFileSystems(mockNumfs);
                }).thenReturn(mockFileSystems);

                Assert.assertEquals(Fsstat.getFileSystems(mockNumfs)[0], mockFileSystems[0]);
            }
        }
    }

    @Test
    void testQueryPartitionToMountMap() {
        if (Platform.isMac()) {
            int mockNumfs = 1;
            SystemB.Statfs[] mockFileSystems = new SystemB.Statfs[mockNumfs];
            SystemB.Statfs fs = new SystemB.Statfs();
            fs.f_mntfromname = "/test/dev/".getBytes();
            fs.f_mntonname = "/hello".getBytes();
            mockFileSystems[0] = fs;

            Map<String, String> expectedMountPointMap = new HashMap<>();
            expectedMountPointMap.put("/test", "/hello");

            try (MockedStatic<Fsstat> fsstatMock = Mockito.mockStatic(Fsstat.class, Mockito.CALLS_REAL_METHODS)) {
                fsstatMock.when(() -> {
                    Fsstat.queryFsstat(null, 0, 0);
                }).thenReturn(mockNumfs);
                fsstatMock.when(() -> {
                    Fsstat.getFileSystems(mockNumfs);
                }).thenReturn(mockFileSystems);

                Assert.assertEquals(Fsstat.queryFsstat(null, 0, 0), mockNumfs);
                Assert.assertEquals(Fsstat.getFileSystems(mockNumfs)[0], mockFileSystems[0]);

                Assert.assertEquals(Fsstat.queryPartitionToMountMap(), expectedMountPointMap);
            }
        }
    }

}
