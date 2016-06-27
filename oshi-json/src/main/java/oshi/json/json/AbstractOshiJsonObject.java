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

import java.util.Properties;

import javax.json.JsonObject;

import oshi.json.util.JsonUtil;

public abstract class AbstractOshiJsonObject implements OshiJsonObject {

    private static final long serialVersionUID = 1L;

    private Properties properties = new Properties();

    /**
     * {@inheritDoc}
     */
    public String toCompactJSON() {
        return toCompactJSON(this.properties);
    }

    /**
     * {@inheritDoc}
     */
    public String toCompactJSON(Properties properties) {
        this.properties = properties;
        return toJSON(this.properties).toString();
    }

    /**
     * {@inheritDoc}
     */
    public String toPrettyJSON() {
        return toPrettyJSON(this.properties);
    }

    /**
     * {@inheritDoc}
     */
    public String toPrettyJSON(Properties properties) {
        this.properties = properties;
        return JsonUtil.jsonPrettyPrint(toJSON(this.properties));
    }

    /**
     * {@inheritDoc}
     */
    public JsonObject toJSON() {
        return toJSON(this.properties);
    }
}
