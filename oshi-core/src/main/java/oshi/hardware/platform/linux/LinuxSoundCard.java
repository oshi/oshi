package oshi.hardware.platform.linux;

import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LinuxSoundCard extends AbstractSoundCard {

    private static final String SC_PATH = "/proc/asound/";
    private static final String CARD_FOLDER = "card";
    private static LinuxSoundCard soundCard;

    public LinuxSoundCard(String kernelVersion, String name, String codec, SoundCard[] devices) {
        super(kernelVersion, name, codec, devices);
    }


    public static List<SoundCard> getLinuxSoundCards() {
        for (File card : getCardFolders()) {

        }
        return null;
    }

    /**
     * Method to find all the card folders contained in the asound folder denoting the cards
     * currently contained in our machine.
     *
     * @return : A list of files starting with 'card'
     */
    private static List<File> getCardFolders() {
        File cardsDirectory = new File(SC_PATH);
        List<File> cardFolders = new ArrayList<>();
        for (File card : Objects.requireNonNull(cardsDirectory.listFiles())) {
            if (card.getName().startsWith(CARD_FOLDER)) {
                cardFolders.add(card);
            }
        }
        return cardFolders;
    }


}
