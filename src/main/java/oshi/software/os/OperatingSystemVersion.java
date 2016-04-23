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
package oshi.software.os;

import oshi.json.OshiJsonObject;

public interface OperatingSystemVersion extends OshiJsonObject {
    /**
     * @return the version
     */
    public String getVersion();

    /**
     * @param version
     *            the version to set
     */
    public void setVersion(String version);

    /**
     * @return the codeName
     */
    public String getCodeName();

    /**
     * @param codeName
     *            the codeName to set
     */
    public void setCodeName(String codeName);

    /**
     * @return the build number
     */
    public String getBuildNumber();

    /**
     * @param buildNumber
     *            the build number to set
     */
    public void setBuildNumber(String buildNumber);
}
