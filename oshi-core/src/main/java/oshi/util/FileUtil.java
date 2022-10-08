/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * File reading methods
 */
@ThreadSafe
public final class FileUtil {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    private static final String READING_LOG = "Reading file {}";
    private static final String READ_LOG = "Read {}";

    private FileUtil() {
    }

    /**
     * Read an entire file at one time. Intended primarily for Linux /proc filesystem to avoid recalculating file
     * contents on iterative reads.
     *
     * @param filename The file to read
     * @return A list of Strings representing each line of the file, or an empty list if file could not be read or is
     *         empty
     */
    public static List<String> readFile(String filename) {
        return readFile(filename, true);
    }

    /**
     * Read an entire file at one time. Intended primarily for Linux /proc filesystem to avoid recalculating file
     * contents on iterative reads.
     *
     * @param filename    The file to read
     * @param reportError Whether to log errors reading the file
     * @return A list of Strings representing each line of the file, or an empty list if file could not be read or is
     *         empty
     */
    public static List<String> readFile(String filename, boolean reportError) {
        if (new File(filename).canRead()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(READING_LOG, filename);
            }
            try {
                return Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
            } catch (IOException e) {
                if (reportError) {
                    LOG.error("Error reading file {}. {}", filename, e.getMessage());
                } else {
                    LOG.debug("Error reading file {}. {}", filename, e.getMessage());
                }
            }
        } else if (reportError) {
            LOG.warn("File not found or not readable: {}", filename);
        }
        return new ArrayList<>();
    }

    /**
     * Read an entire file at one time. Intended primarily for Linux /proc filesystem to avoid recalculating file
     * contents on iterative reads.
     *
     * @param filename The file to read
     * @return A byte array representing the file
     */
    public static byte[] readAllBytes(String filename) {
        return readAllBytes(filename, true);
    }

    /**
     * Read an entire file at one time. Intended primarily for Linux /proc filesystem to avoid recalculating file
     * contents on iterative reads.
     *
     * @param filename    The file to read
     * @param reportError Whether to log errors reading the file
     * @return A byte array representing the file
     */
    public static byte[] readAllBytes(String filename, boolean reportError) {
        if (new File(filename).canRead()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(READING_LOG, filename);
            }
            try {
                return Files.readAllBytes(Paths.get(filename));
            } catch (IOException e) {
                if (reportError) {
                    LOG.error("Error reading file {}. {}", filename, e.getMessage());
                } else {
                    LOG.debug("Error reading file {}. {}", filename, e.getMessage());
                }
            }
        } else if (reportError) {
            LOG.warn("File not found or not readable: {}", filename);
        }
        return new byte[0];
    }

    /**
     * Read an entire file at one time. Intended for unix /proc binary files to avoid reading file contents on iterative
     * reads.
     *
     *
     * @param filename The file to read
     * @return A bytebuffer representing the file if read was successful; null otherwise
     */
    public static ByteBuffer readAllBytesAsBuffer(String filename) {
        byte[] bytes = readAllBytes(filename, false);
        ByteBuffer buff = ByteBuffer.allocate(bytes.length);
        buff.order(ByteOrder.nativeOrder());
        for (byte b : bytes) {
            buff.put(b);
        }
        buff.flip();
        return buff;
    }

    /**
     * Reads a byte value from a ByteBuffer
     *
     * @param buff The bytebuffer to read from
     * @return The next byte value
     */
    public static byte readByteFromBuffer(ByteBuffer buff) {
        if (buff.position() < buff.limit()) {
            return buff.get();
        }
        return 0;
    }

    /**
     * Reads a short value from a ByteBuffer
     *
     * @param buff The bytebuffer to read from
     * @return The next short value
     */
    public static short readShortFromBuffer(ByteBuffer buff) {
        if (buff.position() <= buff.limit() - 2) {
            return buff.getShort();
        }
        return 0;
    }

    /**
     * Reads an int value from a ByteBuffer
     *
     * @param buff The bytebuffer to read from
     * @return The next int value
     */
    public static int readIntFromBuffer(ByteBuffer buff) {
        if (buff.position() <= buff.limit() - 4) {
            return buff.getInt();
        }
        return 0;
    }

    /**
     * Reads a long value from a ByteBuffer
     *
     * @param buff The bytebuffer to read from
     * @return The next long value
     */
    public static long readLongFromBuffer(ByteBuffer buff) {
        if (buff.position() <= buff.limit() - 8) {
            return buff.getLong();
        }
        return 0L;
    }

    /**
     * Reads a NativeLong value from a ByteBuffer
     *
     * @param buff The bytebuffer to read from
     * @return The next value
     */
    public static NativeLong readNativeLongFromBuffer(ByteBuffer buff) {
        return new NativeLong(Native.LONG_SIZE == 4 ? readIntFromBuffer(buff) : readLongFromBuffer(buff));
    }

    /**
     * Reads a size_t value from a ByteBuffer
     *
     * @param buff The bytebuffer to read from
     * @return The next value
     */
    public static size_t readSizeTFromBuffer(ByteBuffer buff) {
        return new size_t(Native.SIZE_T_SIZE == 4 ? readIntFromBuffer(buff) : readLongFromBuffer(buff));
    }

    /**
     * Reads a byte array value from a ByteBuffer
     *
     * @param buff  The bytebuffer to read from
     * @param array The array into which to read the data
     */
    public static void readByteArrayFromBuffer(ByteBuffer buff, byte[] array) {
        if (buff.position() <= buff.limit() - array.length) {
            buff.get(array);
        }
    }

    /**
     * Reads a Pointer value from a ByteBuffer
     *
     * @param buff The bytebuffer to read from
     * @return The next value
     */
    public static Pointer readPointerFromBuffer(ByteBuffer buff) {
        if (buff.position() <= buff.limit() - Native.POINTER_SIZE) {
            return Native.POINTER_SIZE == 4 ? new Pointer(buff.getInt()) : new Pointer(buff.getLong());
        }
        return Pointer.NULL;
    }

    /**
     * Read a file and return the long value contained therein. Intended primarily for Linux /sys filesystem
     *
     * @param filename The file to read
     * @return The value contained in the file, if any; otherwise zero
     */
    public static long getLongFromFile(String filename) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(READING_LOG, filename);
        }
        List<String> read = FileUtil.readFile(filename, false);
        if (!read.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(READ_LOG, read.get(0));
            }
            return ParseUtil.parseLongOrDefault(read.get(0), 0L);
        }
        return 0L;
    }

    /**
     * Read a file and return the unsigned long value contained therein as a long. Intended primarily for Linux /sys
     * filesystem
     *
     * @param filename The file to read
     * @return The value contained in the file, if any; otherwise zero
     */
    public static long getUnsignedLongFromFile(String filename) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(READING_LOG, filename);
        }
        List<String> read = FileUtil.readFile(filename, false);
        if (!read.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(READ_LOG, read.get(0));
            }
            return ParseUtil.parseUnsignedLongOrDefault(read.get(0), 0L);
        }
        return 0L;
    }

    /**
     * Read a file and return the int value contained therein. Intended primarily for Linux /sys filesystem
     *
     * @param filename The file to read
     * @return The value contained in the file, if any; otherwise zero
     */
    public static int getIntFromFile(String filename) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(READING_LOG, filename);
        }
        try {
            List<String> read = FileUtil.readFile(filename, false);
            if (!read.isEmpty()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(READ_LOG, read.get(0));
                }
                return Integer.parseInt(read.get(0));
            }
        } catch (NumberFormatException ex) {
            LOG.warn("Unable to read value from {}. {}", filename, ex.getMessage());
        }
        return 0;
    }

    /**
     * Read a file and return the String value contained therein. Intended primarily for Linux /sys filesystem
     *
     * @param filename The file to read
     * @return The value contained in the file, if any; otherwise empty string
     */
    public static String getStringFromFile(String filename) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(READING_LOG, filename);
        }
        List<String> read = FileUtil.readFile(filename, false);
        if (!read.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(READ_LOG, read.get(0));
            }
            return read.get(0);
        }
        return "";
    }

    /**
     * Read a file and return a map of string keys to string values contained therein. Intended primarily for Linux
     * {@code /proc/[pid]} files to provide more detailed or accurate information not available in the API.
     *
     * @param filename  The file to read
     * @param separator Character(s) in each line of the file that separate the key and the value.
     *
     * @return The map contained in the file, delimited by the separator, with the value whitespace trimmed. If keys and
     *         values are not parsed, an empty map is returned.
     */
    public static Map<String, String> getKeyValueMapFromFile(String filename, String separator) {
        Map<String, String> map = new HashMap<>();
        if (LOG.isDebugEnabled()) {
            LOG.debug(READING_LOG, filename);
        }
        List<String> lines = FileUtil.readFile(filename, false);
        for (String line : lines) {
            String[] parts = line.split(separator);
            if (parts.length == 2) {
                map.put(parts[0], parts[1].trim());
            }
        }
        return map;
    }

    /**
     * Read a configuration file from the sequence of context classloader, system classloader and classloader of the
     * current class, and return its properties
     *
     * @param propsFilename The filename
     * @return A {@link Properties} object containing the properties.
     */
    public static Properties readPropertiesFromFilename(String propsFilename) {
        Properties archProps = new Properties();
        // Load the configuration file from at least one of multiple possible
        // ClassLoaders, evaluated in order, eliminating duplicates
        for (ClassLoader loader : Stream.of(Thread.currentThread().getContextClassLoader(),
                ClassLoader.getSystemClassLoader(), FileUtil.class.getClassLoader())
                .collect(Collectors.toCollection(LinkedHashSet::new))) {
            if (readPropertiesFromClassLoader(propsFilename, archProps, loader)) {
                return archProps;
            }
        }
        LOG.warn("Failed to load configuration file from classloader: {}", propsFilename);
        return archProps;
    }

    private static boolean readPropertiesFromClassLoader(String propsFilename, Properties archProps,
            ClassLoader loader) {
        if (loader == null) {
            return false;
        }
        // Load the configuration file from the classLoader
        try {
            List<URL> resources = Collections.list(loader.getResources(propsFilename));
            if (resources.isEmpty()) {
                LOG.debug("No {} file found from ClassLoader {}", propsFilename, loader);
                return false;
            }
            if (resources.size() > 1) {
                LOG.warn("Configuration conflict: there is more than one {} file on the classpath: {}", propsFilename,
                        resources);
            }
            try (InputStream in = resources.get(0).openStream()) {
                if (in != null) {
                    archProps.load(in);
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Reads the target of a symbolic link
     *
     * @param file The file to read
     * @return The symlink name, or null if the read failed
     */
    public static String readSymlinkTarget(File file) {
        try {
            return Files.readSymbolicLink(Paths.get(file.getAbsolutePath())).toString();
        } catch (IOException e) {
            return null;
        }
    }
}
