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

package oshi.software.os;

import java.io.Serializable;

public class OSService implements Serializable {

	private String name = "";
	private int processID;
	private State state = State.OTHER;

    /**
     * Service Execution Statuses
     */
    public enum State {
        RUNNING, STOPPED, OTHER
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
     * Getter for the field <code>status</code>.
     * </p>
     *
     * @return Returns the status of the service.
     */
    public Status getState() {
    	return this.state;
    }

    /**
     * Set the name of the service.
     *
     * @param name
     *            service name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the processID of the service.
     *
     * @param processID
     *            process ID
     */
    public void setProcessID(int processID) {
        this.processID = processID;
    }

    /**
     * Set the status of the service.
     *
     * @param status
     *            status
     */
    public void setState(Status state) {
        this.state = state;
    }
}