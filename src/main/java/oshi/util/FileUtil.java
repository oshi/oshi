/**
 * Copyright (c) Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * Read an entire file at one time and return a list of Strings for each line.
 * Intended primarily for Linux /proc filesystem to avoid recalculating file
 * contents on iterative reads
 * 
 * @author widdis[at]gmail[dot]com
 */
public class FileUtil {
	public static List<String> readFile(String filename) throws IOException {
		File f = new File(filename);
		byte[] bytes = Files.readAllBytes(f.toPath());
		String s = new String(bytes, "UTF-8");
		return Arrays.asList(s.split(System.getProperty("line.separator")));
	}
}
