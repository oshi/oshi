/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Quintet;
import oshi.util.tuples.Triplet;

/**
 * Test object pair.
 */
class TupleTest {

    @Test
    void testTuples() {
        Pair<String, Integer> pair = new Pair<>("A", 1);
        Triplet<String, Integer, Long> triplet = new Triplet<>("B", 2, Long.MAX_VALUE);
        Quartet<String, Integer, Long, Character> quartet = new Quartet<>("C", 3, Long.MIN_VALUE, 'c');
        Quintet<String, Integer, Long, Character, BigInteger> quintet = new Quintet<>("D", 4, Long.valueOf("0"), 'd',
                BigInteger.ZERO);

        assertThat("pair.getA() should be A", pair.getA(), is("A"));
        assertThat("triplet.getA() should be B", triplet.getA(), is("B"));
        assertThat("quartet.getA() should be C", quartet.getA(), is("C"));
        assertThat("quintet.getA() should be D", quintet.getA(), is("D"));

        assertThat("pair.getB().intValue() should be 1", pair.getB().intValue(), is(1));
        assertThat("triplet.getB().intValue() should be 2", triplet.getB().intValue(), is(2));
        assertThat("quartet.getB().intValue() should be 3", quartet.getB().intValue(), is(3));
        assertThat("quintet.getB().intValue() should be 4", quintet.getB().intValue(), is(4));

        assertThat("triplet.getC().longValue() should be Long.MAX_VALUE", triplet.getC().longValue(),
                is(Long.MAX_VALUE));
        assertThat("quartet.getC().longValue() should be Long.MIN_VALUE", quartet.getC().longValue(),
                is(Long.MIN_VALUE));
        assertThat("quintet.getC().longValue() should be 0L", quintet.getC().longValue(), is(0L));

        assertThat("quartet.getD().charValue() should be c", quartet.getD().charValue(), is('c'));
        assertThat("quintet.getD().charValue() should be d", quintet.getD().charValue(), is('d'));

        assertThat("quintet.getE() should be BigInteger.ZERO", quintet.getE(), is(BigInteger.ZERO));
    }
}
