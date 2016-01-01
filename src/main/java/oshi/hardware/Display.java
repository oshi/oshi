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
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware;

/**
 * Display refers to the information regarding a video source and monitor
 * identified by the EDID standard.
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface Display {
    /**
     * The EDID byte array.
     * 
     * @return The original unparsed EDID byte array.
     */
    byte[] getEdid();
}
