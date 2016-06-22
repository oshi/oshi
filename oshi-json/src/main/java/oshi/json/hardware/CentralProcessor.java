/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.json.hardware;

import oshi.json.json.OshiJsonObject;
import oshi.json.software.os.OSProcess;

/**
 * {@inheritDoc}
 */
public interface CentralProcessor extends oshi.hardware.CentralProcessor, OshiJsonObject {
    /**
     * {@inheritDoc}
     */
    String getVendor();

    /**
     * {@inheritDoc}
     */
    void setVendor(String vendor);

    /**
     * {@inheritDoc}
     */
    String getName();

    /**
     * {@inheritDoc}
     */
    void setName(String name);

    /**
     * {@inheritDoc}
     */
    long getVendorFreq();

    /**
     * {@inheritDoc}
     */
    void setVendorFreq(long freq);

    /**
     * {@inheritDoc}
     */
    String getIdentifier();

    /**
     * {@inheritDoc}
     */
    void setIdentifier(String identifier);

    /**
     * {@inheritDoc}
     */
    boolean isCpu64bit();

    /**
     * {@inheritDoc}
     */
    void setCpu64(boolean cpu64);

    /**
     * {@inheritDoc}
     */
    String getStepping();

    /**
     * {@inheritDoc}
     */
    void setStepping(String stepping);

    /**
     * {@inheritDoc}
     */
    String getModel();

    /**
     * {@inheritDoc}
     */
    void setModel(String model);

    /**
     * {@inheritDoc}
     */
    String getFamily();

    /**
     * {@inheritDoc}
     */
    void setFamily(String family);

    /**
     * {@inheritDoc}
     */
    double getSystemCpuLoadBetweenTicks();

    /**
     * {@inheritDoc}
     */
    long[] getSystemCpuLoadTicks();

    /**
     * {@inheritDoc}
     */
    long getSystemIOWaitTicks();

    /**
     * {@inheritDoc}
     */
    long[] getSystemIrqTicks();

    /**
     * {@inheritDoc}
     */
    double getSystemCpuLoad();

    /**
     * {@inheritDoc}
     */
    double getSystemLoadAverage();

    /**
     * {@inheritDoc}
     */
    double[] getSystemLoadAverage(int nelem);

    /**
     * {@inheritDoc}
     */
    double[] getProcessorCpuLoadBetweenTicks();

    /**
     * {@inheritDoc}
     */
    long[][] getProcessorCpuLoadTicks();

    /**
     * {@inheritDoc}
     */
    long getSystemUptime();

    /**
     * {@inheritDoc}
     */
    String getSystemSerialNumber();

    /**
     * {@inheritDoc}
     */
    int getLogicalProcessorCount();

    /**
     * {@inheritDoc}
     */
    int getPhysicalProcessorCount();

    /**
     * {@inheritDoc}
     */
    OSProcess[] getProcesses();

    /**
     * {@inheritDoc}
     */
    OSProcess getProcess(int pid);

    /**
     * {@inheritDoc}
     */
    int getProcessId();

    /**
     * {@inheritDoc}
     */
    int getProcessCount();

    /**
     * {@inheritDoc}
     */
    int getThreadCount();
}
