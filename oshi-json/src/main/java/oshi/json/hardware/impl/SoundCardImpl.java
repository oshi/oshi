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
package oshi.json.hardware.impl;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.hardware.SoundCard;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement SoundCard interface with platform-specific objects
 */
public class SoundCardImpl extends AbstractOshiJsonObject implements SoundCard {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.SoundCard soundCard;

    /**
     * Creates a new platform-specific SoundCard object wrapping the provided
     * argument
     *
     * @param soundCard
     *            a platform-specific SoundCard object
     */
    public SoundCardImpl(oshi.hardware.SoundCard soundCard) {
        this.soundCard = soundCard;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDriverVersion() {
        return this.soundCard.getDriverVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.soundCard.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCodec() {
        return this.soundCard.getCodec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.soundCards.driverVersion")) {
            json.add("driverVersion", getDriverVersion());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.soundCards.name")) {
            json.add("name", getName());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.soundCards.codec")) {
            json.add("vendor", getCodec());
        }
        return json.build();
    }

    @Override
    public String toString() {
        return this.soundCard.toString();
    }
}
