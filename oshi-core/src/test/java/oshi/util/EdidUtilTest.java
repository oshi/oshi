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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/*
 * Tests EdidUtil
 */
public class EdidUtilTest {

    private final static String EDID_HEADER = "00FFFFFFFFFFFF00";
    private final static String EDID_MANUFID = "0610";
    private final static String EDID_PRODCODE = "2792";
    private final static String EDID_SERIAL = "250C2C16";
    private final static String EDID_WKYR = "2C16";
    private final static String EDID_VERSION = "0104";
    private final static String EDID_VIDEO = "B53C2278226FB1A7554C9E250C5054000000";
    private final static String EDID_TIMING = "01010101010101010101010101010101";
    private final static String EDID_DESC1 = "565E00A0A0A029503020350055502100001A";
    private final static String EDID_DESC2 = "1A1D008051D01C204080350055502100001C";
    private final static String EDID_DESC3 = "000000FF004330324A4D325046463247430A";
    private final static String EDID_DESC4 = "000000FC005468756E646572626F6C740A20";
    private final static String EDID_DESC5 = "000000FA004330324A4D325046463247430A";
    private final static String EDID_DESC6 = "000000FB005468756E646572626F6C740A20";
    private final static String EDID_DESC7 = "000000FD004330324A4D325046463247430A";
    private final static String EDID_DESC8 = "000000FE005468756E646572626F6C740A20";
    private final static String EDID_EXTS = "01";
    private final static String EDID_CHKSUM = "C7";
    private final static String EDID_STR = EDID_HEADER + EDID_MANUFID + EDID_PRODCODE + EDID_SERIAL + EDID_WKYR
            + EDID_VERSION + EDID_VIDEO + EDID_TIMING + EDID_DESC1 + EDID_DESC2 + EDID_DESC3 + EDID_DESC4 + EDID_EXTS
            + EDID_CHKSUM;
    private final static String EDID_STR2 = EDID_HEADER + EDID_MANUFID + EDID_PRODCODE + EDID_SERIAL + EDID_WKYR
            + EDID_VERSION + EDID_VIDEO + EDID_TIMING + EDID_DESC5 + EDID_DESC6 + EDID_DESC7 + EDID_DESC8 + EDID_EXTS
            + EDID_CHKSUM;
    private final static byte[] EDID = ParseUtil.hexStringToByteArray(EDID_STR);

    @Test
    public void testGetEdidAttrs() {
        assertEquals("A", EdidUtil.getManufacturerID(EDID));
        assertEquals("9227", EdidUtil.getProductID(EDID));
        assertEquals("162C0C25", EdidUtil.getSerialNo(EDID));
        assertEquals((byte) 44, EdidUtil.getWeek(EDID));
        assertEquals(2012, EdidUtil.getYear(EDID));
        assertEquals("1.4", EdidUtil.getVersion(EDID));
        assertTrue(EdidUtil.isDigital(EDID));
        assertEquals(60, EdidUtil.getHcm(EDID));
        assertEquals(34, EdidUtil.getVcm(EDID));
    }

    @Test
    public void testGetDescriptors() {
        byte[][] descs = EdidUtil.getDescriptors(EDID);
        for (int i = 0; i < 4; i++) {
            int type = EdidUtil.getDescriptorType(descs[i]);
            String timing = EdidUtil.getTimingDescriptor(descs[i]);
            String range = EdidUtil.getDescriptorRangeLimits(descs[i]);
            switch (i) {
            case 0:
                assertEquals(0x565E00A0, type);
                assertEquals("Clock 241MHz, Active Pixels 2560x1440 ", timing);
                assertEquals("Field Rate -96-41 Hz vertical, 80-48 Hz horizontal, Max clock: 320 MHz", range);
                break;
            case 1:
                assertEquals(0x1A1D0080, type);
                assertEquals("Clock 74MHz, Active Pixels 1280x720 ", timing);
                assertEquals("Field Rate -48-28 Hz vertical, 32-64 Hz horizontal, Max clock: -1280 MHz", range);
                break;
            case 2:
                assertEquals(0xFF, type);
                assertEquals("C02JM2PFF2GC", EdidUtil.getDescriptorText(descs[i]));
                assertEquals(EDID_DESC3, ParseUtil.byteArrayToHexString(descs[i]));
                break;
            case 3:
                assertEquals(0xFC, type);
                assertEquals("Thunderbolt", EdidUtil.getDescriptorText(descs[i]));
                break;
            default:
            }
        }
    }

    @Test
    public void testToString() {
        String[] toString = EdidUtil.toString(EDID).split("\\n");
        assertEquals(6, toString.length);
        toString = EdidUtil.toString(ParseUtil.hexStringToByteArray(EDID_STR2)).split("\\n");
        assertEquals(6, toString.length);
    };
}
