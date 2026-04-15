/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static oshi.hardware.common.platform.linux.TestFileUtil.writeFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import oshi.hardware.SoundCard;

class LinuxSoundCardTest {

    // -------------------------------------------------------------------------
    // getCardFolders
    // -------------------------------------------------------------------------

    @Test
    void testGetCardFoldersNonexistentPath(@TempDir Path tempDir) {
        List<File> folders = LinuxSoundCard.getCardFolders(tempDir.resolve("missing").toString());
        assertThat(folders, is(empty()));
    }

    @Test
    void testGetCardFoldersEmptyDir(@TempDir Path tempDir) throws IOException {
        Path asound = tempDir.resolve("asound");
        Files.createDirectories(asound);
        List<File> folders = LinuxSoundCard.getCardFolders(asound.toString());
        assertThat(folders, is(empty()));
    }

    @Test
    void testGetCardFoldersFiltersCorrectly(@TempDir Path tempDir) throws IOException {
        Path asound = tempDir.resolve("asound");
        Files.createDirectories(asound.resolve("card0"));
        Files.createDirectories(asound.resolve("card1"));
        // Not a card directory — should be excluded
        Files.createDirectories(asound.resolve("timers"));
        // File starting with "card" but not a directory
        writeFile(asound.resolve("cards"), "content");

        List<File> folders = LinuxSoundCard.getCardFolders(asound.toString());
        assertThat(folders, hasSize(2));
        List<String> names = new ArrayList<>();
        for (File f : folders) {
            names.add(f.getName());
        }
        assertThat(names, containsInAnyOrder("card0", "card1"));
    }

    // -------------------------------------------------------------------------
    // getSoundCardVersion
    // -------------------------------------------------------------------------

    @Test
    void testGetSoundCardVersionPresent(@TempDir Path tempDir) throws IOException {
        Path asound = tempDir.resolve("asound/");
        Files.createDirectories(asound);
        writeFile(asound.resolve("version"), "Advanced Linux Sound Architecture Driver Version k5.15.0.");

        String version = LinuxSoundCard.getSoundCardVersion(asound.toString());
        assertThat(version, is("Advanced Linux Sound Architecture Driver Version k5.15.0."));
    }

    @Test
    void testGetSoundCardVersionMissing(@TempDir Path tempDir) {
        String version = LinuxSoundCard.getSoundCardVersion(tempDir.resolve("asound").toString());
        assertThat(version, is("not available"));
    }

    // -------------------------------------------------------------------------
    // getCardCodec
    // -------------------------------------------------------------------------

    @Test
    void testGetCardCodecFromFile(@TempDir Path tempDir) throws IOException {
        Path card0 = tempDir.resolve("card0");
        Files.createDirectories(card0);
        writeFile(card0.resolve("codec#0"), "Codec: Realtek ALC892\nAddress: 0\nVendor Id: 0x10ec0892");

        String codec = LinuxSoundCard.getCardCodec(card0.toFile());
        assertThat(codec, is("Realtek ALC892"));
    }

    @Test
    void testGetCardCodecFromSubdirectory(@TempDir Path tempDir) throws IOException {
        // CentOS-style: codec is a directory containing files like "codec#0-0"
        Path card0 = tempDir.resolve("card0");
        Path codecDir = card0.resolve("codec");
        Files.createDirectories(codecDir);
        writeFile(codecDir.resolve("ac97#0-0"), "dummy");

        String codec = LinuxSoundCard.getCardCodec(card0.toFile());
        assertThat(codec, is("ac97"));
    }

    @Test
    void testGetCardCodecNoCodecFile(@TempDir Path tempDir) throws IOException {
        Path card0 = tempDir.resolve("card0");
        Files.createDirectories(card0);
        writeFile(card0.resolve("pcm0p"), "dummy");

        String codec = LinuxSoundCard.getCardCodec(card0.toFile());
        assertThat(codec, is(""));
    }

    @Test
    void testGetCardCodecEmptyDir(@TempDir Path tempDir) throws IOException {
        Path card0 = tempDir.resolve("card0");
        Files.createDirectories(card0);

        String codec = LinuxSoundCard.getCardCodec(card0.toFile());
        assertThat(codec, is(""));
    }

    // -------------------------------------------------------------------------
    // getCardName
    // -------------------------------------------------------------------------

    @Test
    void testGetCardNameMatches(@TempDir Path tempDir) throws IOException {
        Path asound = tempDir.resolve("asound");
        Path card0 = asound.resolve("card0");
        Files.createDirectories(card0);
        writeFile(card0.resolve("id"), "PCH");
        // cards file: key contains the card id, value is the name
        writeFile(asound.resolve("cards"), " 0 [PCH            ]: HDA-Intel - HDA Intel PCH");

        String name = LinuxSoundCard.getCardName(card0.toFile(), asound.toString());
        assertThat(name, is("HDA-Intel - HDA Intel PCH"));
    }

    @Test
    void testGetCardNameNoMatch(@TempDir Path tempDir) throws IOException {
        Path asound = tempDir.resolve("asound");
        Path card0 = asound.resolve("card0");
        Files.createDirectories(card0);
        writeFile(card0.resolve("id"), "HDMI");
        writeFile(asound.resolve("cards"), " 0 [PCH            ]: HDA-Intel - HDA Intel PCH");

        String name = LinuxSoundCard.getCardName(card0.toFile(), asound.toString());
        assertThat(name, is("Not Found.."));
    }

    // -------------------------------------------------------------------------
    // getSoundCards (integration)
    // -------------------------------------------------------------------------

    @Test
    void testGetSoundCardsIntegration(@TempDir Path tempDir) throws IOException {
        Path asound = tempDir.resolve("asound/");
        Files.createDirectories(asound);
        writeFile(asound.resolve("version"), "ALSA v1.2.6");

        Path card0 = asound.resolve("card0");
        Files.createDirectories(card0);
        writeFile(card0.resolve("id"), "PCH");
        writeFile(card0.resolve("codec#0"), "Codec: ALC892\nAddress: 0");
        writeFile(asound.resolve("cards"), " 0 [PCH            ]: HDA-Intel - HDA Intel PCH");

        List<SoundCard> cards = LinuxSoundCard.getSoundCards(asound.toString());
        assertThat(cards, hasSize(1));
        SoundCard sc = cards.get(0);
        assertThat(sc.getDriverVersion(), is("ALSA v1.2.6"));
        assertThat(sc.getName(), is("HDA-Intel - HDA Intel PCH"));
        assertThat(sc.getCodec(), is("ALC892"));
    }

    @Test
    void testGetSoundCardsEmptyDir(@TempDir Path tempDir) throws IOException {
        Path asound = tempDir.resolve("asound");
        Files.createDirectories(asound);

        List<SoundCard> cards = LinuxSoundCard.getSoundCards(asound.toString());
        assertThat(cards, is(empty()));
    }
}
