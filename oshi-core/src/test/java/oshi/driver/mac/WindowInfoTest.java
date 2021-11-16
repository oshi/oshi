package oshi.driver.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.sun.jna.Platform;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import oshi.software.os.OSDesktopWindow;
import oshi.software.os.OSProcess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WindowInfoTest {
    @Test
    void testQueryDesktopWindows(){
        if(Platform.isMac()){
            final List<OSDesktopWindow> osDesktopWindows = WindowInfo.queryDesktopWindows(true);
            final List<OSDesktopWindow> allOsDesktopWindows = WindowInfo.queryDesktopWindows(false);
            final Set<Integer> windowOrders = new HashSet<>();
            final Set<Long> windowIds = new HashSet<>();

            assertThat("Desktop should have at least one window.", osDesktopWindows.size(), is(greaterThan(0)));
            assertThat("The number of all desktop windows should be greater than the number of visible desktop windows", allOsDesktopWindows.size(), is(greaterThan(osDesktopWindows.size())));
            for(OSDesktopWindow window : osDesktopWindows){
                windowOrders.add(window.getOrder());
                windowIds.add(window.getWindowId());
                assertThat("Windows should have a title.", window.getTitle(), is(not(emptyOrNullString())));
                assertThat("Window should be visible", window.isVisible(), is(true));
                System.out.println(window);
            }
            assertThat("Number of layers must be less than or equal to 20.", windowOrders.size(), is(lessThanOrEqualTo(20)));
            assertThat("Desktop window layer is not present.", windowOrders.contains(0), is(true));
            assertThat("Dock window layer is not present", windowOrders.contains(24), is(true));
            assertThat("Dock icon window layer is not present", windowOrders.contains(25), is(true));

        }
    }
}
