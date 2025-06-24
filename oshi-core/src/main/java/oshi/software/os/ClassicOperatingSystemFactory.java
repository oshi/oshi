package oshi.software.os;

public class ClassicOperatingSystemFactory implements OperatingSystemFactory{
    @Override
    public boolean isSupportedOnThisJdk() {
        return true;
    }
}
