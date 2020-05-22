/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;

/**
 * Common fields or methods used by platform-specific implementations of
 * HardwareAbstractionLayer
 */
@ThreadSafe
public abstract class AbstractHardwareAbstractionLayer implements HardwareAbstractionLayer {

    private final Supplier<ComputerSystem> computerSystem = memoize(this::createComputerSystem);

    private final Supplier<CentralProcessor> processor = memoize(this::createProcessor);

    private final Supplier<GlobalMemory> memory = memoize(this::createMemory);

    private final Supplier<Sensors> sensors = memoize(this::createSensors);

    @Override
    public ComputerSystem getComputerSystem() {
        return computerSystem.get();
    }

    /**
     * Instantiates the platform-specific {@link ComputerSystem} object
     *
     * @return platform-specific {@link ComputerSystem} object
     */
    protected abstract ComputerSystem createComputerSystem();

    @Override
    public CentralProcessor getProcessor() {
        return processor.get();
    }

    /**
     * Instantiates the platform-specific {@link CentralProcessor} object
     *
     * @return platform-specific {@link CentralProcessor} object
     */
    protected abstract CentralProcessor createProcessor();

    @Override
    public GlobalMemory getMemory() {
        return memory.get();
    }

    /**
     * Instantiates the platform-specific {@link GlobalMemory} object
     *
     * @return platform-specific {@link GlobalMemory} object
     */
    protected abstract GlobalMemory createMemory();

    @Override
    public Sensors getSensors() {
        return sensors.get();
    }

    /**
     * Instantiates the platform-specific {@link Sensors} object
     *
     * @return platform-specific {@link Sensors} object
     */
    protected abstract Sensors createSensors();
}
