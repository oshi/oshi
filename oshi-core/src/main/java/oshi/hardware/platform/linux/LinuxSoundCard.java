/*
 * Copyright 2018-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.FileUtil;
import oshi.util.platform.linux.ProcPath;

/**
 * Sound card data obtained via /proc/asound directory
 */
@Immutable
final class LinuxSoundCard extends AbstractSoundCard {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxSoundCard.class);

    private static final String CARD_FOLDER = "card";
    private static final String CARDS_FILE = "cards";
    private static final String ID_FILE = "id";

    /**
     * Constructor for LinuxSoundCard.
     *
     * @param kernelVersion The version
     * @param name          The name
     * @param codec         The codec
     */
    LinuxSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * Method to find all the card folders contained in the <b>asound</b> folder denoting the cards currently contained
     * in our machine.
     *
     * @return : A list of files starting with 'card'
     */
    private static List<File> getCardFolders() {
        File cardsDirectory = new File(ProcPath.ASOUND);
        List<File> cardFolders = new ArrayList<>();
        File[] allContents = cardsDirectory.listFiles();
        if (allContents != null) {
            for (File card : allContents) {
                if (card.getName().startsWith(CARD_FOLDER) && card.isDirectory()) {
                    cardFolders.add(card);
                }
            }
        } else {
            LOG.warn("No Audio Cards Found");
        }
        return cardFolders;
    }

    /**
     * Reads the 'version' file in the asound folder that contains the complete name of the ALSA driver. Reads all the
     * lines of the file and retrieves the first line.
     *
     * @return The complete name of the ALSA driver currently residing in our machine
     */
    private static String getSoundCardVersion() {
        String driverVersion = FileUtil.getStringFromFile(ProcPath.ASOUND + "version");
        return driverVersion.isEmpty() ? "not available" : driverVersion;
    }

    /**
     * Retrieves the codec of the sound card contained in the <b>codec</b> file. The name of the codec is always the
     * first line of that file. <br>
     * <b>Working</b> <br>
     * This converts the codec file into key value pairs using the {@link FileUtil} class and then returns the value of
     * the <b>Codec</b> key.
     *
     * @param cardDir The sound card directory
     * @return The name of the codec
     */
    private static String getCardCodec(File cardDir) {
        String cardCodec = "";
        File[] cardFiles = cardDir.listFiles();
        if (cardFiles != null) {
            for (File file : cardFiles) {
                if (file.getName().startsWith("codec")) {
                    if (!file.isDirectory()) {
                        cardCodec = FileUtil.getKeyValueMapFromFile(file.getPath(), ":").get("Codec");
                    } else {
                        // on various centos environments, this is a subdirectory
                        // each file is usually named something like
                        // codec#0-0
                        // example : ac97#0-0
                        File[] codecs = file.listFiles();
                        if (codecs != null) {
                            for (File codec : codecs) {
                                if (!codec.isDirectory() && codec.getName().contains("#")) {
                                    cardCodec = codec.getName().substring(0, codec.getName().indexOf('#'));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return cardCodec;
    }

    /**
     * Retrieves the name of the sound card by :
     * <ol>
     * <li>Reading the <b>id</b> file and comparing each id with the card id present in the <b>cards</b> file</li>
     * <li>If the id and the card name matches , then it assigns that name to {@literal cardName}</li>
     * </ol>
     *
     * @param file The sound card File.
     * @return The name of the sound card.
     */
    private static String getCardName(File file) {
        String cardName = "Not Found..";
        Map<String, String> cardNamePairs = FileUtil.getKeyValueMapFromFile(ProcPath.ASOUND + "/" + CARDS_FILE, ":");
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
     *
     * @return List of {@link oshi.hardware.platform.linux.LinuxSoundCard} objects.
     */
    public static List<SoundCard> getSoundCards() {
        List<SoundCard> soundCards = new ArrayList<>();
        for (File cardFile : getCardFolders()) {
            soundCards.add(new LinuxSoundCard(getSoundCardVersion(), getCardName(cardFile), getCardCodec(cardFile)));
        }
        return soundCards;
    }
}
