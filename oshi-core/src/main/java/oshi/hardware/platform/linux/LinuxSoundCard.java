package oshi.hardware.platform.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.FileUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LinuxSoundCard extends AbstractSoundCard {

    private static final String SC_PATH = "/proc/asound/";
    private static final String CARD_FOLDER = "card";
    private static final String CARDS_FILE = "cards";
    private static final String ID_FILE = "id";

    private static final Logger LOG = LoggerFactory.getLogger(LinuxSoundCard.class);


    public LinuxSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
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
    private static String getSoundCardVersion() {
        String driverVersion = "not available";
        try {
            //driverVersion = Files.readAllLines(Paths.get(new File(SC_PATH + "version").toURI())).get(0);
            driverVersion = FileUtil.getStringFromFile(SC_PATH + "version");
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return driverVersion;
    }


    private static String getCardCodec(File file) {
        File codecFile = Objects.requireNonNull(file.listFiles(
                (dir, name)
                        -> name.contains("codec")))
                [0];
        return FileUtil.getKeyValueMapFromFile(file.getPath() + "/" + codecFile.getName(), ":").get("Codec");

    }

    private static String getCardName(File file) {
        String cardName = "Not Found..";
        Map<String, String> cardNamePairs = FileUtil.getKeyValueMapFromFile(SC_PATH + "/" + CARDS_FILE, ":");
        String cardId = FileUtil.getStringFromFile(file.getPath() + "/" + ID_FILE);
        for (Map.Entry<String, String> entry : cardNamePairs.entrySet()) {
            if (entry.getKey().contains(cardId)) {
                cardName = entry.getValue();
                return cardName;
            }
        }
        return cardName;
    }

    public static List<LinuxSoundCard> getSoundCards() {
        List<LinuxSoundCard> soundCards = new ArrayList<>();
        for (File cardFile : getCardFolders()) {
            soundCards.add(new LinuxSoundCard(getSoundCardVersion(), getCardName(cardFile), getCardCodec(cardFile)));
        }
        return soundCards;
    }

}
