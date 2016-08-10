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
package oshi.json.json;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * Decorator class for {@link javax.json.JsonObjectBuilder} that handles null
 * values properly
 */
public class NullAwareJsonObjectBuilder implements JsonObjectBuilder {
    /*
     * Decorated object per Decorator Pattern.
     */
    private final JsonObjectBuilder builder;

    // Private constructor that wraps the builder
    private NullAwareJsonObjectBuilder(JsonObjectBuilder builder) {
        this.builder = builder;
    }

    /**
     * Wraps a {@link javax.json.JsonObjectBuilder}
     *
     * @param builder
     *            The builder to wrap
     * @return A new instance of this class, wrapping the specified builder
     */
    public static JsonObjectBuilder wrap(JsonObjectBuilder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Can't wrap nothing.");
        }
        return new NullAwareJsonObjectBuilder(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder add(String arg0, JsonValue arg1) {
        if (arg1 == null) {
            this.builder.addNull(arg0);
        } else {
            this.builder.add(arg0, arg1);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder add(String arg0, String arg1) {
        if (arg1 == null) {
            this.builder.addNull(arg0);
        } else {
            this.builder.add(arg0, arg1);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder add(String arg0, BigInteger arg1) {
        if (arg1 == null) {
            this.builder.addNull(arg0);
        } else {
            this.builder.add(arg0, arg1);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder add(String arg0, BigDecimal arg1) {
        if (arg1 == null) {
            this.builder.addNull(arg0);
        } else {
            this.builder.add(arg0, arg1);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder add(String arg0, int arg1) {
        this.builder.add(arg0, arg1);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder add(String arg0, long arg1) {
        this.builder.add(arg0, arg1);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder add(String arg0, double arg1) {
        this.builder.add(arg0, arg1);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder add(String arg0, boolean arg1) {
        this.builder.add(arg0, arg1);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder add(String arg0, JsonObjectBuilder arg1) {
        if (arg1 == null) {
            this.builder.addNull(arg0);
        } else {
            this.builder.add(arg0, arg1);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder add(String arg0, JsonArrayBuilder arg1) {
        if (arg1 == null) {
            this.builder.addNull(arg0);
        } else {
            this.builder.add(arg0, arg1);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObjectBuilder addNull(String arg0) {
        this.builder.addNull(arg0);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject build() {
        return this.builder.build();
    }
}
