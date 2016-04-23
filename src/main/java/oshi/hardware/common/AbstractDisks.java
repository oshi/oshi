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
 * Contributors:
 * enrico[dot]bianchi[at]gmail[dot]com
 *    com.github.dblock - initial API and implementation and/or initial documentation
 */
package oshi.hardware.common;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.hardware.Disks;
import oshi.json.NullAwareJsonObjectBuilder;

/**
 * Linux hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public abstract class AbstractDisks implements Disks {
    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    @Override
    public abstract HWDiskStore[] getDisks();

    @Override
    public JsonObject toJSON() {
        JsonArrayBuilder diakArray = jsonFactory.createArrayBuilder();
        for (HWDiskStore store : getDisks()) {
            diakArray.add(store.toJSON());
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("disks", diakArray).build();
    }
}
