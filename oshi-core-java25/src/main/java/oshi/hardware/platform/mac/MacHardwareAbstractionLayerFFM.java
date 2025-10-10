package oshi.hardware.platform.mac;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GlobalMemory;

@ThreadSafe
public final class MacHardwareAbstractionLayerFFM extends MacHardwareAbstractionLayer {
    @Override
    public GlobalMemory createMemory() {
        return new MacGlobalMemoryFFM();
    }


}
