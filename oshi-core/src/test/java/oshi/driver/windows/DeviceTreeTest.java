/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.platform.win32.Guid.GUID;

import oshi.util.tuples.Quintet;

@EnabledOnOs(OS.WINDOWS)
class DeviceTreeTest {
    private static final GUID GUID_DEVINTERFACE_NET = new GUID("{CAC88484-7515-4C03-82E6-71A87ABAC361}");

    @Test
    void testQueryDeviceTree() {
        Quintet<Set<Integer>, Map<Integer, Integer>, Map<Integer, String>, Map<Integer, String>, Map<Integer, String>> tree = DeviceTree
                .queryDeviceTree(GUID_DEVINTERFACE_NET);
        Set<Integer> rootSet = tree.getA();
        assertThat("Tree root set must not be empty", rootSet, is(not(empty())));
        Map<Integer, Integer> parentMap = tree.getB();
        Set<Integer> branchSet = parentMap.keySet().stream().collect(Collectors.toSet());
        branchSet.retainAll(rootSet); // intersection
        assertThat("Branches cannot match root", branchSet, is(empty()));
        Set<Integer> nodeSet = parentMap.keySet().stream().collect(Collectors.toSet());
        nodeSet.addAll(rootSet); // union

        assertTrue(nodeSet.containsAll(tree.getC().keySet()), "Name map should only have nodes as keys");
        assertTrue(nodeSet.containsAll(tree.getD().keySet()), "Device Id should only have nodes as keys");
        assertTrue(nodeSet.containsAll(tree.getE().keySet()), "Manufacturer map should only have nodes as keys");
    }
}
