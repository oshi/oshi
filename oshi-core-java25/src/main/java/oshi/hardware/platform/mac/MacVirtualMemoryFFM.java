package oshi.hardware.platform.mac;

import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
final class MacVirtualMemoryFFM extends MacVirtualMemory {
    MacVirtualMemoryFFM(MacGlobalMemoryFFM macGlobalMemory) {
        super(macGlobalMemory);
    }
}
