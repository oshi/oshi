/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.common;

import java.util.Arrays;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.hardware.Display;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.util.EdidUtil;

/**
 * A Display
 */
public abstract class AbstractDisplay implements Display {

    private static final long serialVersionUID = 1L;

    protected byte[] edid;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    protected AbstractDisplay(byte[] edid) {
        this.edid = edid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getEdid() {
        return Arrays.copyOf(edid, edid.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("edid", EdidUtil.toString(getEdid())).build();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  Manuf. ID=").append(EdidUtil.getManufacturerID(edid)).append(", Product ID=")
                .append(EdidUtil.getProductID(edid)).append(", ")
                .append((EdidUtil.isDigital(edid) ? "Digital" : "Analog")).append(", Serial=")
                .append(EdidUtil.getSerialNo(edid)).append(", ManufDate=")
                .append((EdidUtil.getWeek(edid) * 12 / 52 + 1) + "/").append(EdidUtil.getYear(edid)).append(", EDID v")
                .append(EdidUtil.getVersion(edid));
        int hSize = EdidUtil.getHcm(edid);
        int vSize = EdidUtil.getVcm(edid);
        sb.append(String.format("%n  %d x %d cm (%.1f x %.1f in)", hSize, vSize, hSize / 2.54, vSize / 2.54));
        byte[][] desc = EdidUtil.getDescriptors(edid);
        for (int d = 0; d < desc.length; d++) {
            switch (EdidUtil.getDescriptorType(desc[d])) {
            case 0xff:
                sb.append("\n  Serial Number: ").append(EdidUtil.getDescriptorText(desc[d]));
                break;
            case 0xfe:
                sb.append("\n  Unspecified Text: ").append(EdidUtil.getDescriptorText(desc[d]));
                break;
            case 0xfd:
                sb.append("\n  Range Limits: ").append(EdidUtil.getDescriptorRangeLimits(desc[d]));
                break;
            case 0xfc:
                sb.append("\n  Monitor Name: ").append(EdidUtil.getDescriptorText(desc[d]));
                break;
            case 0xfb:
                sb.append("\n  White Point Data: ").append(EdidUtil.getDescriptorHex(desc[d]));
                break;
            case 0xfa:
                sb.append("\n  Standard Timing ID: ").append(EdidUtil.getDescriptorHex(desc[d]));
                break;
            default:
                if (EdidUtil.getDescriptorType(desc[d]) <= 0x0f && EdidUtil.getDescriptorType(desc[d]) >= 0x00) {
                    sb.append("\n  Manufacturer Data: ").append(EdidUtil.getDescriptorHex(desc[d]));
                } else {
                    sb.append("\n  Preferred Timing: ").append(EdidUtil.getTimingDescriptor(desc[d]));
                }
            }
        }
        return sb.toString();
    }
}