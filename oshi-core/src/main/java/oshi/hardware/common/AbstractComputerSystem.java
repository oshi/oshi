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

import oshi.hardware.Baseboard;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Firmware;

/**
 * Computer System data.
 */
public abstract class AbstractComputerSystem implements ComputerSystem {

    private static final long serialVersionUID = 1L;

    protected volatile String manufacturer;

    protected volatile String model;

    protected volatile String serialNumber;

    private volatile Firmware firmware;

    private volatile Baseboard baseboard;

    /** {@inheritDoc} */
    @Override
    public Firmware getFirmware() {
        Firmware localRef = this.firmware;
        if (localRef == null) {
            synchronized (this) {
                localRef = this.firmware;
                if (localRef == null) {
                    this.firmware = localRef = createFirmware();
                }
            }
        }
        return localRef;
    }

    /**
     * Instantiates the platform-specific {@link Firmware} object
     * 
     * @return platform-specific {@link Firmware} object
     */
    protected abstract Firmware createFirmware();

    /** {@inheritDoc} */
    @Override
    public Baseboard getBaseboard() {
        Baseboard localRef = this.baseboard;
        if (localRef == null) {
            synchronized (this) {
                localRef = this.baseboard;
                if (localRef == null) {
                    this.baseboard = localRef = createBaseboard();
                }
            }
        }
        return localRef;
    }

    /**
     * Instantiates the platform-specific {@link Baseboard} object
     * 
     * @return platform-specific {@link Baseboard} object
     */
    protected abstract Baseboard createBaseboard();

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("manufacturer=").append(getManufacturer()).append(", ");
        sb.append("model=").append(getModel()).append(", ");
        sb.append("serial number=").append(getSerialNumber());
        return sb.toString();
    }
}
