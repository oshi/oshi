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
package oshi.json.software.os;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.json.OshiJsonObject;

/**
 * {@inheritDoc}
 */
public class OSFileStore extends oshi.software.os.OSFileStore implements OshiJsonObject {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    /**
     * {@inheritDoc}
     */
    public OSFileStore(String newName, String newVolume, String newMount, String newDescription, String newType,
            String newUuid, long newUsableSpace, long newTotalSpace) {
        super(newName, newVolume, newMount, newDescription, newType, newUuid, newUsableSpace, newTotalSpace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("name", getName())
                .add("volume", getVolume()).add("mountPoint", getMount()).add("description", getDescription())
                .add("fsType", getType()).add("uuid", getUUID()).add("usableSpace", getUsableSpace())
                .add("totalSpace", getTotalSpace()).build();
    }
}
