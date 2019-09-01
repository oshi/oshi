/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The global configuration utility. See
 * {@code src/main/resources/oshi.properties} for default values.
 */
public final class GlobalConfig {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalConfig.class);

    private static final Properties configuration = new Properties();

    static {
        // Load the configuration file from the classpath
        try {
            List<URL> resources = Collections
                    .list(Thread.currentThread().getContextClassLoader().getResources("oshi.properties"));
            if (resources.size() == 0) {
                LOG.warn("No default configuration found");
            } else {
                if (resources.size() > 1) {
                    LOG.warn("Configuration conflict: there is more than one oshi.properties file on the classpath");
                }

                try (InputStream in = resources.get(0).openStream()) {
                    if (in != null) {
                        configuration.load(in);
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed to load default configuration");
        }
    }

    /**
     * Get the {@code String} property associated with the given key.
     *
     * @param key
     *            The property key
     * @param def
     *            The default value
     * @return The property value or the given default if not found
     */
    public static String get(String key, String def) {
        return configuration.getProperty(key, def);
    }

    /**
     * Get the {@code int} property associated with the given key.
     *
     * @param key
     *            The property key
     * @param def
     *            The default value
     * @return The property value or the given default if not found
     */
    public static int get(String key, int def) {
        String value = configuration.getProperty(key);
        return value == null ? def : ParseUtil.parseIntOrDefault(value, def);
    }

    /**
     * Get the {@code double} property associated with the given key.
     *
     * @param key
     *            The property key
     * @param def
     *            The default value
     * @return The property value or the given default if not found
     */
    public static double get(String key, double def) {
        String value = configuration.getProperty(key);
        return value == null ? def : ParseUtil.parseDoubleOrDefault(value, def);
    }

    /**
     * Get the {@code boolean} property associated with the given key.
     *
     * @param key
     *            The property key
     * @param def
     *            The default value
     * @return The property value or the given default if not found
     */
    public static boolean get(String key, boolean def) {
        String value = configuration.getProperty(key);
        return value == null ? def : Boolean.parseBoolean(value);
    }

    /**
     * Set the given property, overwriting any existing value. If the given value is
     * {@code null}, the property is removed.
     *
     * @param key
     *            The property key
     * @param val
     *            The new value
     */
    public static void set(String key, Object val) {
        if (val == null) {
            configuration.remove(key);
        } else {
            configuration.setProperty(key, val.toString());
        }
    }

    /**
     * Reset the given property to its default value.
     *
     * @param key
     *            The property key
     */
    public static void remove(String key) {
        configuration.remove(key);
    }

    /**
     * Clear the configuration.
     */
    public static void clear() {
        configuration.clear();
    }

    /**
     * Load the given {@link java.util.Properties} into the global configuration.
     *
     * @param properties
     *            The new properties
     */
    public static void load(Properties properties) {
        configuration.putAll(properties);
    }

    /**
     * Indicates that a configuration value is invalid.
     */
    public static class PropertyException extends RuntimeException {

        private static final long serialVersionUID = -7482581936621748005L;

        /**
         * @param property
         *            The property name
         */
        public PropertyException(String property) {
            super("Invalid property: \"" + property + "\" = " + GlobalConfig.get(property, null));
        }

        /**
         * @param property
         *            The property name
         * @param message
         *            An exception message
         */
        public PropertyException(String property, String message) {
            super("Invalid property \"" + property + "\": " + message);
        }
    }

    private GlobalConfig() {
    }
}
