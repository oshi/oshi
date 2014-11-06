package oshi.util;

import java.text.DecimalFormatSymbols;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class FormatUtilTest {

    private static char DECIMAL_SEPARATOR;

    @BeforeClass
    public static void setUpClass() {
        // use decimal separator according to current locale
        DECIMAL_SEPARATOR = DecimalFormatSymbols.getInstance().getDecimalSeparator();
    }

    @Test
    public void testFormatBytes() {
        assertEquals("0 bytes", FormatUtil.formatBytes(0));
        assertEquals("1 byte", FormatUtil.formatBytes(1));
        assertEquals("532 bytes", FormatUtil.formatBytes(532));
        assertEquals("1 KB", FormatUtil.formatBytes(1024));
        assertEquals("1 GB", FormatUtil.formatBytes(1024 * 1024 * 1024));
        assertEquals("1 TiB", FormatUtil.formatBytes(1099511627776L));
    }

    @Test
    public void testFormatBytesWithDecimalSeparator() {
        String expected1 = "1" + DECIMAL_SEPARATOR + "3 KB";
        String expected2 = "2" + DECIMAL_SEPARATOR + "3 MB";
        String expected3 = "2" + DECIMAL_SEPARATOR + "2 GB";
        String expected4 = "1" + DECIMAL_SEPARATOR + "1 TiB";
        assertEquals(expected1, FormatUtil.formatBytes(1340));
        assertEquals(expected2, FormatUtil.formatBytes(2400016));
        assertEquals(expected3, FormatUtil.formatBytes(2400000000L));
        assertEquals(expected4, FormatUtil.formatBytes(1099511627776L + 109951162777L));
    }
}
