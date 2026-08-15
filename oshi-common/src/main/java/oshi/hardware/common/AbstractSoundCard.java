/*
 * Copyright 2018-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.util.ParseUtil;

/**
 * An abstract Sound Card
 */
@Immutable
public abstract class AbstractSoundCard implements SoundCard {

    private final String kernelVersion;
    private final String name;
    private final String codec;

    /**
     * Abstract Sound Card Constructor
     *
     * @param kernelVersion The version, or {@code null} if the platform did not report one
     * @param name          The name
     * @param codec         The codec, or {@code null} if the platform did not report one
     */
    protected AbstractSoundCard(@Nullable String kernelVersion, String name, @Nullable String codec) {
        this.kernelVersion = ParseUtil.getStringValueOrUnknown(kernelVersion);
        this.name = name;
        this.codec = ParseUtil.getStringValueOrUnknown(codec);
    }

    @Override
    public String getDriverVersion() {
        return this.kernelVersion;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getCodec() {
        return this.codec;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SoundCard@");
        builder.append(Integer.toHexString(hashCode()));
        builder.append(" [name=");
        builder.append(this.name);
        builder.append(", kernelVersion=");
        builder.append(this.kernelVersion);
        builder.append(", codec=");
        builder.append(this.codec);
        builder.append(']');
        return builder.toString();
    }

}
