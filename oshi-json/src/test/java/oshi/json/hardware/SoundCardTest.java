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
package oshi.json.hardware;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import oshi.json.SystemInfo;

/**
 * Test SoundCard
 *
 * @author BilalAM
 */
public class SoundCardTest {

    /**
     * Testing sound cards , each attribute.
     */
    @Test
    public void testSoundCards() {
        SystemInfo info = new SystemInfo();
        for (SoundCard soundCard : info.getHardware().getSoundCards()) {
            assertNotNull(soundCard.getCodec());
            assertNotNull(soundCard.getDriverVersion());
            assertNotNull(soundCard.getName());
        }
    }

}
