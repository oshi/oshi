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
package oshi.hardware.common;

import java.util.Arrays;

import oshi.hardware.Display;
import oshi.util.EdidUtil;

/**
 * A Display
 */
public abstract class AbstractDisplay implements Display {

    private static final long serialVersionUID = 1L;

    protected byte[] edid;

    protected AbstractDisplay(byte[] edid) {
        this.edid = edid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getEdid() {
        return Arrays.copyOf(this.edid, this.edid.length);
    }

    @Override
    public String toString() {
        return EdidUtil.toString(this.edid);
    }
}