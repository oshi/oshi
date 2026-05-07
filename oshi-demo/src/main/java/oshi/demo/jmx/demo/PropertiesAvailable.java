/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx.demo;

import java.util.List;

/**
 * Interface for objects that expose a list of property names.
 */
public interface PropertiesAvailable {
    /**
     * Gets the list of available property names.
     *
     * @return list of property names
     */
    List<String> getProperties();
}
