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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Test map methods
 */
public class MapUtilTest {

    @Test
    public void testGetPutAndCompute() {
        Map<String, String> testMap = new HashMap<>();

        testMap.put("a", "A");
        assertEquals("A", MapUtil.getOrDefault(testMap, "a", "Z"));
        assertEquals("Z", MapUtil.getOrDefault(testMap, "b", "Z"));

        assertEquals("A", MapUtil.putIfAbsent(testMap, "a", "Z"));
        assertEquals("A", testMap.get("a"));
        assertEquals(null, MapUtil.putIfAbsent(testMap, "b", "B"));
        assertEquals("B", testMap.get("b"));

        Map<String, List<String>> testListMap = new HashMap<>();
        assertEquals(0, MapUtil.createNewListIfAbsent(testListMap, "a").size());
        testListMap.get("a").add("foo");
        assertEquals(1, MapUtil.createNewListIfAbsent(testListMap, "a").size());
        assertEquals("foo", MapUtil.createNewListIfAbsent(testListMap, "a").get(0));
    }
}