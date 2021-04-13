/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.software.os;

import oshi.annotation.concurrent.Immutable;

/**
 * Operating system services are responsible for the management of platform
 * resources, including the processor, memory, files, and input and output. They
 * generally shield applications from the implementation details of the machine.
 * <p>
 * This class is provided for information purposes only. Interpretation of the
 * meaning of services is platform-dependent.
 */
@Immutable
public class OSService {

    private final String name;
    private final int processID;
    private final State state;

    /**
     * Service Execution States
     */
    public enum State {
        RUNNING, STOPPED, OTHER
    }

    /**
     * Instantiate a new {@link OSService}.
     *
     * @param name
     *            The service name.
     * @param processID
     *            The process ID if running, or 0 if stopped.
     * @param state
     *            The service {@link State}.
     */
    public OSService(String name, int processID, State state) {
        this.name = name;
        this.processID = processID;
        this.state = state;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return Returns the name of the service.
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * Getter for the field <code>processID</code>.
     * </p>
     *
     * @return Returns the processID.
     */
    public int getProcessID() {
        return this.processID;
    }

    /**
     * <p>
     * Getter for the field <code>state</code>.
     * </p>
     *
     * @return Returns the state of the service.
     */
    public State getState() {
        return this.state;
    }

}