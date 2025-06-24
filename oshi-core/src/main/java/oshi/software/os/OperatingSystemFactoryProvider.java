package oshi.software.os;

import java.util.ServiceLoader;

public class OperatingSystemFactoryProvider {
    private static final OperatingSystemFactory INSTANCE = loadInstance();

    private static OperatingSystemFactory loadInstance() {
        ServiceLoader<OperatingSystemFactory> loader = ServiceLoader.load(OperatingSystemFactory.class);
        for (OperatingSystemFactory factory : loader) {
            if (factory.isSupportedOnThisJdk()) {
                return factory;
            }
        }
        throw new IllegalStateException("No compatible OperatingSystemFactory found");
    }

    public static OperatingSystemFactory getInstance() {
        return INSTANCE;
    }
}
