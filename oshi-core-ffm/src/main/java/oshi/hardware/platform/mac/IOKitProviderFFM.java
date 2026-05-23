/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.function.Consumer;
import java.util.function.Function;

import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.ffm.util.platform.mac.IOKitUtilFFM;
import oshi.hardware.common.platform.mac.IOKitProvider;

/**
 * FFM-based {@link IOKitProvider} implementation.
 */
final class IOKitProviderFFM implements IOKitProvider {

    static final IOKitProviderFFM INSTANCE = new IOKitProviderFFM();

    private IOKitProviderFFM() {
    }

    @Override
    public <T> T withMatchingService(String serviceName, Function<RegistryEntry, T> extractor) {
        IORegistryEntry entry = IOKitUtilFFM.getMatchingService(serviceName);
        if (entry != null) {
            try (entry) {
                return extractor.apply(new FfmRegistryEntry(entry));
            }
        }
        return null;
    }

    @Override
    public void forEachMatchingService(String serviceName, Consumer<RegistryEntry> consumer) {
        forEachMatchingServiceUntil(serviceName, entry -> {
            consumer.accept(entry);
            return false;
        });
    }

    @Override
    public void forEachMatchingServiceUntil(String serviceName, Function<RegistryEntry, Boolean> visitor) {
        IOIterator iter = IOKitUtilFFM.getMatchingServices(serviceName);
        if (iter != null) {
            try (iter) {
                IORegistryEntry entry = iter.next();
                while (entry != null) {
                    try (IORegistryEntry current = entry) {
                        if (Boolean.TRUE.equals(visitor.apply(new FfmRegistryEntry(current)))) {
                            return;
                        }
                    }
                    entry = iter.next();
                }
            }
        }
    }

    private static final class FfmRegistryEntry implements RegistryEntry {
        private final IORegistryEntry entry;

        FfmRegistryEntry(IORegistryEntry entry) {
            this.entry = entry;
        }

        @Override
        public String getName() {
            return entry.getName();
        }

        @Override
        public byte[] getByteArrayProperty(String key) {
            return entry.getByteArrayProperty(key);
        }

        @Override
        public String getStringProperty(String key) {
            return entry.getStringProperty(key);
        }
    }
}
