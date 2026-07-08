/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import oshi.util.ParseUtil;

/*
 * Tests DisplayInfoImpl
 */
class DisplayInfoImplTest {

    private static final String EDID_STR = "00FFFFFFFFFFFF00" + "06af" + "2792" + "250C2C16" + "2C16" + "0104"
            + "B53C2278226FB1A7554C9E250C5054000000" + "01010101010101010101010101010101"
            + "565E00A0A0A029503020350055502100001A" + "1A1D008051D01C204080350055502100001C"
            + "000000FF004330324A4D325046463247430A" + "000000FC005468756E646572626F6C740A20" + "01" + "C7";
    private static final byte[] EDID = ParseUtil.hexStringToByteArray(EDID_STR);

    @Test
    void testFromEdid() {
        DisplayInfo info = new DisplayInfoImpl(EDID);
        assertThat("synthetic", info.isEdidSynthetic(), is(false));
        assertThat("manufacturerId", info.getManufacturerID(), is("AUO"));
        assertThat("productId", info.getProductID(), is("9227"));
        assertThat("serialNo", info.getSerialNo(), is("162C0C25"));
        assertThat("week", info.getWeek(), is((byte) 44));
        assertThat("year", info.getYear(), is(2012));
        assertThat("version", info.getVersion(), is("1.4"));
        assertThat("digital", info.isDigital(), is(true));
        assertThat("hcm", info.getHcm(), is(60));
        assertThat("vcm", info.getVcm(), is(34));
        assertThat("preferredResolution", info.getPreferredResolution(), is("2560x1440"));
        assertThat("model", info.getModel(), is("Thunderbolt"));
        assertThat("productSerialNumber", info.getProductSerialNumber(), is("C02JM2PFF2GC"));
        assertThat("edid", info.getEdid(), is(EDID));
    }

    @Test
    void testSyntheticRoundTrip() {
        DisplayInfo info = new DisplayInfoImpl("AUO", "9227", "162C0C25", (byte) 44, 2012, "1.4", true, 60, 34,
                "2560x1440", "Thunderbolt", "C02JM2PFF2GC");
        assertThat("synthetic", info.isEdidSynthetic(), is(true));
        // Exercise the synthetic-branch field getters (return the stored values directly)
        assertThat("synth manufacturerId", info.getManufacturerID(), is("AUO"));
        assertThat("synth productId", info.getProductID(), is("9227"));
        assertThat("synth serialNo", info.getSerialNo(), is("162C0C25"));
        assertThat("synth week", info.getWeek(), is((byte) 44));
        assertThat("synth year", info.getYear(), is(2012));
        assertThat("synth version", info.getVersion(), is("1.4"));
        assertThat("synth digital", info.isDigital(), is(true));
        assertThat("synth hcm", info.getHcm(), is(60));
        assertThat("synth vcm", info.getVcm(), is(34));
        assertThat("synth preferredResolution", info.getPreferredResolution(), is("2560x1440"));
        assertThat("synth model", info.getModel(), is("Thunderbolt"));
        assertThat("synth productSerialNumber", info.getProductSerialNumber(), is("C02JM2PFF2GC"));
        // getEdid() is idempotent and stable across calls
        assertThat("stable edid", info.getEdid(), is(info.getEdid()));
        // Re-parsing the synthesized EDID yields the same field values
        DisplayInfo reparsed = new DisplayInfoImpl(info.getEdid());
        assertThat("reparsed not synthetic", reparsed.isEdidSynthetic(), is(false));
        assertThat("manufacturerId", reparsed.getManufacturerID(), is("AUO"));
        assertThat("productId", reparsed.getProductID(), is("9227"));
        assertThat("serialNo", reparsed.getSerialNo(), is("162C0C25"));
        assertThat("week", reparsed.getWeek(), is((byte) 44));
        assertThat("year", reparsed.getYear(), is(2012));
        assertThat("version", reparsed.getVersion(), is("1.4"));
        assertThat("digital", reparsed.isDigital(), is(true));
        assertThat("hcm", reparsed.getHcm(), is(60));
        assertThat("vcm", reparsed.getVcm(), is(34));
        assertThat("preferredResolution", reparsed.getPreferredResolution(), is("2560x1440"));
        assertThat("model", reparsed.getModel(), is("Thunderbolt"));
        assertThat("productSerialNumber", reparsed.getProductSerialNumber(), is("C02JM2PFF2GC"));
    }

    @Test
    void testSyntheticToStringDoesNotSynthesize() {
        // A serial number that EdidUtil.setSerialNo string form rejects: toString must still work (it formats from the
        // decoded fields) without triggering EDID synthesis. getEdid gracefully falls back to zeroed bytes 12-15.
        DisplayInfo info = new DisplayInfoImpl("AUO", "9227", "BADSERIAL", (byte) 1, 2020, "1.4", true, 50, 30,
                "1920x1080", "Acme", "SN12345");
        assertThat("toString model", info.toString(), containsString("Acme"));
        assertThat("toString serial", info.toString(), containsString("SN12345"));
        // getEdid no longer throws; it falls back to zero serial bytes
        byte[] edid = info.getEdid();
        assertThat("edid length", edid.length, is(128));
    }

    @Test
    void testSyntheticNumericSerialFallback() {
        // A serial like "FD626D62" fails EdidUtil.setSerialNo(String) because byte 0xFD is not alphanumeric while
        // 0x62/'b' and 0x6D/'m' are, causing getSerialNo to return a mixed-format string that doesn't match.
        // setSerialNoSafe falls back to the numeric overload, writing the bytes directly.
        DisplayInfo info = new DisplayInfoImpl("APP", "a050", "FD626D62", (byte) 0, 1990, "1.4", true, 34, 22,
                "3456x2234", "Built-in Retina Display", "");
        assertThat("serialNo field", info.getSerialNo(), is("FD626D62"));
        // getEdid succeeds (no exception) and produces a valid 128-byte EDID
        byte[] edid = info.getEdid();
        assertThat("edid length", edid.length, is(128));
        // Bytes 12-15 contain the numeric serial in little-endian
        assertThat("edid[12]", edid[12] & 0xFF, is(0x62));
        assertThat("edid[13]", edid[13] & 0xFF, is(0x6D));
        assertThat("edid[14]", edid[14] & 0xFF, is(0x62));
        assertThat("edid[15]", edid[15] & 0xFF, is(0xFD));
    }

    @Test
    void testSyntheticNullPreferredResolution() {
        // A null preferred resolution should not cause an NPE during EDID synthesis
        DisplayInfo info = new DisplayInfoImpl("APP", "a050", "00000000", (byte) 0, 1990, "1.4", true, 34, 22, null,
                "Built-in Retina Display", "");
        byte[] edid = info.getEdid();
        assertThat("edid length", edid.length, is(128));
    }

    @Test
    void testSyntheticNullSerialNo() {
        // A null serialNo is treated as absent — bytes 12-15 stay zeroed
        DisplayInfo info = new DisplayInfoImpl("APP", "a050", null, (byte) 0, 1990, "1.4", true, 0, 0, "3456x2234",
                "Test", "");
        byte[] edid = info.getEdid();
        assertThat("edid length", edid.length, is(128));
        assertThat("edid[12]", edid[12], is((byte) 0));
        assertThat("edid[13]", edid[13], is((byte) 0));
        assertThat("edid[14]", edid[14], is((byte) 0));
        assertThat("edid[15]", edid[15], is((byte) 0));
    }

    @Test
    void testSyntheticEmptySerialNo() {
        // An empty serialNo is treated as absent — bytes 12-15 stay zeroed
        DisplayInfo info = new DisplayInfoImpl("APP", "a050", "", (byte) 0, 1990, "1.4", true, 0, 0, "3456x2234",
                "Test", "");
        byte[] edid = info.getEdid();
        assertThat("edid length", edid.length, is(128));
        assertThat("edid[12]", edid[12], is((byte) 0));
        assertThat("edid[13]", edid[13], is((byte) 0));
        assertThat("edid[14]", edid[14], is((byte) 0));
        assertThat("edid[15]", edid[15], is((byte) 0));
    }

    @Test
    void testSyntheticNonHexSerialNoFallsBackToZero() {
        // A non-hex 8-char serial that also fails the string form falls back to zeroed bytes
        DisplayInfo info = new DisplayInfoImpl("APP", "a050", "NOTAHEX!", (byte) 0, 1990, "1.4", true, 0, 0,
                "3456x2234", "Test", "");
        byte[] edid = info.getEdid();
        assertThat("edid length", edid.length, is(128));
        assertThat("edid[12]", edid[12], is((byte) 0));
        assertThat("edid[13]", edid[13], is((byte) 0));
        assertThat("edid[14]", edid[14], is((byte) 0));
        assertThat("edid[15]", edid[15], is((byte) 0));
    }

    @Test
    void testSyntheticNullProductSerialNumber() {
        // A null product serial number is omitted from both the synthesized EDID and the toString output; an analog
        // display also exercises the non-digital toString branch
        DisplayInfo info = new DisplayInfoImpl("AUO", "9227", "162C0C25", (byte) 44, 2012, "1.4", false, 60, 34,
                "2560x1440", "Thunderbolt", null);
        assertThat("analog", info.isDigital(), is(false));
        assertThat("toString analog", info.toString(), containsString("Analog"));
        // An absent product serial number is reported as an empty string, never null
        assertThat("productSerialNumber", info.getProductSerialNumber(), is(""));
        assertThat("toString omits serial", info.toString(), not(containsString("Serial Number")));
        // The synthesized EDID has no serial-number descriptor, so a reparse also finds none
        DisplayInfo reparsed = new DisplayInfoImpl(info.getEdid());
        assertThat("reparsed productSerialNumber", reparsed.getProductSerialNumber(), is(""));
    }
}
