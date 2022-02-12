/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.HWPartition;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

@EnabledOnOs(OS.AIX)
class LsDevicesTest {
    @Test
    void testQueryLsDevices() {
        Map<String, Pair<Integer, Integer>> majMinMap = Ls.queryDeviceMajorMinor();
        assertThat("Device Major Minor Map shouldn't be empty", majMinMap, not(anEmptyMap()));
        // Key set is device name
        boolean foundNonNull = false;
        boolean foundPartitions = false;
        for (String device : majMinMap.keySet()) {
            Pair<String, String> modSer = Lscfg.queryModelSerial(device);
            if (modSer.getA() != null || modSer.getB() != null) {
                foundNonNull = true;
                List<HWPartition> lvs = Lspv.queryLogicalVolumes(device, majMinMap);
                if (!lvs.isEmpty()) {
                    foundPartitions = true;
                }
            }
            if (foundNonNull && foundPartitions) {
                break;
            }
        }
        assertTrue(foundNonNull, "At least one device model/serial should be non null");
        assertTrue(foundPartitions, "At least one device should have partitions");

        List<String> lscfg = Lscfg.queryAllDevices();
        assertThat("Output of lscfg should be nonempty", lscfg.size(), greaterThan(0));

        Triplet<String, String, String> modSerVer = Lscfg.queryBackplaneModelSerialVersion(lscfg);
        // Either all should be null or none should be null
        if (!(modSerVer.getA() == null && modSerVer.getB() == null && modSerVer.getC() == null)) {
            assertNotNull(modSerVer.getA(), "Backplane Model should not be null");
            assertNotNull(modSerVer.getB(), "Backplane Serial should not be null");
            assertNotNull(modSerVer.getC(), "Backplane Version should not be null");
        }
    }
}
