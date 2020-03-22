package oshi.util;

import java.util.Properties;

import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

/**
 * Maps a processor's family and model to its microarchitecture codename
 */
public class MicroarchitectureUtil {

    private static final String OSHI_ARCHITECTURE_PROPERTIES = "oshi.architecture.properties";

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
        Properties architectures = FileUtil.readPropertiesFromFilename(OSHI_ARCHITECTURE_PROPERTIES);
        // Intel or AMD
        StringBuilder sb = new StringBuilder();
        if (pi.getVendor().contains("AMD")) {
            sb.append("amd.");
        }
        sb.append(pi.getFamily());
        // Check for match with only family
        String arch = architectures.getProperty(sb.toString());
        if (Util.isBlank(arch)) {
            // Append model
            sb.append('.').append(pi.getModel());
        }
        if (Util.isBlank(arch)) {
            // Append stepping
            sb.append('.').append(pi.getStepping());
        }
        arch = architectures.getProperty(sb.toString());
        return Util.isBlank(arch) ? Constants.UNKNOWN : arch;
    }
}
