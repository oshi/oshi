package oshi.software.os;

public interface OperatingSystemFactory {
    /**
     * Returns true if this factory supports the current JDK.
     */
    boolean isSupportedOnThisJdk();
}
