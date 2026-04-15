/*
 * Copyright 2018-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

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
import oshi.util.linux.ProcPath;

/**
 * Sound card data obtained via /proc/asound directory
 */
@Immutable
class LinuxSoundCard extends AbstractSoundCard {

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
     * @param asoundPath The path to the asound directory
     * @return : A list of files starting with 'card'
     */
    static List<File> getCardFolders(String asoundPath) {
        File cardsDirectory = new File(asoundPath);
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
     * @param asoundPath The path to the asound directory
     * @return The complete name of the ALSA driver currently residing in our machine
     */
    static String getSoundCardVersion(String asoundPath) {
        String driverVersion = FileUtil.getStringFromFile(new File(asoundPath, "version").getPath());
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
    static String getCardCodec(File cardDir) {
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
     * @param file       The sound card File.
     * @param asoundPath The path to the asound directory
     * @return The name of the sound card.
     */
    static String getCardName(File file, String asoundPath) {
        String cardName = "Not Found..";
        Map<String, String> cardNamePairs = FileUtil.getKeyValueMapFromFile(new File(asoundPath, CARDS_FILE).getPath(),
                ":");
        String cardId = FileUtil.getStringFromFile(new File(file, ID_FILE).getPath());
        for (Map.Entry<String, String> entry : cardNamePairs.entrySet()) {
            if (entry.getKey().contains(cardId)) {
                cardName = entry.getValue();
                return cardName;
            }
        }
        return cardName;
    }

    /**
     * public method used by {@code AbstractHardwareAbstractionLayer} to access the sound cards.
     *
     * @return List of {@link LinuxSoundCard} objects.
     */
    public static List<SoundCard> getSoundCards() {
        return getSoundCards(ProcPath.ASOUND);
    }

    static List<SoundCard> getSoundCards(String asoundPath) {
        List<SoundCard> soundCards = new ArrayList<>();
        String version = getSoundCardVersion(asoundPath);
        for (File cardFile : getCardFolders(asoundPath)) {
            soundCards.add(new LinuxSoundCard(version, getCardName(cardFile, asoundPath), getCardCodec(cardFile)));
        }
        return soundCards;
    }
}
