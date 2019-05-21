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
package oshi.hardware.common;

import static oshi.SystemInfo.UNKNOWN;
import static oshi.util.StringUtil.isBlank;
import oshi.hardware.Baseboard;

/**
 * Baseboard data. This implementation is immutable.
 *
 * @author widdis [at] gmail [dot] com
 */
public abstract class AbstractBaseboard implements Baseboard {

    private static final long serialVersionUID = 1L;

    private final String manufacturer;
    private final String model;
    private final String version;
    private final String serialNumber;

    public AbstractBaseboard() {
        this(null);
    }

    public AbstractBaseboard(BaseboardInitializer initializer ) {
        if (initializer == null) {
            initializer = getInitializer();
        }

        if (initializer != null) {
            manufacturer = isBlank(initializer.manufacturer) ? UNKNOWN : initializer.manufacturer;
            model = isBlank(initializer.model) ? UNKNOWN : initializer.model;
            version = isBlank(initializer.version) ? UNKNOWN : initializer.version;
            serialNumber = isBlank(initializer.serialNumber) ? "" : initializer.serialNumber;
        } else {
            manufacturer = UNKNOWN;
            model = UNKNOWN;
            version = UNKNOWN;
            serialNumber = "";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        return this.manufacturer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        return this.model;
    }

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
    public String getSerialNumber() {
        return this.serialNumber;
    }

    protected abstract BaseboardInitializer getInitializer();

    public static class BaseboardInitializer {
        public String manufacturer;
        public String model;
        public String version;
        public String serialNumber;
    }
}
