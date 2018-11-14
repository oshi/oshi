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
package oshi.software.os;

import java.io.Serializable;

public interface OperatingSystemVersion extends Serializable {
    /**
     * @return the version
     */
    String getVersion();

    /**
     * @param version
     *            the version to set
     */
    void setVersion(String version);

    /**
     * @return the codeName
     */
    String getCodeName();

    /**
     * @param codeName
     *            the codeName to set
     */
    void setCodeName(String codeName);

    /**
     * @return the build number
     */
    String getBuildNumber();

    /**
     * @param buildNumber
     *            the build number to set
     */
    void setBuildNumber(String buildNumber);
}
