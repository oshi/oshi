package oshi.hardware.platform.mac;

import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
final class MacVirtualMemoryJNA extends MacVirtualMemory {
    MacVirtualMemoryJNA(MacGlobalMemoryJNA macGlobalMemory) {
        super(macGlobalMemory);
    }
}
