/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Abstracts IOKit registry operations so that iteration logic can be shared between JNA and FFM implementations.
 */
public interface IOKitProvider {

    /**
     * Looks up a single IORegistry service by class name, extracts a result, and releases the entry.
     *
     * @param <T>         the result type
     * @param serviceName the IOService class name (e.g. "IOPlatformExpertDevice")
     * @param extractor   function applied to the matched entry; receives a non-null {@link RegistryEntry}
     * @return the extracted value, or {@code null} if the service was not found
     */
    <T> T withMatchingService(String serviceName, Function<RegistryEntry, T> extractor);

    /**
     * Iterates all IORegistry services matching a class name, invoking a consumer for each entry. Entries and the
     * iterator are released automatically.
     *
     * @param serviceName the IOService class name (e.g. "IOPlatformDevice")
     * @param consumer    consumer invoked for each matched entry
     */
    void forEachMatchingService(String serviceName, Consumer<RegistryEntry> consumer);

    /**
     * Iterates all IORegistry services matching a class name, invoking a function for each entry. If the function
     * returns {@code true}, iteration stops early. Entries and the iterator are released automatically.
     *
     * @param serviceName the IOService class name (e.g. "AppleARMIODevice")
     * @param visitor     function invoked for each matched entry; return {@code true} to stop iteration
     */
    void forEachMatchingServiceUntil(String serviceName, Function<RegistryEntry, Boolean> visitor);

    /**
     * A handle to an IORegistry entry, providing property access.
     */
    interface RegistryEntry {

        /**
         * Gets the name of this registry entry.
         *
         * @return the entry name, or {@code null} if unavailable
         */
        String getName();

        /**
         * Gets a byte array property from this registry entry.
         *
         * @param key the property key
         * @return the property value as a byte array, or {@code null} if not found
         */
        byte[] getByteArrayProperty(String key);

        /**
         * Gets a string property from this registry entry.
         *
         * @param key the property key
         * @return the property value as a string, or {@code null} if not found
         */
        String getStringProperty(String key);
    }
}
