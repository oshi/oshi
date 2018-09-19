package oshi.hardware;

public interface PowerSourceUpdated {

    String getName();

    String getVendor();

    String getModelName();

    double getRemainingPercentage();

    double getDischargeTime();


}
