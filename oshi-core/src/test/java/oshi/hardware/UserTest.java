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
package oshi.hardware;

import org.junit.Assert;
import org.junit.Test;
import oshi.SystemInfo;

/**
 * Test User class
 *
 * @author : BilalAM
 */
public class UserTest {

        @Test
        public void testUser(){
                SystemInfo info = new SystemInfo();
                User user = info.getHardware().getUser();
                Assert.assertNotNull(user.getHomeDir());
                Assert.assertNotNull(user.getRealName());
                Assert.assertNotNull(user.getUserId());
                Assert.assertNotNull(user.getUserName());
        }
}
