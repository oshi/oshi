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
package oshi.hardware;

import java.io.Serializable;

public class LogicalProcessor implements Serializable {

    private static final long serialVersionUID = 1L;

    private int processorNumber;
    private int physicalProcessorNumber;
    private int physicalPackageNumber;
    private long currentFrequency;

    /**
     * The Logical Processor number as seen by the Operating System. Used for
     * assigning process affinity and reporting CPU usage and other statistics.
     * 
     * @return the processorNumber
     */
    public int getProcessorNumber() {
        return processorNumber;
    }

    /**
     * @param processorNumber
     *            the processorNumber to set
     */
    public void setProcessorNumber(int processorNumber) {
        this.processorNumber = processorNumber;
    }

    /**
     * The physical processor (core) id number assigned to this logical
     * processor. Hyperthreaded logical processors which share the same physical
     * processor will have the same number.
     * 
     * @return the physicalProcessorNumber
     */
    public int getPhysicalProcessorNumber() {
        return physicalProcessorNumber;
    }

    /**
     * @param physicalProcessorNumber
     *            the physicalProcessorNumber to set
     */
    public void setPhysicalProcessorNumber(int physicalProcessorNumber) {
        this.physicalProcessorNumber = physicalProcessorNumber;
    }

    /**
     * The physical package (socket) id number assigned to this logical
     * processor. Multicore CPU packages may have multiple physical processors
     * which share the same number.
     * 
     * @return the physicalPackageNumber
     */
    public int getPhysicalPackageNumber() {
        return physicalPackageNumber;
    }

    /**
     * @param physicalPackageNumber
     *            the physicalPackageNumber to set
     */
    public void setPhysicalPackageNumber(int physicalPackageNumber) {
        this.physicalPackageNumber = physicalPackageNumber;
    }

    /**
     * The current operating frequency of this logical processor, if known.
     * Defaults to the vendor frequency.
     * 
     * @return the currentFrequency
     */
    public long getCurrentFrequency() {
        return currentFrequency;
    }

    /**
     * @param currentFrequency
     *            the currentFrequency to set
     */
    public void setCurrentFrequency(long currentFrequency) {
        this.currentFrequency = currentFrequency;
    }

}
