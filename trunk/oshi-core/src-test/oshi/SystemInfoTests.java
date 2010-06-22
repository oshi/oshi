/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi;

import junit.framework.TestCase;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;

/**
 * @author dblock[at]dblock[dot]org
 */
public class SystemInfoTests extends TestCase {
	
	public static void main(String[] args) {
		SystemInfo si = new SystemInfo();
		// software
		OperatingSystem os = si.getOperatingSystem();
		System.out.println(os.toString());
	}
	
	public void testGetVersion() {
		SystemInfo si = new SystemInfo();
		OperatingSystem os = si.getOperatingSystem();
		assertNotNull(os);
		OperatingSystemVersion version = os.getVersion();
		assertNotNull(version);
		assertTrue(os.toString().startsWith("Microsoft Windows "));
	}
}
