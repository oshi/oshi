package oshi.hardware.common;

import oshi.hardware.PowerSourceUpdated;

public abstract class AbstractPowerSourceUpdated implements PowerSourceUpdated {

    private String name;
    private String vendor;
    private String model;
    private double remainingPercentage;
    private double dischargeTime;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public double getRemainingPercentage() {
        return remainingPercentage;
    }

    public void setRemainingPercentage(double remainingPercentage) {
        this.remainingPercentage = remainingPercentage;
    }

    @Override
    public double getDischargeTime() {
        return dischargeTime;
    }

    public void setDischargeTime(double dischargeTime) {
        this.dischargeTime = dischargeTime;
    }


}
