/*
 * MIT License
 *
 * Copyright (c) 2019-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
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

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Firmware;

/**
 * Computer System data.
 */
@Immutable
public abstract class AbstractComputerSystem implements ComputerSystem {

    private final Supplier<Firmware> firmware = memoize(this::createFirmware);

    private final Supplier<Baseboard> baseboard = memoize(this::createBaseboard);

    @Override
    public Firmware getFirmware() {
        return firmware.get();
    }

    /**
     * Instantiates the platform-specific {@link Firmware} object
     *
     * @return platform-specific {@link Firmware} object
     */
    protected abstract Firmware createFirmware();

    @Override
    public Baseboard getBaseboard() {
        return baseboard.get();
    }

    /**
     * Instantiates the platform-specific {@link Baseboard} object
     *
     * @return platform-specific {@link Baseboard} object
     */
    protected abstract Baseboard createBaseboard();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("manufacturer=").append(getManufacturer()).append(", ");
        sb.append("model=").append(getModel()).append(", ");
        sb.append("serial number=").append(getSerialNumber()).append(", ");
        sb.append("uuid=").append(getHardwareUUID());
        return sb.toString();
    }
}
