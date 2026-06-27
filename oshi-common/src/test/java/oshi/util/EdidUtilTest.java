/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/*
 * Tests EdidUtil
 */
class EdidUtilTest {

    private static final String EDID_HEADER = "00FFFFFFFFFFFF00";
    private static final String EDID_MANUFID = "06af";
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
        assertThat("manufacturerId", EdidUtil.getManufacturerID(EDID), is("AUO"));
        assertThat("productId", EdidUtil.getProductID(EDID), is("9227"));
        assertThat("serialNo", EdidUtil.getSerialNo(EDID), is("162C0C25"));
        assertThat("week", EdidUtil.getWeek(EDID), is((byte) 44));
        assertThat("year", EdidUtil.getYear(EDID), is(2012));
        assertThat("version", EdidUtil.getVersion(EDID), is("1.4"));
        assertThat("digital", EdidUtil.isDigital(EDID), is(true));
        assertThat("hcm", EdidUtil.getHcm(EDID), is(60));
        assertThat("vcm", EdidUtil.getVcm(EDID), is(34));
        assertThat("preferredResolution", EdidUtil.getPreferredResolution(EDID), is("2560x1440"));
        assertThat("model", EdidUtil.getModel(EDID), is("Thunderbolt"));
        assertThat("productSerialNumber", EdidUtil.getProductSerialNumber(EDID), is("C02JM2PFF2GC"));
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

    @Test
    void testGetModelAbsent() {
        // An EDID with no monitor-name (0xFC) descriptor returns an empty string rather than throwing
        assertThat("absent model", EdidUtil.getModel(EdidUtil.newEdidTemplate()), is(""));
    }

    @Test
    void testToStringNegativeDescriptorType() {
        // A descriptor whose type decodes to a negative int falls through to the preferred-timing branch
        byte[] edid = EdidUtil.newEdidTemplate();
        byte[] desc = new byte[18];
        desc[0] = (byte) 0x80; // high bit set so getDescriptorType is negative
        EdidUtil.setDescriptor(edid, 1, desc);
        assertThat("preferred timing", EdidUtil.toString(edid), containsString("Preferred Timing"));
    }

    @Test
    void testFieldRoundTrip() {
        // Synthesize an EDID from field values, then confirm each set* encoder inverts its get*/is* parser
        byte[] edid = EdidUtil.newEdidTemplate();
        EdidUtil.setManufacturerID(edid, "AUO");
        EdidUtil.setProductID(edid, "9227");
        EdidUtil.setSerialNo(edid, "162C0C25");
        EdidUtil.setWeek(edid, (byte) 44);
        EdidUtil.setYear(edid, 2012);
        EdidUtil.setVersion(edid, "1.4");
        EdidUtil.setDigital(edid, true);
        EdidUtil.setHcm(edid, 60);
        EdidUtil.setVcm(edid, 34);
        EdidUtil.setPreferredResolution(edid, "2560x1440");
        EdidUtil.setModel(edid, "Thunderbolt");
        EdidUtil.setProductSerialNumber(edid, "C02JM2PFF2GC");
        EdidUtil.updateChecksum(edid);

        assertThat("manufacturerId", EdidUtil.getManufacturerID(edid), is("AUO"));
        assertThat("productId", EdidUtil.getProductID(edid), is("9227"));
        assertThat("serialNo", EdidUtil.getSerialNo(edid), is("162C0C25"));
        assertThat("week", EdidUtil.getWeek(edid), is((byte) 44));
        assertThat("year", EdidUtil.getYear(edid), is(2012));
        assertThat("version", EdidUtil.getVersion(edid), is("1.4"));
        assertThat("digital", EdidUtil.isDigital(edid), is(true));
        assertThat("hcm", EdidUtil.getHcm(edid), is(60));
        assertThat("vcm", EdidUtil.getVcm(edid), is(34));
        assertThat("preferredResolution", EdidUtil.getPreferredResolution(edid), is("2560x1440"));
        assertThat("model", EdidUtil.getModel(edid), is("Thunderbolt"));
        assertThat("productSerialNumber", EdidUtil.getProductSerialNumber(edid), is("C02JM2PFF2GC"));

        // A valid EDID's 128 bytes sum to zero modulo 256
        int sum = 0;
        for (byte b : edid) {
            sum += b & 0xFF;
        }
        assertThat("checksum", sum % 256, is(0));
    }

    @Test
    void testByteRoundTrip() {
        // For injective fields, set*(template, get*(EDID)) reproduces the original EDID bytes exactly
        byte[] edid = EdidUtil.newEdidTemplate();
        EdidUtil.setManufacturerID(edid, EdidUtil.getManufacturerID(EDID));
        EdidUtil.setProductID(edid, EdidUtil.getProductID(EDID));
        EdidUtil.setSerialNo(edid, EdidUtil.getSerialNo(EDID));
        EdidUtil.setWeek(edid, EdidUtil.getWeek(EDID));
        EdidUtil.setYear(edid, EdidUtil.getYear(EDID));
        EdidUtil.setVersion(edid, EdidUtil.getVersion(EDID));
        EdidUtil.setHcm(edid, EdidUtil.getHcm(EDID));
        EdidUtil.setVcm(edid, EdidUtil.getVcm(EDID));

        assertThat("manufacturer bytes 8-9", Arrays.copyOfRange(edid, 8, 10), is(Arrays.copyOfRange(EDID, 8, 10)));
        assertThat("product bytes 10-11", Arrays.copyOfRange(edid, 10, 12), is(Arrays.copyOfRange(EDID, 10, 12)));
        assertThat("serial bytes 12-15", Arrays.copyOfRange(edid, 12, 16), is(Arrays.copyOfRange(EDID, 12, 16)));
        assertThat("week byte 16", edid[16], is(EDID[16]));
        assertThat("year byte 17", edid[17], is(EDID[17]));
        assertThat("version bytes 18-19", Arrays.copyOfRange(edid, 18, 20), is(Arrays.copyOfRange(EDID, 18, 20)));
        assertThat("hcm byte 21", edid[21], is(EDID[21]));
        assertThat("vcm byte 22", edid[22], is(EDID[22]));
    }

    @Test
    void testSetDescriptors() {
        byte[] edid = EdidUtil.newEdidTemplate();
        // Text descriptors can be written into any of the four slots, including the fourth
        EdidUtil.setDescriptorText(edid, 3, 0xFE, "Extra Text");
        byte[][] descs = EdidUtil.getDescriptors(edid);
        assertThat("slot 3 type", EdidUtil.getDescriptorType(descs[3]), is(0xFE));
        assertThat("slot 3 text", EdidUtil.getDescriptorText(descs[3]), is("Extra Text"));

        // A raw 18-byte descriptor round-trips through getDescriptors
        byte[] raw = descs[3];
        byte[] edid2 = EdidUtil.newEdidTemplate();
        EdidUtil.setDescriptor(edid2, 0, raw);
        assertThat("raw descriptor", EdidUtil.getDescriptors(edid2)[0], is(raw));

        // Out-of-range slot and wrong-size descriptor are rejected
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setDescriptorText(edid, 4, 0xFE, "x"));
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setDescriptor(edid, 0, new byte[10]));
    }

    @Test
    void testEncoderEdgeCases() {
        byte[] edid = EdidUtil.newEdidTemplate();
        // Version with no minor component defaults the minor to 0
        EdidUtil.setVersion(edid, "2");
        assertThat("major-only version", EdidUtil.getVersion(edid), is("2.0"));
        // Analog (non-digital) display clears the digital bit
        EdidUtil.setDigital(edid, false);
        assertThat("analog", EdidUtil.isDigital(edid), is(false));
        // High unsigned byte values round-trip (guards against signed-byte sign extension)
        EdidUtil.setHcm(edid, 200);
        assertThat("hcm 200", EdidUtil.getHcm(edid), is(200));
        EdidUtil.setVcm(edid, 255);
        assertThat("vcm 255", EdidUtil.getVcm(edid), is(255));
        EdidUtil.setYear(edid, 2200);
        assertThat("year 2200", EdidUtil.getYear(edid), is(2200));
        // Descriptor text longer than 13 characters is truncated with no terminator/padding
        EdidUtil.setDescriptorText(edid, 3, 0xFE, "ThisIsAVeryLongName");
        assertThat("truncated descriptor", EdidUtil.getDescriptorText(EdidUtil.getDescriptors(edid)[3]),
                is("ThisIsAVeryLo"));
        // No serial-number descriptor present returns an empty string (never null), like getSerialNo/getModel
        assertThat("absent serial", EdidUtil.getProductSerialNumber(EdidUtil.newEdidTemplate()), is(""));
    }

    @Test
    void testEncoderValidation() {
        byte[] edid = EdidUtil.newEdidTemplate();
        // Manufacturer ID must be exactly three uppercase A-Z letters
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setManufacturerID(edid, "AB"));
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setManufacturerID(edid, "auo"));
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setManufacturerID(edid, "A1B"));
        // Product ID must be a hex value within 0-FFFF
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setProductID(edid, "10000"));
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setProductID(edid, "GG"));
        // Year must be within 1990-2245
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setYear(edid, 1989));
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setYear(edid, 2246));
        // Size in cm must be within 0-255
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setHcm(edid, 256));
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setHcm(edid, -1));
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setVcm(edid, 256));
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setVcm(edid, -1));
        // A negative descriptor slot is rejected
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setDescriptor(edid, -1, new byte[18]));
    }

    @Test
    void testSetSerialNo() {
        // A 4-character all-alphanumeric serial round-trips
        byte[] edid = EdidUtil.newEdidTemplate();
        EdidUtil.setSerialNo(edid, "AB12");
        assertThat("alphanumeric serial", EdidUtil.getSerialNo(edid), is("AB12"));
        // Wrong length is rejected
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setSerialNo(edid, "ABC"));
        // Hex-looking ASCII whose bytes are letters does not round-trip (getSerialNo would render "ABCD")
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setSerialNo(edid, "41424344"));
        // Non-alphanumeric 4-character value does not round-trip
        assertThrows(IllegalArgumentException.class, () -> EdidUtil.setSerialNo(edid, "AB-1"));
    }
}
