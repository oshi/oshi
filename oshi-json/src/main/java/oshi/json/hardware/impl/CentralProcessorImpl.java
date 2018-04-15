/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.json.hardware.impl;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.hardware.CentralProcessor;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement CentralProcessor interface with platform-specific
 * objects
 */
public class CentralProcessorImpl extends AbstractOshiJsonObject implements CentralProcessor {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.CentralProcessor processor;

    /**
     * Creates a new platform-specific CentralProcessor object wrapping the
     * provided argument
     *
     * @param processor
     *            a platform-specific CentralProcessor object
     */
    public CentralProcessorImpl(oshi.hardware.CentralProcessor processor) {
        this.processor = processor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVendor() {
        return this.processor.getVendor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVendor(String vendor) {
        this.processor.setVendor(vendor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.processor.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setName(String name) {
        this.processor.setName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProcessorID() {
        return this.processor.getProcessorID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProcessorID(String processorID) {
        this.processor.setProcessorID(processorID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getVendorFreq() {
        return this.processor.getVendorFreq();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVendorFreq(long freq) {
        this.processor.setVendorFreq(freq);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentifier() {
        return this.processor.getIdentifier();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIdentifier(String identifier) {
        this.processor.setIdentifier(identifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCpu64bit() {
        return this.processor.isCpu64bit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCpu64(boolean cpu64) {
        this.processor.setCpu64(cpu64);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStepping() {
        return this.processor.getStepping();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStepping(String stepping) {
        this.processor.setStepping(stepping);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        return this.processor.getModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(String model) {
        this.processor.setModel(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFamily() {
        return this.processor.getFamily();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFamily(String family) {
        this.processor.setFamily(family);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getSystemCpuLoadBetweenTicks() {
        return this.processor.getSystemCpuLoadBetweenTicks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getSystemCpuLoadTicks() {
        return this.processor.getSystemCpuLoadTicks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getSystemCpuLoad() {
        return this.processor.getSystemCpuLoad();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getSystemLoadAverage() {
        return this.processor.getSystemLoadAverage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getSystemLoadAverage(int nelem) {
        return this.processor.getSystemLoadAverage(nelem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getProcessorCpuLoadBetweenTicks() {
        return this.processor.getProcessorCpuLoadBetweenTicks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[][] getProcessorCpuLoadTicks() {
        return this.processor.getProcessorCpuLoadTicks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        return this.processor.getSystemUptime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public String getSystemSerialNumber() {
        return this.processor.getSystemSerialNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLogicalProcessorCount() {
        return this.processor.getLogicalProcessorCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPhysicalProcessorCount() {
        return this.processor.getPhysicalProcessorCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPhysicalPackageCount() {
        return this.processor.getPhysicalPackageCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getContextSwitches() {
        return this.processor.getContextSwitches();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInterrupts() {
        return this.processor.getInterrupts();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.name")) {
            json.add("name", getName());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.physicalPackageCount")) {
            json.add("physicalPackageCount", getPhysicalPackageCount());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.physicalProcessorCount")) {
            json.add("physicalProcessorCount", getPhysicalProcessorCount());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.logicalProcessorCount")) {
            json.add("logicalProcessorCount", getLogicalProcessorCount());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.vendor")) {
            json.add("vendor", getVendor());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.vendorFreq")) {
            json.add("vendorFreq", getVendorFreq());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.processorID")) {
            json.add("processorID", getProcessorID());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.identifier")) {
            json.add("identifier", getIdentifier());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.cpu64bit")) {
            json.add("cpu64bit", isCpu64bit());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.family")) {
            json.add("family", getFamily());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.model")) {
            json.add("model", getModel());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.stepping")) {
            json.add("stepping", getStepping());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.systemCpuLoadBetweenTicks")) {
            json.add("systemCpuLoadBetweenTicks", getSystemCpuLoadBetweenTicks());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.systemCpuLoadTicks")) {
            JsonArrayBuilder systemCpuLoadTicksArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (long ticks : getSystemCpuLoadTicks()) {
                systemCpuLoadTicksArrayBuilder.add(ticks);
            }
            json.add("systemCpuLoadTicks", systemCpuLoadTicksArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.systemCpuLoad")) {
            json.add("systemCpuLoad", getSystemCpuLoad());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.systemLoadAverage")) {
            json.add("systemLoadAverage", getSystemLoadAverage());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.systemLoadAverages")) {
            JsonArrayBuilder systemLoadAverageArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (double avg : getSystemLoadAverage(3)) {
                systemLoadAverageArrayBuilder.add(avg);
            }
            json.add("systemLoadAverages", systemLoadAverageArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.processorCpuLoadBetweenTicks")) {
            JsonArrayBuilder processorCpuLoadBetweenTicksArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (double load : getProcessorCpuLoadBetweenTicks()) {
                processorCpuLoadBetweenTicksArrayBuilder.add(load);
            }
            json.add("processorCpuLoadBetweenTicks", processorCpuLoadBetweenTicksArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.processorCpuLoadTicks")) {
            JsonArrayBuilder processorCpuLoadTicksArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (long[] procTicks : getProcessorCpuLoadTicks()) {
                JsonArrayBuilder processorTicksArrayBuilder = this.jsonFactory.createArrayBuilder();
                for (long ticks : procTicks) {
                    processorTicksArrayBuilder.add(ticks);
                }
                processorCpuLoadTicksArrayBuilder.add(processorTicksArrayBuilder.build());
            }
            json.add("processorCpuLoadTicks", processorCpuLoadTicksArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.systemUptime")) {
            json.add("systemUptime", getSystemUptime());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.contextSwitches")) {
            json.add("contextSwitches", getContextSwitches());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.processor.interrupts")) {
            json.add("interrupts", getInterrupts());
        }
        return json.build();
    }

    @Override
    public String toString() {
        return this.processor.toString();
    }
}
