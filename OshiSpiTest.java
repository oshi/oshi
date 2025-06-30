import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemFactory;
import oshi.software.os.OperatingSystemFactoryProvider;

public class OshiSpiTest {
    public static void main(String[] args) {
        System.out.println(System.getProperty("java.version"));
        OperatingSystemFactory factory = OperatingSystemFactoryProvider.getInstance();
        boolean os = factory.isSupportedOnThisJdk();

        System.out.println("Loaded factory: " + factory.getClass().getName());
    }
}
