/*
 * Copyright (c) Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
		List<String> result = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filename));
			for (;;) {
				String line = br.readLine();
				if (line == null)
					break;
				result.add(line);
			}
		} catch (FileNotFoundException e) {
			throw new IOException("Unable to read from " + filename);
		} finally {
			if (br != null)
				br.close();
		}
		return result;
	}
}
