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

/**
 * Sound card data obtained via /proc/asound directory
 *
 * @author BilalAM
 */

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
     * Method to find all the card folders contained in the <b>asound</b> folder denoting the cards
     * currently contained in our machine.
     *
     * @return : A list of files starting with 'card'
     */
    private static List<File> getCardFolders() {
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


    /**
     * Retrieves the codec of the sound card contained in the <b>codec</b> file.
     * The name of the codec is always the first line of that file.
     * <br>
     * <b>Working</b>
     * <br>
     * This converts the codec file into key value pairs using the {@link FileUtil} class and then
     * returns the value of the <b>Codec</b> key.
     *
     * @param file The sound card file
     * @return The name of the codec
     */
    private static String getCardCodec(File file) {
        File codecFile = Objects.requireNonNull(file.listFiles(
                (dir, name)
                        -> name.contains("codec")))
                [0];
        return FileUtil.getKeyValueMapFromFile(file.getPath() + "/" + codecFile.getName(), ":").get("Codec");

    }


    /**
     * Retrieves the name of the sound card by :
     * <ol>
     *     <li>Reading the <b>id</b> file and comparing each id with the card id present in the <b>cards</b> file</li>
     *     <li>If the id and the card name matches , then it assigns that name to {@literal cardName}</li>
     * </ol>
     * @param file The soundcard File.
     * @return The name of the sound card.
     */
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


    /**
     * public method used by {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access the sound cards.
     * @return List of {@link LinuxSoundCard} objects.
     */
    public static List<LinuxSoundCard> getSoundCards() {
        List<LinuxSoundCard> soundCards = new ArrayList<>();
        for (File cardFile : getCardFolders()) {
            soundCards.add(new LinuxSoundCard(getSoundCardVersion(), getCardName(cardFile), getCardCodec(cardFile)));
        }
        return soundCards;
    }

}
