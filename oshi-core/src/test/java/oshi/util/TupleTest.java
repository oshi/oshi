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

import java.math.BigInteger;

import org.junit.Test;

import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Quintet;
import oshi.util.tuples.Triplet;

/**
 * Test object pair.
 */
public class TupleTest {

    @Test
    public void testTuples() {
        Pair<String, Integer> pair = new Pair<>("A", 1);
        Triplet<String, Integer, Long> triplet = new Triplet<>("B", 2, Long.MAX_VALUE);
        Quartet<String, Integer, Long, Character> quartet = new Quartet<>("C", 3, Long.MIN_VALUE, 'c');
        Quintet<String, Integer, Long, Character, BigInteger> quintet = new Quintet<>("D", 4, Long.valueOf("0"), 'd',
                BigInteger.ZERO);

        assertEquals("pair.getA() should be A", "A", pair.getA());
        assertEquals("triplet.getA() should be B", "B", triplet.getA());
        assertEquals("quartet.getA() should be C", "C", quartet.getA());
        assertEquals("quintet.getA() should be D", "D", quintet.getA());

        assertEquals("pair.getB().intValue() should be 1", 1, pair.getB().intValue());
        assertEquals("triplet.getB().intValue() should be 2", 2, triplet.getB().intValue());
        assertEquals("quartet.getB().intValue() should be 3", 3, quartet.getB().intValue());
        assertEquals("quintet.getB().intValue() should be 4", 4, quintet.getB().intValue());

        assertEquals("triplet.getC().longValue() should be Long.MAX_VALUE", Long.MAX_VALUE, triplet.getC().longValue());
        assertEquals("quartet.getC().longValue() should be Long.MIN_VALUE", Long.MIN_VALUE, quartet.getC().longValue());
        assertEquals("quintet.getC().longValue() should be 0L", 0L, quintet.getC().longValue());

        assertEquals("quartet.getD().charValue() should be c", 'c', quartet.getD().charValue());
        assertEquals("quintet.getD().charValue() should be d", 'd', quintet.getD().charValue());

        assertEquals("quintet.getE() should be BigInteger.ZERO", BigInteger.ZERO, quintet.getE());
    }
}
