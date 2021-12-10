package oshi.driver.linux.proc;

import com.sun.jna.Platform;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DiskStatsTest {

    @Test
    public void testGetDiskStats() {
        if (Platform.isLinux()) {
            Map<String, Map<DiskStats.IoStat, Long>> map = DiskStats.getDiskStats();
            assertNotNull(map, "DiskStats should not be null");
            DiskStats.IoStat[] enumArray = DiskStats.IoStat.class.getEnumConstants();

            map.forEach((key, value) -> {
                assertNotNull(value, "Entry should not have a null map!");
                assertInstanceOf(EnumMap.class, value, "Value should be enum map!");
            });
        }
    }

}
