package oshi.hardware.platform.mac;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GlobalMemory;

@ThreadSafe
public final class MacHardwareAbstractionLayerJNA extends MacHardwareAbstractionLayer {
    @Override
    public GlobalMemory createMemory() {
        return new MacGlobalMemoryJNA();
    }


}
