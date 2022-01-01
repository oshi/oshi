/*
 * MIT License
 *
 * Copyright (c) 2018-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

/*
 * Tests EdidUtil
 */
class EdidUtilTest {

    private static final String EDID_HEADER = "00FFFFFFFFFFFF00";
    private static final String EDID_MANUFID = "0610";
    private static final String EDID_PRODCODE = "2792";
    private static final String EDID_SERIAL = "250C2C16";
    private static final String EDID_WKYR = "2C16";
    private static final String EDID_VERSION = "0104";
    private static final String EDID_VIDEO = "B53C2278226FB1A7554C9E250C5054000000";
    private static final String EDID_TIMING = "01010101010101010101010101010101";
    private static final String EDID_DESC1 = "565E00A0A0A029503020350055502100001A";
    private static final String EDID_DESC2 = "1A1D008051D01C204080350055502100001C";
    private static final String EDID_DESC3 = "000000FF004330324A4D325046463247430A";
    private static final String EDID_DESC4 = "000000FC005468756E646572626F6C740A20";
    private static final String EDID_DESC5 = "000000FA004330324A4D325046463247430A";
    private static final String EDID_DESC6 = "000000FB005468756E646572626F6C740A20";
    private static final String EDID_DESC7 = "000000FD004330324A4D325046463247430A";
    private static final String EDID_DESC8 = "000000FE005468756E646572626F6C740A20";
    private static final String EDID_EXTS = "01";
    private static final String EDID_CHKSUM = "C7";
    private static final String EDID_STR = EDID_HEADER + EDID_MANUFID + EDID_PRODCODE + EDID_SERIAL + EDID_WKYR
            + EDID_VERSION + EDID_VIDEO + EDID_TIMING + EDID_DESC1 + EDID_DESC2 + EDID_DESC3 + EDID_DESC4 + EDID_EXTS
            + EDID_CHKSUM;
    private static final String EDID_STR2 = EDID_HEADER + EDID_MANUFID + EDID_PRODCODE + EDID_SERIAL + EDID_WKYR
            + EDID_VERSION + EDID_VIDEO + EDID_TIMING + EDID_DESC5 + EDID_DESC6 + EDID_DESC7 + EDID_DESC8 + EDID_EXTS
            + EDID_CHKSUM;
    private static final byte[] EDID = ParseUtil.hexStringToByteArray(EDID_STR);

    @Test
    void testGetEdidAttrs() {
        assertThat("manufacturerId", EdidUtil.getManufacturerID(EDID), is("A"));
        assertThat("productId", EdidUtil.getProductID(EDID), is("9227"));
        assertThat("serialNo", EdidUtil.getSerialNo(EDID), is("162C0C25"));
        assertThat("week", EdidUtil.getWeek(EDID), is((byte) 44));
        assertThat("year", EdidUtil.getYear(EDID), is(2012));
        assertThat("version", EdidUtil.getVersion(EDID), is("1.4"));
        assertThat("digital", EdidUtil.isDigital(EDID), is(true));
        assertThat("hcm", EdidUtil.getHcm(EDID), is(60));
        assertThat("vcm", EdidUtil.getVcm(EDID), is(34));
    }

    @Test
    void testGetDescriptors() {
        byte[][] descs = EdidUtil.getDescriptors(EDID);
        for (int i = 0; i < 4; i++) {
            int type = EdidUtil.getDescriptorType(descs[i]);
            String timing = EdidUtil.getTimingDescriptor(descs[i]);
            String range = EdidUtil.getDescriptorRangeLimits(descs[i]);
            switch (i) {
            case 0:
                assertThat("desc 0 type", type, is(0x565E00A0));
                assertThat("desc 0 timing", timing, is("Clock 241MHz, Active Pixels 2560x1440 "));
                assertThat("desc 0 range", range,
                        is("Field Rate -96-41 Hz vertical, 80-48 Hz horizontal, Max clock: 320 MHz"));
                break;
            case 1:
                assertThat("desc 1 type", type, is(0x1A1D0080));
                assertThat("desc 1 timing", timing, is("Clock 74MHz, Active Pixels 1280x720 "));
                assertThat("desc 1 range", range,
                        is("Field Rate -48-28 Hz vertical, 32-64 Hz horizontal, Max clock: -1280 MHz"));
                break;
            case 2:
                assertThat("desc 2 type", type, is(0xFF));
                assertThat("desc 2 descriptorText", EdidUtil.getDescriptorText(descs[i]), is("C02JM2PFF2GC"));
                assertThat("desc 2 descriptorHex", ParseUtil.byteArrayToHexString(descs[i]), is(EDID_DESC3));
                break;
            case 3:
                assertThat("desc 3 type", type, is(0xFC));
                assertThat("desc 3 descriptorText", EdidUtil.getDescriptorText(descs[i]), is("Thunderbolt"));
                break;
            default:
            }
        }
    }

    @Test
    void testToString() {
        assertThat("edid toString", EdidUtil.toString(EDID).split("\\n"), is(arrayWithSize(6)));
        assertThat("edid2 toString", EdidUtil.toString(ParseUtil.hexStringToByteArray(EDID_STR2)).split("\\n"),
                is(arrayWithSize(6)));
    }
}
