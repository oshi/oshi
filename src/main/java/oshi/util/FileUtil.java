/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
		return Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
	}
}