package oshi.driver.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.sun.jna.Platform;
import org.junit.jupiter.api.Test;
import oshi.software.os.OSDesktopWindow;
import oshi.software.os.OSProcess;

import java.util.HashSet;
import java.util.List;

public class WindowInfoTest {
    @Test
    void testQueryDesktopWindows(){
        if(Platform.isMac()){
            final List<OSDesktopWindow> osDesktopWindows = WindowInfo.queryDesktopWindows(true);
            final List<OSDesktopWindow> allOsDesktopWindows = WindowInfo.queryDesktopWindows(false);
            HashSet<Long> windowIds = new HashSet<>();

            assertThat("Desktop should have at least one window.", osDesktopWindows.size(), is(greaterThan(0)));
            assertThat("The number of all desktop windows should be greater than the number of visible desktop windows", allOsDesktopWindows.size(), is(greaterThan(osDesktopWindows.size())));
            for(OSDesktopWindow window : osDesktopWindows){
                assertThat("Windows should have a title.", window.getTitle(), is(not(emptyOrNullString())));
                assertThat("Window IDs should be unique.", window.getWindowId(), is(not(in(windowIds))));
                windowIds.add(window.getWindowId());
                assertThat("Window order number must be greater than or equal to 0", window.getOrder(), is(greaterThanOrEqualTo(0)));
                assertThat("Window order number must be less than the number of windows", window.getOrder(), is(lessThan(osDesktopWindows.size())));
                assertThat("Window should be visible", window.isVisible(), is(true));
            }
        }
    }
}
