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
package oshi.json.hardware;

import oshi.json.json.OshiJsonObject;

/**
 * Display refers to the information regarding a video source and monitor
 * identified by the EDID standard.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Display extends OshiJsonObject {
    /**
     * The EDID byte array.
     *
     * @return The original unparsed EDID byte array.
     */
    byte[] getEdid();
}
