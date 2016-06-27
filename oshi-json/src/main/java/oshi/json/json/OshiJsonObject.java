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

import java.io.Serializable;
import java.util.Properties;

import javax.json.JsonObject;

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
     * @return a {@link javax.json.JsonObject} for this class.
     */
    JsonObject toJSON(Properties properties);
}
