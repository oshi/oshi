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
 * enrico[dot]bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.common;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.hardware.Networks;
import oshi.hardware.stores.HWNetworkStore;
import oshi.json.NullAwareJsonObjectBuilder;

/**
 * Network interfaces implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public abstract class AbstractNetworks implements Networks {
    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    @Override
    public abstract HWNetworkStore[] getNetworks();

    @Override
    public JsonObject toJSON() {
        JsonArrayBuilder netArray = jsonFactory.createArrayBuilder();
        for (HWNetworkStore store : getNetworks()) {
            netArray.add(store.toJSON());
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("networks", netArray).build();
    }
}
