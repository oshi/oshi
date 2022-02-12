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
package oshi.driver.unix.openbsd.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.Platform; // NOSONAR squid:S1191

import oshi.hardware.HWPartition;
import oshi.software.os.unix.openbsd.OpenBsdOperatingSystem;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;
import oshi.util.tuples.Quartet;

@EnabledOnOs(OS.OTHER)
class DisklabelTest {
    @Test
    void testDisklabel() {
        if (Platform.isOpenBSD()) {
            String[] devices = OpenBsdSysctlUtil.sysctl("hw.disknames", "").split(",");
            for (String device : devices) {
                String diskName = device.split(":")[0];
                Quartet<String, String, Long, List<HWPartition>> diskdata = Disklabel.getDiskParams(diskName);
                // First 3 only available with elevation
                if (new OpenBsdOperatingSystem().isElevated()) {
                    assertThat("Disk label is not null", diskdata.getA(), not(nullValue()));
                    assertThat("Disk duid is not null", diskdata.getB(), not(nullValue()));
                    assertThat("Disk size is nonnegative", diskdata.getC().longValue(), greaterThanOrEqualTo(0L));
                    for (HWPartition part : diskdata.getD()) {
                        assertTrue(part.getIdentification().startsWith(diskName), "Partition ID starts with disk");
                    }
                }
            }
        }
    }
}
