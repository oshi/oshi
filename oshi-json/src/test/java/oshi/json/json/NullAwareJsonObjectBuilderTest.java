/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

        String nullString = null;
        json.add("nullString", nullString);
        json.add("string", "String");

        BigInteger nullBigInt = null;
        json.add("nullBigInt", nullBigInt);
        json.add("big1", BigInteger.ONE);

        BigDecimal nullBigDec = null;
        json.add("nullBigDec", nullBigDec);
        json.add("big0.42", BigDecimal.valueOf(0.42));

        json.add("int", 42);
        json.add("long", 4200L);
        json.add("double", 42d);
        json.add("boolean", true);
        json.add("nan", Double.NaN);
        json.add("inf", Double.POSITIVE_INFINITY);
        json.add("-inf", Double.NEGATIVE_INFINITY);

        JsonArrayBuilder arrayBuilder = null;
        json.add("nullArray", arrayBuilder);

        arrayBuilder = jsonFactory.createArrayBuilder();
        JsonObjectBuilder jsonA = null;
        json.add("nullBuilder", jsonA);

        jsonA = NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder());
        JsonObject zero = null;
        json.add("nullObject", zero);

        zero = jsonA.add("zero", 0).build();
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

        assertEquals(JsonValue.ValueType.STRING, obj.get("nan").getValueType());
        assertEquals("\"NaN\"", obj.get("nan").toString());
        assertEquals(JsonValue.ValueType.STRING, obj.get("inf").getValueType());
        assertEquals("\"Infinity\"", obj.get("inf").toString());
        assertEquals(JsonValue.ValueType.STRING, obj.get("-inf").getValueType());
        assertEquals("\"-Infinity\"", obj.get("-inf").toString());

        assertEquals(JsonValue.ValueType.ARRAY, obj.get("zeroArray").getValueType());
        assertEquals("[{\"zero\":0}]", obj.get("zeroArray").toString());

        assertEquals(JsonValue.ValueType.OBJECT, obj.get("zeroObject").getValueType());
        assertEquals("{\"zero\":0}", obj.get("zeroObject").toString());

        assertEquals(JsonValue.NULL, obj.get("nullArray"));
        assertEquals(JsonValue.NULL, obj.get("nullObject"));
        assertEquals(JsonValue.NULL, obj.get("nullBuilder"));
    }
}
