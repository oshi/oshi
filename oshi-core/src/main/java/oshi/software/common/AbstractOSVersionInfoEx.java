/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.common;

import oshi.software.os.OperatingSystemVersion;

/**
 * Contains operating system version information. The information includes major
 * and minor version numbers, a build number, a platform identifier, and
 * descriptive text about the operating system.
 */
public class AbstractOSVersionInfoEx implements OperatingSystemVersion {

    private static final long serialVersionUID = 1L;

    protected String version;

    protected String codeName;

    protected String versionStr;

    protected String buildNumber;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return this.version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCodeName() {
        return this.codeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCodeName(String codeName) {
        this.codeName = codeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBuildNumber() {
        return this.buildNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    @Override
    public String toString() {
        if (this.versionStr == null) {
            StringBuilder sb = new StringBuilder(getVersion() != null ? getVersion() : "Unknown");
            if (getCodeName().length() > 0) {
                sb.append(" (").append(getCodeName()).append(')');
            }
            if (getBuildNumber().length() > 0) {
                sb.append(" build ").append(getBuildNumber());
            }
            this.versionStr = sb.toString();
        }
        return this.versionStr;
    }
}
