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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test general utility methods
 */
public class UtilTest {

    @Test
    public void testSleep() {
        long now = System.currentTimeMillis();
        Util.sleep(100);
        assertTrue(System.currentTimeMillis() - now >= 100);

        now = System.currentTimeMillis();
        long then = now + 100;
        Util.sleepAfter(then, 100);
        assertTrue(System.currentTimeMillis() - now >= 200);

        now = System.currentTimeMillis();
        then = now - 550;
        Util.sleepAfter(then, 500);
        assertTrue(System.currentTimeMillis() - now < 500);
    }
}