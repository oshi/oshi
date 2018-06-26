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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Test String utility methods
 */
public class StringUtilTest {

    @Test
    public void testJoin() {
        String[] empty = new String[0];
        assertEquals("", StringUtil.join(",", empty));

        String[] single = new String[] { "foo" };
        assertEquals("foo", StringUtil.join(",", single));

        String[] multiple = new String[] { "foo", "bar" };
        assertEquals("foo,bar", StringUtil.join(",", multiple));

        List<String> list = Arrays.asList(multiple);
        assertEquals("foo,bar", StringUtil.join(",", list));
    }
}