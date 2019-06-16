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
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The global configuration utility.
 * 
 * @author cilki
 * @since 4.0.0
 */
public final class ConfigUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigUtil.class);

    private static final Properties configuration = new Properties();

    static {
        try (InputStream in = ConfigUtil.class.getResourceAsStream("/default.properties")) {
            if (in != null)
                configuration.load(in);
            else
                LOG.warn("Default configuration not found");
        } catch (IOException e) {
            LOG.warn("Failed to load default configuration");
        }
    }

    /**
     * Get the {@code String} property associated with the given key.
     * 
     * @param key The property key
     * @return The property value or {@code null} if not found
     */
    public static String get(String key) {
        return configuration.getProperty(key);
    }

    /**
     * Get the {@code String} property associated with the given key.
     * 
     * @param key The property key
     * @param def The default value
     * @return The property value or the default if not found
     */
    public static String get(String key, String def) {
        return configuration.getProperty(key, def);
    }

    /**
     * Get the {@code Integer} property associated with the given key.
     * 
     * @param key The property key
     * @return The property value or {@code 0} if not found
     */
    public static int getInteger(String key) {
        return getInteger(key, 0);
    }

    /**
     * Get the {@code Integer} property associated with the given key.
     * 
     * @param key The property key
     * @param def The default value
     * @return The property value or the default if not found
     */
    public static int getInteger(String key, int def) {
        String value = configuration.getProperty(key);
        return value == null ? def : Integer.parseInt(value);
    }

    /**
     * Get the {@code Double} property associated with the given key.
     * 
     * @param key The property key
     * @return The property value or {@code 0.0} if not found
     */
    public static Double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    /**
     * Get the {@code Double} property associated with the given key.
     * 
     * @param key The property key
     * @param def The default value
     * @return The property value or the default if not found
     */
    public static Double getDouble(String key, double def) {
        String value = configuration.getProperty(key);
        return value == null ? def : Double.parseDouble(value);
    }

    /**
     * Get the {@code Boolean} property associated with the given key.
     * 
     * @param key The property key
     * @return The property value or {@code false} if not found
     */
    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Get the {@code Boolean} property associated with the given key.
     * 
     * @param key The property key
     * @param def The default value
     * @return The property value or the default if not found
     */
    public static boolean getBoolean(String key, boolean def) {
        String value = configuration.getProperty(key);
        return value == null ? def : Boolean.parseBoolean(value);
    }

    /**
     * Set the given property, overwriting any existing value.
     * 
     * @param key The property key
     * @param val The new value
     */
    public static void set(String key, Object val) {
        configuration.setProperty(key, val.toString());
    }

    /**
     * Load the given {@link Properties} into the global configuration.
     * 
     * @param properties The new properties
     */
    public static void load(Properties properties) {
        configuration.putAll(properties);
    }

    private ConfigUtil() {
    }
}
