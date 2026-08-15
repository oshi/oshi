/*
 * Copyright 2019-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static oshi.util.GlobalConfig.clear;
import static oshi.util.GlobalConfig.get;
import static oshi.util.GlobalConfig.load;
import static oshi.util.GlobalConfig.remove;
import static oshi.util.GlobalConfig.set;
import static oshi.util.GlobalConfigTest.GlobalConfigAsserter.asserter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;

import oshi.util.GlobalConfig.PropertyException;

@Execution(SAME_THREAD)
class GlobalConfigTest {
    private static final String PROPERTY = "oshi.test.property";
    private static final double EPSILON = Double.MIN_VALUE;

    @BeforeEach
    void setUp() {
        clear();
    }

    @Test
    void testGetString() {
        asserter(PROPERTY).assertDefaultThat(null, null);
        set(PROPERTY, "test");
        asserter(PROPERTY).assertThat("test", null);
        set(PROPERTY, 123);

        asserter(PROPERTY).assertThat("123", null);
    }

    @Test
    void testGetInteger() {
        asserter(PROPERTY).assertDefaultThat(0, 0);
        set(PROPERTY, 123);
        asserter(PROPERTY).assertThat(123, 0).assertThat("123", null);

        // Invalid integer
        set(PROPERTY, "1.23");
        asserter(PROPERTY).assertThat(0, 0);
    }

    @Test
    void testGetDouble() {
        asserter(PROPERTY).assertDefaultThat(0.0, 0.0);
        set(PROPERTY, 1.23d);
        asserter(PROPERTY).assertThat(1.23, 0.0).assertThat("1.23", null);

        // Invalid double
        set(PROPERTY, "1.2.3");
        asserter(PROPERTY).assertThat(0.0, 0.0);
    }

    @Test
    void testGetBoolean() {
        asserter(PROPERTY).assertDefaultThat(false, false);
        set(PROPERTY, true);
        asserter(PROPERTY).assertThat(true, false).assertThat("true", null);
    }

    @Test
    void testSetNull() {
        set(PROPERTY, "test");
        set(PROPERTY, null);
        asserter(PROPERTY).assertThat("123", "123");
    }

    @Test
    void testRemove() {
        String removed = "test";
        set(PROPERTY, removed);
        remove(PROPERTY);
        asserter(PROPERTY).assertThat(format(Locale.ROOT, "Should have removed property %s", removed), "123", "123");
    }

    @Test
    void testLoad() {
        load(propertiesWith("321"));

        asserter(PROPERTY).assertThat("321", null);
    }

    @Test
    void testPropertyExceptionMessage() {
        set(PROPERTY, "test");
        assertThat(new PropertyException(PROPERTY).getMessage(),
                is(format(Locale.ROOT, "Invalid property: \"%s\" = test", PROPERTY)));
    }

    @Test
    void testLinuxPrivilegedProperties() {
        // Test defaults are empty strings
        assertThat(get(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, ""), is(""));
        assertThat(get(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST, ""), is(""));
        assertThat(get(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, ""), is(""));

        // Test setting and getting
        set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "sudo -n");
        assertThat(get(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, ""), is("sudo -n"));

        set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST, "dmidecode,lshw");
        assertThat(get(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST, ""), is("dmidecode,lshw"));

        set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST,
                "/proc/*/io,/sys/devices/virtual/dmi/id/product_serial");
        assertThat(get(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, ""),
                is("/proc/*/io,/sys/devices/virtual/dmi/id/product_serial"));
    }

    @Test
    void testLoadExternalConfig(@TempDir Path tempDir) throws Exception {
        // Write a temp properties file
        Path propsFile = tempDir.resolve("test-oshi.properties");
        Files.write(propsFile, "oshi.test.external=fromfile\n".getBytes(StandardCharsets.UTF_8));

        // Set system property to point to it
        System.setProperty("oshi.properties.file", propsFile.toString());
        try {
            Properties config = new Properties();
            GlobalConfig.loadExternalConfig(config);
            // External file value should be loaded
            assertThat(config.getProperty("oshi.test.external"), is("fromfile"));
            // System property override should win
            System.setProperty("oshi.test.external", "fromsysprop");
            config = new Properties();
            GlobalConfig.loadExternalConfig(config);
            assertThat(config.getProperty("oshi.test.external"), is("fromsysprop"));
        } finally {
            System.clearProperty("oshi.properties.file");
            System.clearProperty("oshi.test.external");
        }
    }

    @Test
    void testLoadExternalConfigMissingFile() {
        // Non-existent file should not throw or mutate config
        System.setProperty("oshi.properties.file", "/nonexistent/path/oshi.properties");
        try {
            Properties config = new Properties();
            GlobalConfig.loadExternalConfig(config);
            assertThat("Config should not contain the meta-property", config.getProperty("oshi.properties.file"),
                    is((String) null));
        } finally {
            System.clearProperty("oshi.properties.file");
        }
    }

    @Test
    void testEnvKeyToProperty() {
        assertThat(GlobalConfig.envKeyToProperty("OSHI_OS_LINUX_PRIVILEGED_PREFIX"),
                is("oshi.os.linux.privileged.prefix"));
        assertThat(GlobalConfig.envKeyToProperty("OSHI_UTIL_MEMOIZER_EXPIRATION"), is("oshi.util.memoizer.expiration"));
        assertThat(GlobalConfig.envKeyToProperty("OSHI_UTIL_PROC_PATH"), is("oshi.util.proc.path"));
        // Verify renamed constants match their env var mapping
        assertThat(GlobalConfig.envKeyToProperty("OSHI_OS_UNIX_WHOCOMMAND"), is(GlobalConfig.OSHI_OS_UNIX_WHOCOMMAND));
        assertThat(GlobalConfig.envKeyToProperty("OSHI_OS_SOLARIS_ALLOWKSTAT2"),
                is(GlobalConfig.OSHI_OS_SOLARIS_ALLOWKSTAT2));
    }

    private Properties propertiesWith(String value) {
        Properties updates = new Properties();
        updates.setProperty(PROPERTY, value);
        return updates;
    }

    static final class GlobalConfigAsserter {
        private final String property;

        private GlobalConfigAsserter(String property) {
            this.property = property;
        }

        static GlobalConfigAsserter asserter(String property) {
            return new GlobalConfigAsserter(property);
        }

        GlobalConfigAsserter assertThat(@Nullable Object expected, @Nullable Object def) {
            assertThat(failureMessage(def), expected, def);
            return this;
        }

        GlobalConfigAsserter assertThat(String message, @Nullable Object expected, @Nullable Object def) {
            if (def == null) {
                // No default supplied: exercise the single-argument overload, which is the only one that can
                // report an absent property as null
                assertThat(message, get(property), is(expected));
            } else if (def instanceof String) {
                assertThat(message, get(property, (String) def), is(expected));
            } else if (def instanceof Boolean) {
                assertThat(message, get(property, (boolean) def), is(expected));
            } else if (def instanceof Integer) {
                assertThat(message, get(property, (Integer) def), is(expected));
            } else if (def instanceof Double) {
                Double expectedDouble = (Double) expected;
                assertNotNull(expectedDouble, message);
                assertThat(message, get(property, (Double) def), is(closeTo(expectedDouble, EPSILON)));
            }
            return this;
        }

        GlobalConfigAsserter assertDefaultThat(@Nullable Object expected, @Nullable Object def) {
            assertThat(defaultFailureMessage(def), expected, def);
            return this;
        }

        private String failureMessage(@Nullable Object def) {
            return format(Locale.ROOT, "property: %s value for def: %s should be", property, def);
        }

        private String defaultFailureMessage(@Nullable Object def) {
            return format(Locale.ROOT, "Property: %s default value def: %s should be", PROPERTY, def);
        }
    }
}
