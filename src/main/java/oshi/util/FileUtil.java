/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File reading methods
 * 
 * @author widdis[at]gmail[dot]com
 */
public class FileUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    /**
     * Read an entire file at one time. Intended primarily for Linux /proc
     * filesystem to avoid recalculating file contents on iterative reads.
     * 
     * @param filename
     *            The file to read
     * 
     * @return A list of Strings representing each line of the file, or an empty
     *         list if file could not be read or is empty
     */
    public static List<String> readFile(String filename) {
        return readFile(filename, true);
    }

    /**
     * Read an entire file at one time. Intended primarily for Linux /proc
     * filesystem to avoid recalculating file contents on iterative reads.
     * 
     * @param filename
     *            The file to read
     * @param reportError
     *            Whether to log errors reading the file
     * 
     * @return A list of Strings representing each line of the file, or an empty
     *         list if file could not be read or is empty
     */
    public static List<String> readFile(String filename, boolean reportError) {
        if (new File(filename).exists()) {
            LOG.debug("Reading file {}", filename);
            try {
                return Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
            } catch (IOException e) {
                if (reportError) {
                    LOG.error("Error reading file {}", filename);
                }
            }
        } else if (reportError) {
            LOG.warn("File not found: {}", filename);
        }
        return new ArrayList<String>();
    }

    /**
     * Read a file and return the long value contained therein. Intended
     * primarily for Linux /sys filesystem
     * 
     * @param filename
     *            The file to read
     * @return The value contained in the file, if any; otherwise zero
     */
    public static long getLongFromFile(String filename) {
        LOG.debug("Reading file {}", filename);
        try {
            List<String> read = FileUtil.readFile(filename, false);
            if (!read.isEmpty()) {
                LOG.trace("Read {}", read.get(0));
                return Long.parseLong(read.get(0));
            }
        } catch (NumberFormatException ex) {
            LOG.debug("Unable to read value from {}", filename);
        }
        return 0L;
    }

    /**
     * Read a file and return the int value contained therein. Intended
     * primarily for Linux /sys filesystem
     * 
     * @param filename
     *            The file to read
     * @return The value contained in the file, if any; otherwise zero
     */
    public static int getIntFromFile(String filename) {
        LOG.debug("Reading file {}", filename);
        try {
            List<String> read = FileUtil.readFile(filename, false);
            if (!read.isEmpty()) {
                LOG.trace("Read {}", read.get(0));
                return Integer.parseInt(read.get(0));
            }
        } catch (NumberFormatException ex) {
            LOG.debug("Unable to read value from {}", filename);
        }
        return 0;
    }

    /**
     * Read a file and return an array of whitespace-delimited string values
     * contained therein. Intended primarily for Linux /proc
     * 
     * @param filename
     *            The file to read
     * @return An array of strings containing delimited values
     */
    public static String[] getSplitFromFile(String filename) {
        LOG.debug("Reading file {}", filename);
        List<String> read = FileUtil.readFile(filename, false);
        if (!read.isEmpty()) {
            LOG.trace("Read {}", read.get(0));
            return read.get(0).split("\\s+");
        }
        return new String[0];
    }
}