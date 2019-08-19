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

import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;

/**
 * Common fields or methods used by platform-specific implementations of
 * HardwareAbstractionLayer
 */
public abstract class AbstractHardwareAbstractionLayer implements HardwareAbstractionLayer {

    private static final long serialVersionUID = 1L;

    private volatile ComputerSystem computerSystem;

    private volatile CentralProcessor processor;

    private volatile GlobalMemory memory;

    private volatile Sensors sensors;

    /** {@inheritDoc} */
    @Override
    public ComputerSystem getComputerSystem() {
        ComputerSystem localRef = this.computerSystem;
        if (localRef == null) {
            synchronized (this) {
                localRef = this.computerSystem;
                if (localRef == null) {
                    this.computerSystem = localRef = createComputerSystem();
                }
            }
        }
        return localRef;
    }

    /**
     * Instantiates the platform-specific {@link ComputerSystem} object
     * 
     * @return platform-specific {@link ComputerSystem} object
     */
    protected abstract ComputerSystem createComputerSystem();

    /** {@inheritDoc} */
    @Override
    public CentralProcessor getProcessor() {
        CentralProcessor localRef = this.processor;
        if (localRef == null) {
            synchronized (this) {
                localRef = this.processor;
                if (localRef == null) {
                    this.processor = localRef = createProcessor();
                }
            }
        }
        return localRef;
    }

    /**
     * Instantiates the platform-specific {@link CentralProcessor} object
     * 
     * @return platform-specific {@link CentralProcessor} object
     */
    protected abstract CentralProcessor createProcessor();

    /** {@inheritDoc} */
    @Override
    public GlobalMemory getMemory() {
        GlobalMemory localRef = this.memory;
        if (localRef == null) {
            synchronized (this) {
                localRef = this.memory;
                if (localRef == null) {
                    this.memory = localRef = createMemory();
                }
            }
        }
        return localRef;
    }

    /**
     * Instantiates the platform-specific {@link GlobalMemory} object
     * 
     * @return platform-specific {@link GlobalMemory} object
     */
    protected abstract GlobalMemory createMemory();

    /** {@inheritDoc} */
    @Override
    public Sensors getSensors() {
        Sensors localRef = this.sensors;
        if (localRef == null) {
            synchronized (this) {
                localRef = this.sensors;
                if (localRef == null) {
                    this.sensors = localRef = createSensors();
                }
            }
        }
        return localRef;
    }

    /**
     * Instantiates the platform-specific {@link Sensors} object
     * 
     * @return platform-specific {@link Sensors} object
     */
    protected abstract Sensors createSensors();
}
