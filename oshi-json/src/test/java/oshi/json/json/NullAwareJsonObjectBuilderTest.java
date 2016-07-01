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

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.Test;

public class NullAwareJsonObjectBuilderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testWrapNull() {
        NullAwareJsonObjectBuilder.wrap(null);
    }

    @Test
    public void testBuilder() {
        JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder());

        json.addNull("null");
        json.add("nullValue", JsonValue.NULL);
        json.add("false", JsonValue.FALSE);

        json.add("nullString", (String) null);
        json.add("string", "String");

        json.add("nullBigInt", (BigInteger) null);
        json.add("big1", BigInteger.ONE);

        json.add("nullBigDec", (BigDecimal) null);
        json.add("big0.42", BigDecimal.valueOf(0.42));

        json.add("int", 42);
        json.add("long", 4200L);
        json.add("double", 42d);
        json.add("boolean", true);

        JsonArrayBuilder arrayBuilder = jsonFactory.createArrayBuilder();
        JsonObjectBuilder jsonA = NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder());
        JsonObject zero = jsonA.add("zero", 0).build();
        arrayBuilder.add(zero);
        json.add("zeroArray", arrayBuilder);
        json.add("zeroObject", zero);

        JsonObject obj = json.build();

        assertEquals(JsonValue.NULL, obj.get("null"));
        assertEquals(JsonValue.NULL, obj.get("nullValue"));
        assertEquals(JsonValue.FALSE, obj.get("false"));

        assertEquals(JsonValue.NULL, obj.get("nullString"));
        assertEquals(JsonValue.ValueType.STRING, obj.get("string").getValueType());
        assertEquals("\"String\"", obj.get("string").toString());

        assertEquals(JsonValue.NULL, obj.get("nullBigInt"));
        assertEquals(JsonValue.ValueType.NUMBER, obj.get("big1").getValueType());
        assertEquals("1", obj.get("big1").toString());

        assertEquals(JsonValue.NULL, obj.get("nullBigDec"));
        assertEquals(JsonValue.ValueType.NUMBER, obj.get("big0.42").getValueType());
        assertEquals("0.42", obj.get("big0.42").toString());

        assertEquals(JsonValue.ValueType.NUMBER, obj.get("int").getValueType());
        assertEquals("42", obj.get("int").toString());
        assertEquals(JsonValue.ValueType.NUMBER, obj.get("long").getValueType());
        assertEquals("4200", obj.get("long").toString());
        assertEquals(JsonValue.ValueType.NUMBER, obj.get("double").getValueType());
        assertEquals("42.0", obj.get("double").toString());
        assertEquals(JsonValue.ValueType.TRUE, obj.get("boolean").getValueType());
        assertEquals("true", obj.get("boolean").toString());
        assertEquals(JsonValue.ValueType.ARRAY, obj.get("zeroArray").getValueType());
        assertEquals("[{\"zero\":0}]", obj.get("zeroArray").toString());
        assertEquals(JsonValue.ValueType.OBJECT, obj.get("zeroObject").getValueType());
        assertEquals("{\"zero\":0}", obj.get("zeroObject").toString());

    }
}
