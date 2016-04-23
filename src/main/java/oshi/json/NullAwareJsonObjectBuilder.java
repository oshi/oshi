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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.json;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * Decorator class for JsonObjectBuilder that handles null values properly
 */
public class NullAwareJsonObjectBuilder implements JsonObjectBuilder {
    // Use the Factory Pattern to create an instance.
    public static JsonObjectBuilder wrap(JsonObjectBuilder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Can't wrap nothing.");
        }
        return new NullAwareJsonObjectBuilder(builder);
    }

    // Decorated object per Decorator Pattern.
    private final JsonObjectBuilder builder;

    private NullAwareJsonObjectBuilder(JsonObjectBuilder builder) {
        this.builder = builder;
    }

    @Override
    public JsonObjectBuilder add(String arg0, JsonValue arg1) {
        if (arg1 == null) {
            builder.addNull(arg0);
        } else {
            builder.add(arg0, arg1);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(String arg0, String arg1) {
        if (arg1 == null) {
            builder.addNull(arg0);
        } else {
            builder.add(arg0, arg1);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(String arg0, BigInteger arg1) {
        if (arg1 == null) {
            builder.addNull(arg0);
        } else {
            builder.add(arg0, arg1);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(String arg0, BigDecimal arg1) {
        if (arg1 == null) {
            builder.addNull(arg0);
        } else {
            builder.add(arg0, arg1);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(String arg0, int arg1) {
        builder.add(arg0, arg1);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String arg0, long arg1) {
        builder.add(arg0, arg1);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String arg0, double arg1) {
        builder.add(arg0, arg1);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String arg0, boolean arg1) {
        builder.add(arg0, arg1);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String arg0, JsonObjectBuilder arg1) {
        if (arg1 == null) {
            builder.addNull(arg0);
        } else {
            builder.add(arg0, arg1);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(String arg0, JsonArrayBuilder arg1) {
        if (arg1 == null) {
            builder.addNull(arg0);
        } else {
            builder.add(arg0, arg1);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder addNull(String arg0) {
        builder.addNull(arg0);
        return this;
    }

    @Override
    public JsonObject build() {
        return builder.build();
    }
}
