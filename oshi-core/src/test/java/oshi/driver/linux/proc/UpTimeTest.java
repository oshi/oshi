package oshi.driver.linux.proc;

import com.sun.jna.Platform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UpTimeTest {

    @Test
    public void testGetSystemUptimeSeconds() {
        if (Platform.isLinux()) {
            double uptime = UpTime.getSystemUptimeSeconds();
            assertTrue(uptime >= 0, "Uptime should be more than equal to 0 seconds");
        }
    }
}
