/*
 * Copyright 2018-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;

/**
 * SoundCard interface.
 */
@Immutable
public interface SoundCard {

    /**
     * Retrieves the driver version currently in use in machine
     *
     * @return The current and complete name of the driver version
     */
    String getDriverVersion();

    /**
     * Retrieves the full name of the card.
     *
     * @return The name of the card.
     */
    String getName();

    /**
     * Retrieves the codec of the Sound card
     *
     * @return The name of the codec of the sound card
     */
    String getCodec();

}
