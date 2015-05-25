package oshi.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

public class FileUtilTest {

	private static String THISCLASS = "src/test/java/oshi/util/FileUtilTest.java";

	@Test
	public void testReadFile() {
		List<String> thisFile = null;
		try {
			thisFile = FileUtil.readFile(THISCLASS);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Comment ONE line
		int lineOne = 0;
		// Comment TWO line
		int lineTwo = 0;
		for (int i = 0; i < thisFile.size(); i++) {
			String line = thisFile.get(i);
			if (line.contains("Comment ONE line"))
				lineOne = i;
			if (line.contains("Comment TWO line"))
				lineTwo = i;
		}
		assertEquals(2, lineTwo - lineOne);
	}
}
