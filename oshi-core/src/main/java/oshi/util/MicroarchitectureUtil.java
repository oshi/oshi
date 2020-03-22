package oshi.util;

import java.util.Properties;
import java.util.function.Supplier;

import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

/**
 * Maps a processor's family and model to its microarchitecture codename
 */
public final class MicroarchitectureUtil {

    private static final String OSHI_ARCHITECTURE_PROPERTIES = "oshi.architecture.properties";

    private static final Supplier<Properties> architectures = Memoizer.memoize(MicroarchitectureUtil::readProperties);

    private MicroarchitectureUtil() {
    }

    /**
     * Returns the processor's microarchitecture, if known.
     *
     * @param pi
     *            The identifier returned from
     *            {@link CentralProcessor#getProcessorIdentifier}
     * @return A string containing the microarchitecture if known.
     *         {@link Constants#UNKNOWN} otherwise.
     */
    public static String getArchitecture(ProcessorIdentifier pi) {
        Properties archProps = architectures.get();
        // Intel is default, no prefix
        StringBuilder sb = new StringBuilder();
        // AMD and ARM properties have prefix
        if (pi.getVendor().contains("AMD")) {
            sb.append("amd.");
        } else if (pi.getVendor().contains("ARM")) {
            sb.append("arm.");
        }
        sb.append(pi.getFamily());
        // Check for match with only family
        String arch = archProps.getProperty(sb.toString());

        if (Util.isBlank(arch)) {
            // Append model
            sb.append('.').append(pi.getModel());
        }
        arch = archProps.getProperty(sb.toString());

        if (Util.isBlank(arch)) {
            // Append stepping
            sb.append('.').append(pi.getStepping());
        }
        arch = archProps.getProperty(sb.toString());

        return Util.isBlank(arch) ? Constants.UNKNOWN : arch;
    }

    private static Properties readProperties() {
        return FileUtil.readPropertiesFromFilename(OSHI_ARCHITECTURE_PROPERTIES);
    }
}
