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

import java.util.Properties;

import javax.json.JsonObject;

import oshi.json.util.JsonUtil;

/**
 * Implements common methods for the OshiJsonObject interface
 */
public abstract class AbstractOshiJsonObject implements OshiJsonObject {

    private static final long serialVersionUID = 1L;

    private Properties properties = new Properties();

    /**
     * {@inheritDoc}
     */
    @Override
    public String toCompactJSON() {
        return toCompactJSON(this.properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toCompactJSON(Properties properties) {
        this.properties = properties;
        return toJSON(this.properties).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toPrettyJSON() {
        return toPrettyJSON(this.properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toPrettyJSON(Properties properties) {
        this.properties = properties;
        return JsonUtil.jsonPrettyPrint(toJSON(this.properties));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        return toJSON(this.properties);
    }

    @Override
    public String toString() {
        return toPrettyJSON();
    }
}
