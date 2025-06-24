package oshi.software.os;

import oshi.software.os.OperatingSystemFactory;

public class FfmOperatingSystemFactory implements OperatingSystemFactory{
    @Override
    public boolean isSupportedOnThisJdk() {
        String version = System.getProperty("java.specification.version");
        try {
            int v = Integer.parseInt(version);
            return v >= 24;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
