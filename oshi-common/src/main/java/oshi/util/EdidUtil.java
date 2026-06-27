/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.SuppressForbidden;
import oshi.annotation.concurrent.ThreadSafe;

/**
 * EDID parsing utility.
 */
@ThreadSafe
public final class EdidUtil {

    private static final Logger LOG = LoggerFactory.getLogger(EdidUtil.class);

    // EDID byte offsets and field lengths
    private static final int EDID_LENGTH = 128;
    private static final int MANUFACTURER_ID_OFFSET = 8;
    private static final int PRODUCT_ID_OFFSET = 10;
    private static final int SERIAL_NUMBER_OFFSET = 12;
    private static final int WEEK_OFFSET = 16;
    private static final int YEAR_OFFSET = 17;
    private static final int YEAR_BASE = 1990;
    private static final int VERSION_OFFSET = 18;
    private static final int VIDEO_PARAMS_OFFSET = 20;
    private static final int HORIZONTAL_SIZE_OFFSET = 21;
    private static final int VERTICAL_SIZE_OFFSET = 22;
    private static final int STD_TIMING_OFFSET = 38;
    private static final int DESCRIPTOR_OFFSET = 54;
    private static final int DESCRIPTOR_LENGTH = 18;
    private static final int DESCRIPTOR_COUNT = 4;
    private static final int CHECKSUM_OFFSET = 127;
    private static final int MONITOR_NAME_TYPE = 0xFC;
    private static final int DISPLAY_SERIAL_TYPE = 0xFF;

    private EdidUtil() {
    }

    /**
     * Gets the Manufacturer ID from (up to) 3 5-bit characters in bytes 8 and 9
     *
     * @param edid The EDID byte array
     * @return The manufacturer ID
     */
    @SuppressForbidden(reason = "customized base 2 parsing not in Util class")
    public static String getManufacturerID(byte[] edid) {
        // Bytes 8-9 are manufacturer ID in 3 5-bit characters.
        String temp = String.format(Locale.ROOT, "%8s%8s", Integer.toBinaryString(edid[MANUFACTURER_ID_OFFSET] & 0xFF),
                Integer.toBinaryString(edid[MANUFACTURER_ID_OFFSET + 1] & 0xFF)).replace(' ', '0');
        LOG.debug("Manufacurer ID: {}", temp);
        return String.format(Locale.ROOT, "%s%s%s", (char) (64 + Integer.parseInt(temp.substring(1, 6), 2)),
                (char) (64 + Integer.parseInt(temp.substring(6, 11), 2)),
                (char) (64 + Integer.parseInt(temp.substring(11, 16), 2))).replace("@", "");
    }

    /**
     * Gets the Product ID, bytes 10 and 11
     *
     * @param edid The EDID byte array
     * @return The product ID
     */
    public static String getProductID(byte[] edid) {
        // Bytes 10-11 are product ID expressed in hex characters
        return Integer.toHexString(ByteBuffer.wrap(Arrays.copyOfRange(edid, PRODUCT_ID_OFFSET, PRODUCT_ID_OFFSET + 2))
                .order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff);
    }

    /**
     * Gets the Serial number, bytes 12-15
     *
     * @param edid The EDID byte array
     * @return If all 4 bytes represent alphanumeric characters, a 4-character string, otherwise a hex string.
     */
    public static String getSerialNo(byte[] edid) {
        // Bytes 12-15 are Serial number (last 4 characters)
        if (LOG.isDebugEnabled()) {
            LOG.debug("Serial number: {}",
                    Arrays.toString(Arrays.copyOfRange(edid, SERIAL_NUMBER_OFFSET, SERIAL_NUMBER_OFFSET + 4)));
        }
        return String.format(Locale.ROOT, "%s%s%s%s", getAlphaNumericOrHex(edid[SERIAL_NUMBER_OFFSET + 3]),
                getAlphaNumericOrHex(edid[SERIAL_NUMBER_OFFSET + 2]),
                getAlphaNumericOrHex(edid[SERIAL_NUMBER_OFFSET + 1]), getAlphaNumericOrHex(edid[SERIAL_NUMBER_OFFSET]));
    }

    private static String getAlphaNumericOrHex(byte b) {
        return Character.isLetterOrDigit((char) b) ? String.format(Locale.ROOT, "%s", (char) b)
                : String.format(Locale.ROOT, "%02X", b);
    }

    /**
     * Return the week of year of manufacture
     *
     * @param edid The EDID byte array
     * @return The week of year
     */
    public static byte getWeek(byte[] edid) {
        // Byte 16 is manufacture week
        return edid[WEEK_OFFSET];
    }

    /**
     * Return the year of manufacture
     *
     * @param edid The EDID byte array
     * @return The year of manufacture
     */
    public static int getYear(byte[] edid) {
        // Byte 17 is manufacture year-1990
        byte temp = edid[YEAR_OFFSET];
        LOG.debug("Year-1990: {}", temp);
        return temp + YEAR_BASE;
    }

    /**
     * Return the EDID version
     *
     * @param edid The EDID byte array
     * @return The EDID version
     */
    public static String getVersion(byte[] edid) {
        // Bytes 18-19 are EDID version
        return edid[VERSION_OFFSET] + "." + edid[VERSION_OFFSET + 1];
    }

    /**
     * Test if this EDID is a digital monitor based on byte 20
     *
     * @param edid The EDID byte array
     * @return True if the EDID represents a digital monitor, false otherwise
     */
    public static boolean isDigital(byte[] edid) {
        // Byte 20 is Video input params
        return 1 == (edid[VIDEO_PARAMS_OFFSET] & 0xff) >> 7;
    }

    /**
     * Get monitor width in cm
     *
     * @param edid The EDID byte array
     * @return Monitor width in cm
     */
    public static int getHcm(byte[] edid) {
        // Byte 21 is horizontal size in cm
        return edid[HORIZONTAL_SIZE_OFFSET];
    }

    /**
     * Get monitor height in cm
     *
     * @param edid The EDID byte array
     * @return Monitor height in cm
     */
    public static int getVcm(byte[] edid) {
        // Byte 22 is vertical size in cm
        return edid[VERTICAL_SIZE_OFFSET];
    }

    /**
     * Get the VESA descriptors
     *
     * @param edid The EDID byte array
     * @return A 2D array with four 18-byte elements representing VESA descriptors
     */
    public static byte[][] getDescriptors(byte[] edid) {
        byte[][] desc = new byte[DESCRIPTOR_COUNT][DESCRIPTOR_LENGTH];
        for (int i = 0; i < desc.length; i++) {
            System.arraycopy(edid, DESCRIPTOR_OFFSET + DESCRIPTOR_LENGTH * i, desc[i], 0, DESCRIPTOR_LENGTH);
        }
        return desc;
    }

    /**
     * Get the VESA descriptor type
     *
     * @param desc An 18-byte VESA descriptor
     * @return An integer representing the first four bytes of the VESA descriptor
     */
    public static int getDescriptorType(byte[] desc) {
        return ByteBuffer.wrap(Arrays.copyOfRange(desc, 0, 4)).getInt();
    }

    /**
     * Parse a detailed timing descriptor
     *
     * @param desc An 18-byte VESA descriptor
     * @return A string describing part of the detailed timing descriptor
     */
    public static String getTimingDescriptor(byte[] desc) {
        int clock = ByteBuffer.wrap(Arrays.copyOfRange(desc, 0, 2)).order(ByteOrder.LITTLE_ENDIAN).getShort() / 100;
        int hActive = (desc[2] & 0xff) + ((desc[4] & 0xf0) << 4);
        int vActive = (desc[5] & 0xff) + ((desc[7] & 0xf0) << 4);
        return String.format(Locale.ROOT, "Clock %dMHz, Active Pixels %dx%d ", clock, hActive, vActive);
    }

    /**
     * Parse descriptor range limits
     *
     * @param desc An 18-byte VESA descriptor
     * @return A string describing some of the range limits
     */
    public static String getDescriptorRangeLimits(byte[] desc) {
        return String.format(Locale.ROOT, "Field Rate %d-%d Hz vertical, %d-%d Hz horizontal, Max clock: %d MHz",
                desc[5], desc[6], desc[7], desc[8], desc[9] * 10);
    }

    /**
     * Parse descriptor text
     *
     * @param desc An 18-byte VESA descriptor
     * @return Plain text starting at the 4th byte
     */
    public static String getDescriptorText(byte[] desc) {
        return new String(Arrays.copyOfRange(desc, 4, 18), StandardCharsets.US_ASCII).trim();
    }

    /**
     * Get the preferred resolution for the monitor (Eg: 1920x1080)
     *
     * @param edid The edid Byte array
     * @return Plain text preferred resolution
     */

    public static String getPreferredResolution(byte[] edid) {
        int dtd = DESCRIPTOR_OFFSET;
        int horizontalRes = (edid[dtd + 4] & 0xF0) << 4 | edid[dtd + 2] & 0xFF;
        int verticalRes = (edid[dtd + 7] & 0xF0) << 4 | edid[dtd + 5] & 0xFF;
        return horizontalRes + "x" + verticalRes;
    }

    /**
     * Get the monitor model from the EDID
     *
     * @param edid The edid Byte array
     * @return Plain text monitor model
     */

    public static String getModel(byte[] edid) {

        byte[][] desc = EdidUtil.getDescriptors(edid);
        String model = null;

        for (byte[] b : desc) {

            if (EdidUtil.getDescriptorType(b) == MONITOR_NAME_TYPE) {
                model = EdidUtil.getDescriptorText(b);
                break;
            }
        }

        assert model != null;
        String[] tokens = model.split("\\s+");
        if (tokens.length >= 1) {
            model = tokens[tokens.length - 1];
        }
        return model.trim();
    }

    /**
     * Get the display product serial number from the EDID, the text of the serial-number descriptor (type 0xFF). This
     * is distinct from {@link #getSerialNo(byte[])}, which returns the numeric ID serial number in bytes 12-15.
     *
     * @param edid The edid byte array
     * @return The serial-number descriptor text, or {@code null} if the EDID has no serial-number descriptor.
     */
    public static String getProductSerialNumber(byte[] edid) {
        for (byte[] desc : getDescriptors(edid)) {
            if (getDescriptorType(desc) == DISPLAY_SERIAL_TYPE) {
                return getDescriptorText(desc);
            }
        }
        return null;
    }

    /**
     * Creates a 128-byte EDID array populated with the fixed header, EDID version 1.4, "unused" standard timing slots,
     * and zero extension blocks, ready to be populated by the {@code set*} methods.
     * <p>
     * The checksum is not valid until {@link #updateChecksum(byte[])} is called after all fields have been set.
     *
     * @return A new, mutable 128-byte EDID array.
     */
    public static byte[] newEdidTemplate() {
        byte[] edid = new byte[EDID_LENGTH];
        // Fixed 8-byte header: 00 FF FF FF FF FF FF 00
        for (int i = 1; i <= 6; i++) {
            edid[i] = (byte) 0xFF;
        }
        // EDID version 1.4
        edid[VERSION_OFFSET] = 0x01;
        edid[VERSION_OFFSET + 1] = 0x04;
        // Mark the eight standard timing slots (bytes 38-53) as unused (0x01 0x01)
        for (int i = STD_TIMING_OFFSET; i < DESCRIPTOR_OFFSET; i++) {
            edid[i] = 0x01;
        }
        return edid;
    }

    /**
     * Sets the Manufacturer ID into bytes 8 and 9, the inverse of {@link #getManufacturerID(byte[])}.
     * <p>
     * Reliably round-trips a standard three-letter A-Z code. An identifier whose decoded form begins with a stripped
     * '@' (zero-valued) character cannot be reconstructed and is not supported.
     *
     * @param edid           The EDID byte array to modify
     * @param manufacturerId A three-letter manufacturer ID (e.g. {@code "AUO"})
     */
    public static void setManufacturerID(byte[] edid, String manufacturerId) {
        // Pack 3 5-bit characters (value = letter - 64) into bytes 8-9, MSB reserved 0
        int packed = 0;
        for (int i = 0; i < 3; i++) {
            int c = i < manufacturerId.length() ? manufacturerId.charAt(i) - 64 : 0;
            packed = packed << 5 | c & 0x1F;
        }
        edid[MANUFACTURER_ID_OFFSET] = (byte) (packed >> 8 & 0xFF);
        edid[MANUFACTURER_ID_OFFSET + 1] = (byte) (packed & 0xFF);
    }

    /**
     * Sets the Product ID into bytes 10 and 11, the inverse of {@link #getProductID(byte[])}.
     *
     * @param edid      The EDID byte array to modify
     * @param productId The product ID as a hex string
     */
    public static void setProductID(byte[] edid, String productId) {
        // Bytes 10-11 store the product ID little-endian
        int value = ParseUtil.hexStringToInt(productId, 0);
        edid[PRODUCT_ID_OFFSET] = (byte) (value & 0xFF);
        edid[PRODUCT_ID_OFFSET + 1] = (byte) (value >> 8 & 0xFF);
    }

    /**
     * Sets the Serial number into bytes 12-15, the inverse of {@link #getSerialNo(byte[])}.
     * <p>
     * Because {@link #getSerialNo(byte[])} renders each byte as either a single alphanumeric character or two hex
     * digits, only the unambiguous forms are accepted: an 8-character hex string (all four bytes non-printable) or a
     * 4-character printable string (all four bytes alphanumeric).
     *
     * @param edid     The EDID byte array to modify
     * @param serialNo An 8-character hex string or 4-character printable string as returned by
     *                 {@link #getSerialNo(byte[])}
     * @throws IllegalArgumentException if {@code serialNo} is neither 8 hex characters nor 4 printable characters
     */
    public static void setSerialNo(byte[] edid, String serialNo) {
        // getSerialNo emits bytes 15,14,13,12 in order; reverse that mapping back into bytes 12-15
        byte[] bytes;
        if (serialNo.length() == 8) {
            bytes = ParseUtil.hexStringToByteArray(serialNo);
        } else if (serialNo.length() == 4) {
            bytes = serialNo.getBytes(StandardCharsets.US_ASCII);
        } else {
            throw new IllegalArgumentException(
                    "Serial number must be 8 hex characters or 4 printable characters: " + serialNo);
        }
        for (int i = 0; i < 4; i++) {
            edid[SERIAL_NUMBER_OFFSET + 3 - i] = bytes[i];
        }
    }

    /**
     * Sets the week of manufacture into byte 16, the inverse of {@link #getWeek(byte[])}.
     *
     * @param edid The EDID byte array to modify
     * @param week The week of manufacture
     */
    public static void setWeek(byte[] edid, byte week) {
        edid[WEEK_OFFSET] = week;
    }

    /**
     * Sets the year of manufacture into byte 17, the inverse of {@link #getYear(byte[])}.
     *
     * @param edid The EDID byte array to modify
     * @param year The four-digit year of manufacture (stored as {@code year - 1990})
     */
    public static void setYear(byte[] edid, int year) {
        edid[YEAR_OFFSET] = (byte) (year - YEAR_BASE);
    }

    /**
     * Sets the EDID version into bytes 18 and 19, the inverse of {@link #getVersion(byte[])}.
     *
     * @param edid    The EDID byte array to modify
     * @param version The version as a {@code major.minor} string (e.g. {@code "1.4"})
     */
    public static void setVersion(byte[] edid, String version) {
        String[] parts = version.split("\\.");
        edid[VERSION_OFFSET] = (byte) ParseUtil.parseIntOrDefault(parts[0], 1);
        edid[VERSION_OFFSET + 1] = (byte) (parts.length > 1 ? ParseUtil.parseIntOrDefault(parts[1], 0) : 0);
    }

    /**
     * Sets the digital/analog bit (bit 7) of the video input parameters in byte 20, the inverse of
     * {@link #isDigital(byte[])}. The remaining bits of byte 20 are left unchanged.
     *
     * @param edid    The EDID byte array to modify
     * @param digital Whether the EDID represents a digital monitor
     */
    public static void setDigital(byte[] edid, boolean digital) {
        edid[VIDEO_PARAMS_OFFSET] = (byte) (edid[VIDEO_PARAMS_OFFSET] & 0x7F | (digital ? 0x80 : 0x00));
    }

    /**
     * Sets the monitor width in cm into byte 21, the inverse of {@link #getHcm(byte[])}.
     *
     * @param edid The EDID byte array to modify
     * @param hcm  Monitor width in cm
     */
    public static void setHcm(byte[] edid, int hcm) {
        edid[HORIZONTAL_SIZE_OFFSET] = (byte) hcm;
    }

    /**
     * Sets the monitor height in cm into byte 22, the inverse of {@link #getVcm(byte[])}.
     *
     * @param edid The EDID byte array to modify
     * @param vcm  Monitor height in cm
     */
    public static void setVcm(byte[] edid, int vcm) {
        edid[VERTICAL_SIZE_OFFSET] = (byte) vcm;
    }

    /**
     * Sets the preferred resolution into the first (detailed timing) descriptor, the inverse of the resolution decoded
     * by {@link #getPreferredResolution(byte[])}. Only the active-pixel fields are written; other detailed-timing
     * fields are left unchanged.
     *
     * @param edid       The EDID byte array to modify
     * @param resolution The preferred resolution as a {@code WIDTHxHEIGHT} string (e.g. {@code "2560x1440"})
     */
    public static void setPreferredResolution(byte[] edid, String resolution) {
        int x = resolution.indexOf('x');
        int horizontal = ParseUtil.parseIntOrDefault(resolution.substring(0, x), 0);
        int vertical = ParseUtil.parseIntOrDefault(resolution.substring(x + 1), 0);
        int dtd = DESCRIPTOR_OFFSET;
        edid[dtd + 2] = (byte) (horizontal & 0xFF);
        edid[dtd + 4] = (byte) (edid[dtd + 4] & 0x0F | horizontal >> 4 & 0xF0);
        edid[dtd + 5] = (byte) (vertical & 0xFF);
        edid[dtd + 7] = (byte) (edid[dtd + 7] & 0x0F | vertical >> 4 & 0xF0);
    }

    /**
     * Sets the monitor model by writing a monitor-name (type 0xFC) descriptor, the inverse of
     * {@link #getModel(byte[])}. The descriptor is written into descriptor slot one, leaving slot zero for the detailed
     * timing descriptor used by {@link #getPreferredResolution(byte[])}.
     * <p>
     * Since {@link #getModel(byte[])} returns only the final whitespace-delimited token, a single-token model
     * round-trips; a multi-token model is preserved in the EDID bytes but {@code getModel} returns only its last token.
     *
     * @param edid  The EDID byte array to modify
     * @param model The monitor model (truncated to 13 ASCII characters)
     */
    public static void setModel(byte[] edid, String model) {
        setDescriptorText(edid, 1, MONITOR_NAME_TYPE, model);
    }

    /**
     * Sets the display product serial number by writing a serial-number (type 0xFF) descriptor, the inverse of
     * {@link #getProductSerialNumber(byte[])}. The descriptor is written into the third descriptor slot, after the
     * detailed timing descriptor (slot zero) and the monitor name (slot one). This is distinct from
     * {@link #setSerialNo(byte[], String)}, which sets the numeric ID serial number in bytes 12-15.
     *
     * @param edid         The EDID byte array to modify
     * @param serialNumber The product serial number (truncated to 13 ASCII characters)
     */
    public static void setProductSerialNumber(byte[] edid, String serialNumber) {
        setDescriptorText(edid, 2, DISPLAY_SERIAL_TYPE, serialNumber);
    }

    /**
     * Writes a raw 18-byte VESA descriptor into one of the four descriptor slots (0-3), the inverse of
     * {@link #getDescriptors(byte[])}. Slot 0 is conventionally the preferred detailed timing descriptor; slots 1-3
     * hold monitor descriptors (e.g. monitor name, serial number, range limits).
     *
     * @param edid       The EDID byte array to modify
     * @param slot       The descriptor slot, 0-3
     * @param descriptor An 18-byte VESA descriptor
     * @throws IllegalArgumentException if {@code slot} is not in 0-3 or {@code descriptor} is not 18 bytes
     */
    public static void setDescriptor(byte[] edid, int slot, byte[] descriptor) {
        if (slot < 0 || slot >= DESCRIPTOR_COUNT) {
            throw new IllegalArgumentException("Descriptor slot must be 0-" + (DESCRIPTOR_COUNT - 1) + ": " + slot);
        }
        if (descriptor.length != DESCRIPTOR_LENGTH) {
            throw new IllegalArgumentException(
                    "Descriptor must be " + DESCRIPTOR_LENGTH + " bytes: " + descriptor.length);
        }
        System.arraycopy(descriptor, 0, edid, DESCRIPTOR_OFFSET + slot * DESCRIPTOR_LENGTH, DESCRIPTOR_LENGTH);
    }

    /**
     * Writes a text VESA descriptor (e.g. type 0xFC monitor name, 0xFF serial number, 0xFE unspecified text) holding up
     * to 13 ASCII characters into one of the four descriptor slots (0-3), the inverse of
     * {@link #getDescriptorText(byte[])}. The text is 0x0A-terminated and 0x20-padded per the EDID specification.
     *
     * @param edid The EDID byte array to modify
     * @param slot The descriptor slot, 0-3
     * @param type The descriptor type tag
     * @param text The descriptor text (truncated to 13 ASCII characters)
     * @throws IllegalArgumentException if {@code slot} is not in 0-3
     */
    public static void setDescriptorText(byte[] edid, int slot, int type, String text) {
        byte[] desc = new byte[DESCRIPTOR_LENGTH];
        desc[3] = (byte) type;
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        int n = Math.min(13, bytes.length);
        System.arraycopy(bytes, 0, desc, 5, n);
        if (n < 13) {
            desc[5 + n] = 0x0A; // line feed terminator
            for (int i = 6 + n; i < DESCRIPTOR_LENGTH; i++) {
                desc[i] = 0x20; // space padding
            }
        }
        setDescriptor(edid, slot, desc);
    }

    /**
     * Recomputes and stores the EDID checksum in byte 127 so the sum of all 128 bytes is zero modulo 256. Call this
     * after populating an EDID built with {@link #newEdidTemplate()}.
     *
     * @param edid The EDID byte array to modify
     */
    public static void updateChecksum(byte[] edid) {
        int sum = 0;
        for (int i = 0; i < CHECKSUM_OFFSET; i++) {
            sum += edid[i] & 0xFF;
        }
        edid[CHECKSUM_OFFSET] = (byte) ((256 - sum % 256) % 256);
    }

    /**
     * Parse an EDID byte array into user-readable information
     *
     * @param edid An EDID byte array
     * @return User-readable text represented by the EDID
     */
    public static String toString(byte[] edid) {
        StringBuilder sb = new StringBuilder();
        sb.append("  Manuf. ID=").append(EdidUtil.getManufacturerID(edid));
        sb.append(", Product ID=").append(EdidUtil.getProductID(edid));
        sb.append(", ").append(EdidUtil.isDigital(edid) ? "Digital" : "Analog");
        sb.append(", Serial=").append(EdidUtil.getSerialNo(edid));
        sb.append(", ManufDate=").append(EdidUtil.getWeek(edid) * 12 / 52 + 1).append('/')
                .append(EdidUtil.getYear(edid));
        sb.append(", EDID v").append(EdidUtil.getVersion(edid));
        int hSize = EdidUtil.getHcm(edid);
        int vSize = EdidUtil.getVcm(edid);
        sb.append(String.format(Locale.ROOT, "%n  %d x %d cm (%.1f x %.1f in)", hSize, vSize, hSize / 2.54,
                vSize / 2.54));
        byte[][] desc = EdidUtil.getDescriptors(edid);
        for (byte[] b : desc) {
            switch (EdidUtil.getDescriptorType(b)) {
                case 0xff:
                    sb.append("\n  Serial Number: ").append(EdidUtil.getDescriptorText(b));
                    break;
                case 0xfe:
                    sb.append("\n  Unspecified Text: ").append(EdidUtil.getDescriptorText(b));
                    break;
                case 0xfd:
                    sb.append("\n  Range Limits: ").append(EdidUtil.getDescriptorRangeLimits(b));
                    break;
                case 0xfc:
                    sb.append("\n  Monitor Name: ").append(EdidUtil.getDescriptorText(b));
                    break;
                case 0xfb:
                    sb.append("\n  White Point Data: ").append(ParseUtil.byteArrayToHexString(b));
                    break;
                case 0xfa:
                    sb.append("\n  Standard Timing ID: ").append(ParseUtil.byteArrayToHexString(b));
                    break;
                default:
                    if (EdidUtil.getDescriptorType(b) <= 0x0f && EdidUtil.getDescriptorType(b) >= 0x00) {
                        sb.append("\n  Manufacturer Data: ").append(ParseUtil.byteArrayToHexString(b));
                    } else {
                        sb.append("\n  Preferred Timing: ").append(EdidUtil.getTimingDescriptor(b));
                    }
                    break;
            }
        }
        return sb.toString();
    }
}
