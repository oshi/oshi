/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import java.util.Arrays;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.EdidUtil;
import oshi.util.ParseUtil;

/**
 * The default {@link DisplayInfo} implementation. This is not part of the OSHI public API and may change between minor
 * releases.
 * <p>
 * An instance is created either from a raw EDID byte array (fields decoded from the EDID on demand) or from individual
 * field values, in which case {@link #isEdidSynthetic()} is {@code true} and {@link #getEdid()} lazily synthesizes an
 * EDID from the supplied fields. The synthetic serial number must be in a form accepted by
 * {@link EdidUtil#setSerialNo(byte[], String)} (8 hex characters or 4 printable characters).
 */
@ThreadSafe
public class DisplayInfoImpl implements DisplayInfo {

    private final boolean synthetic;

    // For a real EDID this is set in the constructor and the field getters parse it on demand; for a synthetic instance
    // it starts null and is lazily populated by getEdid() from the field values below.
    @SuppressWarnings("java:S3077") // the array reference is swapped wholesale, never mutated in place
    private volatile byte @Nullable [] edid;

    // Populated only for a synthetic instance; for a real instance the getters derive these from the EDID bytes, and
    // these hold the empty string rather than null so that no getter can return null for either flavor of instance.
    private final String manufacturerID;
    private final String productID;
    private final String serialNo;
    private final byte week;
    private final int year;
    private final String version;
    private final boolean digital;
    private final int hcm;
    private final int vcm;
    private final String preferredResolution;
    private final String model;
    private final String productSerialNumber;

    /**
     * Constructs a {@code DisplayInfoImpl} from a raw EDID byte array. The fields are parsed from the EDID on demand.
     *
     * @param edid The raw EDID byte array as reported by the display.
     */
    public DisplayInfoImpl(byte[] edid) {
        this.edid = Arrays.copyOf(edid, edid.length);
        this.synthetic = false;
        this.manufacturerID = "";
        this.productID = "";
        this.serialNo = "";
        this.week = 0;
        this.year = 0;
        this.version = "";
        this.digital = false;
        this.hcm = 0;
        this.vcm = 0;
        this.preferredResolution = "";
        this.model = "";
        this.productSerialNumber = "";
    }

    /**
     * Constructs a synthetic {@code DisplayInfoImpl} from individual field values, for displays that report their
     * attributes without providing an EDID. The EDID returned by {@link #getEdid()} is synthesized on demand from these
     * values, and {@link #isEdidSynthetic()} returns {@code true}.
     *
     * @param manufacturerID      The three-letter manufacturer ID (see {@link EdidUtil#getManufacturerID(byte[])}).
     * @param productID           The product ID as a hex string (see {@link EdidUtil#getProductID(byte[])}).
     * @param serialNo            The serial number, either 8 hex characters or 4 printable characters (see
     *                            {@link EdidUtil#getSerialNo(byte[])}); {@code null} or empty if not available.
     * @param week                The week of manufacture.
     * @param year                The four-digit year of manufacture.
     * @param version             The EDID version as a {@code major.minor} string (e.g. {@code "1.4"}).
     * @param digital             Whether the display is digital.
     * @param hcm                 The monitor width in cm.
     * @param vcm                 The monitor height in cm.
     * @param preferredResolution The preferred resolution as a {@code WIDTHxHEIGHT} string (e.g. {@code "2560x1440"});
     *                            {@code null} or empty if not available.
     * @param model               The monitor model; {@code null} or empty if not available.
     * @param productSerialNumber The display product serial number (the serial-number descriptor text, distinct from
     *                            {@code serialNo}); {@code null} or empty if not available.
     */
    @SuppressWarnings("java:S107") // value holder mirroring the EdidUtil field set
    public DisplayInfoImpl(String manufacturerID, String productID, @Nullable String serialNo, byte week, int year,
            String version, boolean digital, int hcm, int vcm, @Nullable String preferredResolution,
            @Nullable String model, @Nullable String productSerialNumber) {
        this.edid = null;
        this.synthetic = true;
        this.manufacturerID = manufacturerID;
        this.productID = productID;
        this.week = week;
        this.year = year;
        this.version = version;
        this.digital = digital;
        this.hcm = hcm;
        this.vcm = vcm;
        // Normalize absent values to empty so no getter returns null, matching the DisplayInfo contract and the
        // unknown-is-empty convention the rest of the API follows.
        this.serialNo = ParseUtil.getStringValueOrEmpty(serialNo);
        this.preferredResolution = ParseUtil.getStringValueOrEmpty(preferredResolution);
        this.model = ParseUtil.getStringValueOrEmpty(model);
        this.productSerialNumber = ParseUtil.getStringValueOrEmpty(productSerialNumber);
    }

    @Override
    public byte[] getEdid() {
        byte[] result = cachedEdid();
        return Arrays.copyOf(result, result.length);
    }

    private synchronized byte[] cachedEdid() {
        // For a real EDID this is set in the constructor; for a synthetic instance it is synthesized once on first use.
        if (this.edid == null) {
            this.edid = synthesizeEdid();
        }
        return this.edid;
    }

    @Override
    public boolean isEdidSynthetic() {
        return this.synthetic;
    }

    @Override
    public String getManufacturerID() {
        return this.synthetic ? this.manufacturerID : EdidUtil.getManufacturerID(cachedEdid());
    }

    @Override
    public String getProductID() {
        return this.synthetic ? this.productID : EdidUtil.getProductID(cachedEdid());
    }

    @Override
    public String getSerialNo() {
        return this.synthetic ? this.serialNo : EdidUtil.getSerialNo(cachedEdid());
    }

    @Override
    public byte getWeek() {
        return this.synthetic ? this.week : EdidUtil.getWeek(cachedEdid());
    }

    @Override
    public int getYear() {
        return this.synthetic ? this.year : EdidUtil.getYear(cachedEdid());
    }

    @Override
    public String getVersion() {
        return this.synthetic ? this.version : EdidUtil.getVersion(cachedEdid());
    }

    @Override
    public boolean isDigital() {
        return this.synthetic ? this.digital : EdidUtil.isDigital(cachedEdid());
    }

    @Override
    public int getHcm() {
        return this.synthetic ? this.hcm : EdidUtil.getHcm(cachedEdid());
    }

    @Override
    public int getVcm() {
        return this.synthetic ? this.vcm : EdidUtil.getVcm(cachedEdid());
    }

    @Override
    public String getPreferredResolution() {
        return this.synthetic ? this.preferredResolution : EdidUtil.getPreferredResolution(cachedEdid());
    }

    @Override
    public String getModel() {
        return this.synthetic ? this.model : EdidUtil.getModel(cachedEdid());
    }

    @Override
    public String getProductSerialNumber() {
        return this.synthetic ? this.productSerialNumber : EdidUtil.getProductSerialNumber(cachedEdid());
    }

    private byte[] synthesizeEdid() {
        byte[] e = EdidUtil.newEdidTemplate();
        EdidUtil.setManufacturerID(e, this.manufacturerID);
        EdidUtil.setProductID(e, this.productID);
        setSerialNoSafe(e, this.serialNo);
        EdidUtil.setWeek(e, this.week);
        EdidUtil.setYear(e, this.year);
        EdidUtil.setVersion(e, this.version);
        EdidUtil.setDigital(e, this.digital);
        EdidUtil.setHcm(e, this.hcm);
        EdidUtil.setVcm(e, this.vcm);
        EdidUtil.setPreferredResolution(e, this.preferredResolution);
        EdidUtil.setModel(e, this.model);
        if (!this.productSerialNumber.isEmpty()) {
            EdidUtil.setProductSerialNumber(e, this.productSerialNumber);
        }
        EdidUtil.updateChecksum(e);
        return e;
    }

    // Sets the serial number into EDID bytes 12-15. Tries the string form first (which validates round-trip); if that
    // fails (e.g. bytes happen to be printable ASCII causing getSerialNo to interpret differently), falls back to the
    // numeric long form which writes the bytes directly.
    private static void setSerialNoSafe(byte[] edid, String serialNo) {
        if (serialNo.isEmpty()) {
            return;
        }
        try {
            EdidUtil.setSerialNo(edid, serialNo);
        } catch (IllegalArgumentException e) {
            if (serialNo.length() == 8) {
                long numeric = ParseUtil.hexStringToLong(serialNo, 0L);
                if (numeric != 0L) {
                    EdidUtil.setSerialNo(edid, numeric);
                }
            }
        }
    }

    @Override
    public String toString() {
        // For a real EDID, defer to the full EdidUtil rendering of the existing bytes. For a synthetic instance, format
        // directly from the decoded fields so toString never triggers (and re-decodes) a synthesized EDID.
        if (!this.synthetic) {
            return EdidUtil.toString(cachedEdid());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("  Manuf. ID=").append(this.manufacturerID);
        sb.append(", Product ID=").append(this.productID);
        sb.append(", ").append(this.digital ? "Digital" : "Analog");
        sb.append(", Serial=").append(this.serialNo);
        sb.append(", ManufDate=").append(this.week * 12 / 52 + 1).append('/').append(this.year);
        sb.append(", EDID v").append(this.version);
        sb.append(String.format(Locale.ROOT, "%n  %d x %d cm (%.1f x %.1f in)", this.hcm, this.vcm, this.hcm / 2.54,
                this.vcm / 2.54));
        sb.append(String.format(Locale.ROOT, "%n  Preferred Resolution: %s", this.preferredResolution));
        sb.append(String.format(Locale.ROOT, "%n  Monitor Name: %s", this.model));
        if (!this.productSerialNumber.isEmpty()) {
            sb.append(String.format(Locale.ROOT, "%n  Serial Number: %s", this.productSerialNumber));
        }
        return sb.toString();
    }
}
