/*
 * Copyright (c) 2016 com.github.dblock.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * enrico[dot]bianchi[at]gmail[dot]com
 *    com.github.dblock - initial API and implementation and/or initial documentation
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.hardware.HWDiskStore;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.json.OshiJsonObject;

/**
 * Mac hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class MacDisks implements OshiJsonObject {

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);
    private static final Logger LOG = LoggerFactory.getLogger(MacDisks.class);

    public List<HWDiskStore> getDisks() {
        List<HWDiskStore> result;

        result = new ArrayList<>();
        
        //TODO: extract disks hardware information
        
        return result;
    }

    @Override

    public JsonObject toJSON() {

        JsonArrayBuilder array = jsonFactory.createArrayBuilder();

        for (HWDiskStore store : getDisks()) {
            array.add(store.toJSON());
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("disks", array).build();
    }
}
