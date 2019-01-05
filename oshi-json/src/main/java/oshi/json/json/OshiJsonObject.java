/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
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

import java.io.Serializable;
import java.util.Properties;

import javax.json.JsonObject;

/**
 * Public interface functionality for JSON strings
 */
public interface OshiJsonObject extends Serializable {
    /**
     * Creates a compact JSON string containing the information for this class.
     * The properties set by {@link #toCompactJSON(Properties)} configure the
     * type of information returned.
     *
     * @return a compact JSON string
     */
    String toCompactJSON();

    /**
     * Creates a compact JSON string containing the information for this class.
     * The provided properties will be set on this object to configure the type
     * of information returned using this and future invocations of
     * {@link #toCompactJSON()}.
     *
     * @param properties
     *            Properties to configure returned results
     * @return a compact JSON string
     */
    String toCompactJSON(Properties properties);

    /**
     * Creates a pretty JSON string containing the information for this class.
     * The properties set by {@link #toPrettyJSON(Properties)} configure the
     * type of information returned.
     *
     * @return a pretty JSON string
     */
    String toPrettyJSON();

    /**
     * Creates a pretty JSON string containing the information for this class.
     * The provided properties will be set on this object to configure the type
     * of information returned using this and future invocations of
     * {@link #toPrettyJSON()}.
     *
     * @param properties
     *            Properties to configure returned results
     * @return a pretty JSON string
     */
    String toPrettyJSON(Properties properties);

    /**
     * Returns this object as a {@link javax.json.JsonObject}. The properties
     * set by {@link #toJSON(Properties)} configure the type of information
     * returned.
     *
     * @return a {@link javax.json.JsonObject} for this class.
     */
    JsonObject toJSON();

    /**
     * Returns this object as a {@link javax.json.JsonObject}. The provided
     * properties will be set on this object to configure the type of
     * information returned using this and future invocations of
     * {@link #toJSON()}.
     *
     * @param properties
     *            Properties to configure returned results
     * @return a {@link javax.json.JsonObject} for this class.
     */
    JsonObject toJSON(Properties properties);
}
