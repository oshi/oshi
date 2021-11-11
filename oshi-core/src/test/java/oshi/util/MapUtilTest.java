/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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
        Map<String, String> testMap = new HashMap<String, String>();

        testMap.put("a", "A");
        assertEquals("A", MapUtil.getOrDefault(testMap, "a", "Z"));
        assertEquals("Z", MapUtil.getOrDefault(testMap, "b", "Z"));

        assertEquals("A", MapUtil.putIfAbsent(testMap, "a", "Z"));
        assertEquals("A", testMap.get("a"));
        assertEquals(null, MapUtil.putIfAbsent(testMap, "b", "B"));
        assertEquals("B", testMap.get("b"));

        Map<String, List<String>> testListMap = new HashMap<String, List<String>>();
        assertEquals(0, MapUtil.createNewListIfAbsent(testListMap, "a").size());
        testListMap.get("a").add("foo");
        assertEquals(1, MapUtil.createNewListIfAbsent(testListMap, "a").size());
        assertEquals("foo", MapUtil.createNewListIfAbsent(testListMap, "a").get(0));
    }
}