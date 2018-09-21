package oshi.hardware.platform.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LinuxSoundCard extends AbstractSoundCard {

    private static final String SC_PATH = "/proc/asound/";
    private static final String CARD_FOLDER = "card";
    private static final Logger LOG = LoggerFactory.getLogger(LinuxSoundCard.class);

    private static LinuxSoundCard soundCard;

    public LinuxSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }


    public static List<LinuxSoundCard> getLinuxSoundCards() {
        List<LinuxSoundCard> soundCards = new ArrayList<>();
        for (File card : getCardFolders()) {
            // soundCards.add(new LinuxSoundCard(getSoundCardVersion(),getCardName(),getCardCodec(),))
        }
        return null;
    }

    /**
     * Method to find all the card folders contained in the asound folder denoting the cards
     * currently contained in our machine.
     *
     * @return : A list of files starting with 'card'
     */
    public static List<File> getCardFolders() {
        File cardsDirectory = new File(SC_PATH);
        List<File> cardFolders = new ArrayList<>();
        for (File card : Objects.requireNonNull(cardsDirectory.listFiles())) {
            if (card.getName().startsWith(CARD_FOLDER) && card.isDirectory()) {
                cardFolders.add(card);
            }
        }
        return cardFolders;
    }


    /**
     * Reads the 'version' file in the asound folder that contains the complete name of the ALSA driver.
     * Reads all the lines of the file and retrieves the first line.
     *
     * @return The complete name of the ALSA driver currently residing in our machine
     */
    public static String getSoundCardVersion() {
        String driverVersion = "";
        try {
            driverVersion = Files.readAllLines(Paths.get(new File(SC_PATH + "version").toURI())).get(0);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return driverVersion;
    }

}
