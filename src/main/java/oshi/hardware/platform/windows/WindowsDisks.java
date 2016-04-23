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
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.List;

import oshi.hardware.common.AbstractDisks;
import oshi.hardware.common.HWDiskStore;

/**
 * Windows hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class WindowsDisks extends AbstractDisks {

    @Override
    public HWDiskStore[] getDisks() {
        List<HWDiskStore> result;

        result = new ArrayList<>();

        // TODO: extract disks hardware information
        return result.toArray(new HWDiskStore[result.size()]);
    }
}
