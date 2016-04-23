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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;

import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractDisks;

/**
 * Mac hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class MacDisks extends AbstractDisks {

    @Override
    public HWDiskStore[] getDisks() {
        List<HWDiskStore> result;

        result = new ArrayList<>();

        // TODO: extract disks hardware information
        return result.toArray(new HWDiskStore[result.size()]);
    }
}
