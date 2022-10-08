/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;

/**
 * An abstract Sound Card
 */
@Immutable
public abstract class AbstractGraphicsCard implements GraphicsCard {

    private final String name;
    private final String deviceId;
    private final String vendor;
    private final String versionInfo;
    private long vram;

    /**
     * Constructor for AbstractGraphicsCard
     *
     * @param name        The name
     * @param deviceId    The device ID
     * @param vendor      The vendor
     * @param versionInfo The version info
     * @param vram        The VRAM
     */
    protected AbstractGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        this.name = name;
        this.deviceId = deviceId;
        this.vendor = vendor;
        this.versionInfo = versionInfo;
        this.vram = vram;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    @Override
    public String getVersionInfo() {
        return versionInfo;
    }

    @Override
    public long getVRam() {
        return vram;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GraphicsCard@");
        builder.append(Integer.toHexString(hashCode()));
        builder.append(" [name=");
        builder.append(this.name);
        builder.append(", deviceId=");
        builder.append(this.deviceId);
        builder.append(", vendor=");
        builder.append(this.vendor);
        builder.append(", vRam=");
        builder.append(this.vram);
        builder.append(", versionInfo=[");
        builder.append(this.versionInfo);
        builder.append("]]");
        return builder.toString();
    }
}
