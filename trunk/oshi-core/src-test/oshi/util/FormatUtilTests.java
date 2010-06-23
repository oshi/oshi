package oshi.util;

import junit.framework.TestCase;

public class FormatUtilTests extends TestCase {
	public void testFormatBytes() {
		assertEquals("0 bytes", FormatUtil.formatBytes(0));
		assertEquals("1 byte", FormatUtil.formatBytes(1));
		assertEquals("532 bytes", FormatUtil.formatBytes(532));
		assertEquals("1 KB", FormatUtil.formatBytes(1024));
		assertEquals("1.3 KB", FormatUtil.formatBytes(1340));
		assertEquals("2.3 MB", FormatUtil.formatBytes(2400016));
		assertEquals("1 GB", FormatUtil.formatBytes(1024 * 1024 * 1024));
		assertEquals("2.2 GB", FormatUtil.formatBytes(2400000000L));
		assertEquals("1 TB", FormatUtil.formatBytes(1099511627776L));
		assertEquals("1.1 TB", FormatUtil.formatBytes(1099511627776L + 109951162777L));
	}
}
