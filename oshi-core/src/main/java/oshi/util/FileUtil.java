/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors:
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File reading methods
 *
 * @author widdis[at]gmail[dot]com
 */
public class FileUtil {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    private static final String READ = "Read {}";
    private static final String READING_FILE = "Reading file {}";
    private static final String ERROR_READING_FILE = "Error reading file {}. {}";
    private static final String ERROR_CLOSING_FILE = "Error closing file {}. {}";

    private FileUtil() {
    }

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
        List<String> lines = new ArrayList<String>();
        File file = new File(filename);
        if (file.canRead()) {
            LOG.debug(READING_FILE, filename);
            BufferedReader br = null;
            InputStreamReader isr = null;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                isr = new InputStreamReader(fis, ParseUtil.UTF_8);
                br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                if (reportError) {
                    LOG.error(ERROR_READING_FILE, filename, e.getMessage());
                }
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException ex) {
                    LOG.error(ERROR_CLOSING_FILE, filename, ex.getMessage());
                }
                try {
                    if (isr != null) {
                        isr.close();
                    }
                } catch (IOException ex) {
                    LOG.error(ERROR_CLOSING_FILE, filename, ex.getMessage());
                }
                try {
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException ex) {
                    LOG.error(ERROR_CLOSING_FILE, filename, ex.getMessage());
                }
            }
        } else if (reportError) {
            LOG.warn("File not found or not readable: {}", filename);
        }
        return lines;
    }

    /**
     * Read a file and return the long value contained therein. Intended primarily
     * for Linux /sys filesystem
     *
     * @param filename
     *            The file to read
     * @return The value contained in the file, if any; otherwise zero
     */
    public static long getLongFromFile(String filename) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(READING_FILE, filename);
        }
        List<String> read = FileUtil.readFile(filename, false);
        if (!read.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(READ, read.get(0));
            }
            return ParseUtil.parseLongOrDefault(read.get(0), 0L);
        }
        return 0L;
    }

    /**
     * Read a file and return the unsigned long value contained therein as a long.
     * Intended primarily for Linux /sys filesystem
     *
     * @param filename
     *            The file to read
     * @return The value contained in the file, if any; otherwise zero
     */
    public static long getUnsignedLongFromFile(String filename) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(READING_FILE, filename);
        }
        List<String> read = FileUtil.readFile(filename, false);
        if (!read.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(READ, read.get(0));
            }
            return ParseUtil.parseUnsignedLongOrDefault(read.get(0), 0L);
        }
        return 0L;
    }

    /**
     * Read a file and return the int value contained therein. Intended primarily
     * for Linux /sys filesystem
     *
     * @param filename
     *            The file to read
     * @return The value contained in the file, if any; otherwise zero
     */
    public static int getIntFromFile(String filename) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(READING_FILE, filename);
        }
        try {
            List<String> read = FileUtil.readFile(filename, false);
            if (!read.isEmpty()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(READ, read.get(0));
                }
                return Integer.parseInt(read.get(0));
            }
        } catch (NumberFormatException ex) {
            LOG.warn("Unable to read value from {}. {}", filename, ex);
        }
        return 0;
    }

    /**
     * Read a file and return the String value contained therein. Intended primarily
     * for Linux /sys filesystem
     *
     * @param filename
     *            The file to read
     * @return The value contained in the file, if any; otherwise empty string
     */
    public static String getStringFromFile(String filename) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(READING_FILE, filename);
        }
        List<String> read = FileUtil.readFile(filename, false);
        if (!read.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(READ, read.get(0));
            }
            return read.get(0);
        }
        return "";
    }

    /**
     * Read a file and return a map of string keys to string values contained
     * therein. Intended primarily for Linux /proc/[pid]/io
     *
     * @param filename
     *            The file to read
     * @param separator
     *            Characters in each line of the file that separate the key and the
     *            value
     * @return The map contained in the file, if any; otherwise empty map
     */
    public static Map<String, String> getKeyValueMapFromFile(String filename, String separator) {
        Map<String, String> map = new HashMap<String, String>();
        if (LOG.isDebugEnabled()) {
            LOG.debug(READING_FILE, filename);
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
}
