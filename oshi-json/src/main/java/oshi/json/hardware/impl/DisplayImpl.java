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

import oshi.json.hardware.Display;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;
import oshi.util.ParseUtil;

/**
 * Wrapper class to implement Display interface with platform-specific objects
 */
public class DisplayImpl extends AbstractOshiJsonObject implements Display {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.Display display;

    /**
     * Creates a new platform-specific Display object wrapping the provided
     * argument
     *
     * @param display
     *            a platform-specific Display object
     */
    public DisplayImpl(oshi.hardware.Display display) {
        this.display = display;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getEdid() {
        return this.display.getEdid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.displays.edid")) {
            json.add("edid", ParseUtil.byteArrayToHexString(getEdid()));
        }
        return json.build();
    }

    @Override
    public String toString() {
        return this.display.toString();
    }
}
