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
package oshi.driver.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.platform.win32.Guid.GUID;

import oshi.util.tuples.Quintet;

@EnabledOnOs(OS.WINDOWS)
class DeviceTreeTest {
    private static final GUID GUID_DEVINTERFACE_USB_HOST_CONTROLLER = new GUID(
            "{3ABF6F2D-71C4-462A-8A92-1E6861E6AF27}");

    @Test
    void testQueryDeviceTree() {
        Quintet<Set<Integer>, Map<Integer, Integer>, Map<Integer, String>, Map<Integer, String>, Map<Integer, String>> tree = DeviceTree
                .queryDeviceTree(GUID_DEVINTERFACE_USB_HOST_CONTROLLER);
        Set<Integer> rootSet = tree.getA();
        assertThat("Tree root set must not be empty", rootSet, is(not(empty())));
        Map<Integer, Integer> parentMap = tree.getB();
        Set<Integer> branchSet = parentMap.keySet();
        branchSet.retainAll(rootSet); // intersection
        assertThat("Branches cannot match root", branchSet, is(empty()));
        Set<Integer> nodeSet = parentMap.keySet();
        nodeSet.addAll(rootSet); // union
        assertTrue(nodeSet.containsAll(tree.getC().keySet()), "Name map should only have nodes as keys");
        assertTrue(nodeSet.containsAll(tree.getD().keySet()), "Device Id should only have nodes as keys");
        assertTrue(nodeSet.containsAll(tree.getE().keySet()), "Manufacturer map should only have nodes as keys");
    }
}
