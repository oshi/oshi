package oshi.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ParseUtilTest {

	@Test
	public void testParseHertz() {
		assertEquals(1L, ParseUtil.parseHertz("1Hz"));
		assertEquals(500L, ParseUtil.parseHertz("500 Hz"));
		assertEquals(1000L, ParseUtil.parseHertz("1kHz"));
		assertEquals(1000000L, ParseUtil.parseHertz("1MHz"));
		assertEquals(1000000000L, ParseUtil.parseHertz("1GHz"));
		assertEquals(1500000000L, ParseUtil.parseHertz("1.5GHz"));
		assertEquals(1000000000000L, ParseUtil.parseHertz("1THz"));
	}

}
