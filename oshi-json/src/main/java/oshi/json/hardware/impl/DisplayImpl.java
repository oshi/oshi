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
package oshi.json.hardware.impl;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.hardware.Display;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.util.EdidUtil;

public class DisplayImpl implements Display {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.Display display;

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
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("edid", EdidUtil.toString(getEdid())).build();
    }

    @Override
    public String toString() {
        return this.display.toString();
    }
}
