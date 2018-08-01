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
package oshi.json.util;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

/**
 * Formatting utility for json strings
 *
 * @author widdis[at]gmail[dot]com
 */
public class JsonUtil {
    private JsonUtil() {
    }

    /**
     * Pretty print a JSON string.
     *
     * @param json
     *            A JSON object
     * @return String representing the object with added whitespace, new lines,
     *         indentation
     */
    public static String jsonPrettyPrint(JsonObject json) {
        // Pretty printing using JsonWriterFactory
        // Output stream
        StringWriter stringWriter = new StringWriter();
        // Config
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        // Writer
        try (JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)) {
            jsonWriter.write(json);
        }
        // Return
        return stringWriter.toString();
    }
}
