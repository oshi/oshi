package oshi.demo;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author sam
 */
public class DetectVMTest {
    @SuppressWarnings("unchecked")
    @Test
    public void testGetOuiByMacAddressIfPossible() throws NoSuchFieldException, IllegalAccessException {
        Field f = DetectVM.class.getDeclaredField("vmMacAddressOUI");
        f.setAccessible(true);
        Map<String, String> map = (Map<String, String>) f.get(null);
        map.forEach((k, v) -> assertThat(DetectVM.findOuiByMacAddressIfPossible(k + ":00"), is(v)));
    }
}
