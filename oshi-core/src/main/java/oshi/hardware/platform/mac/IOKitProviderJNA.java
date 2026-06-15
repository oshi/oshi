/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;

import oshi.hardware.common.platform.mac.IOKitProvider;

/**
 * JNA-based {@link IOKitProvider} implementation.
 */
final class IOKitProviderJNA implements IOKitProvider {

    static final IOKitProviderJNA INSTANCE = new IOKitProviderJNA();

    private IOKitProviderJNA() {
    }

    @Override
    public <T> T withMatchingService(String serviceName, Function<RegistryEntry, T> extractor) {
        IORegistryEntry entry = IOKitUtil.getMatchingService(serviceName);
        if (entry != null) {
            try {
                return extractor.apply(new JnaRegistryEntry(entry));
            } finally {
                entry.release();
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
    public void forEachMatchingServiceUntil(String serviceName, Predicate<RegistryEntry> visitor) {
        IOIterator iter = IOKitUtil.getMatchingServices(serviceName);
        if (iter != null) {
            try {
                IORegistryEntry entry = iter.next();
                while (entry != null) {
                    try {
                        if (visitor.test(new JnaRegistryEntry(entry))) {
                            return;
                        }
                    } finally {
                        entry.release();
                    }
                    entry = iter.next();
                }
            } finally {
                iter.release();
            }
        }
    }

    private static final class JnaRegistryEntry implements RegistryEntry {
        private final IORegistryEntry entry;

        JnaRegistryEntry(IORegistryEntry entry) {
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

        @Override
        public Integer getIntegerProperty(String key) {
            return entry.getIntegerProperty(key);
        }

        @Override
        public Boolean getBooleanProperty(String key) {
            return entry.getBooleanProperty(key);
        }
    }
}
