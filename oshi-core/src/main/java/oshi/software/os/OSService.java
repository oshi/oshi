/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import oshi.annotation.concurrent.Immutable;

/**
 * Operating system services are responsible for the management of platform resources, including the processor, memory,
 * files, and input and output. They generally shield applications from the implementation details of the machine.
 * <p>
 * This class is provided for information purposes only. Interpretation of the meaning of services is
 * platform-dependent.
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
     * @param name      The service name.
     * @param processID The process ID if running, or 0 if stopped.
     * @param state     The service {@link State}.
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
