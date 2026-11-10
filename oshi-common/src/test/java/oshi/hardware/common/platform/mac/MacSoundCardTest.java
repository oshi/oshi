/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.hardware.SoundCard;

class MacSoundCardTest {

    @Test
    void testAudioControlChipsOnePerPart() {
        // Shape of a 16-inch MacBook Pro (M2 Max): one headphone codec and six speaker amplifiers on I2C, among
        // unrelated I2C devices
        IOKitProvider ioKit = new IicProvider(
                List.of(new IicEntry("audio-speaker-right-woofer-1", "audio-control,sn012776"),
                        new IicEntry("audio-speaker", "audio-control,sn012776"),
                        new IicEntry("audio-speaker-left-woofer-2", "audio-control,sn012776"),
                        new IicEntry("audio-codec-output", "audio-control,cs42l84"),
                        new IicEntry("audio-speaker-right-tweeter", "audio-control,sn012776"),
                        new IicEntry("hpmBusManager", "usbc,manager"), new IicEntry("i2c-nameless", "")));

        List<SoundCard> cards = MacSoundCard.queryAudioControlChips(ioKit, "AppleEmbeddedAudio 940.17");
        assertThat(cards.stream().map(SoundCard::getCodec).toList(), contains("sn012776", "cs42l84"));
        assertThat(cards.stream().map(SoundCard::getName).toList(), contains("audio-speaker", "audio-codec-output"));
        assertThat(cards.stream().map(SoundCard::getDriverVersion).toList(),
                everyItem(is("AppleEmbeddedAudio 940.17")));
    }

    @Test
    void testNoAudioChips() {
        assertThat(MacSoundCard.queryAudioControlChips(new IicProvider(List.of()), "AppleEmbeddedAudio"), is(empty()));
    }

    @Test
    void testPlistValue() {
        List<String> plist = List.of("<dict>", "\t<key>CFBundleShortVersionString</key>", "\t<string>940.17</string>",
                "\t<key>com.apple.driver.AppleHDAController</key>", "\t<string>1.7.2a1</string>", "</dict>");
        assertThat(MacSoundCard.plistValue(plist, "CFBundleShortVersionString"), is("940.17"));
        assertThat(MacSoundCard.plistValue(plist, "com.apple.driver.AppleHDAController"), is("1.7.2a1"));
        assertThat(MacSoundCard.plistValue(plist, "Missing"), is(""));
        assertThat(MacSoundCard.plistValue(List.of(), "CFBundleShortVersionString"), is(""));
    }

    private record IicEntry(String name, String compatible) implements IOKitProvider.RegistryEntry {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public byte[] getByteArrayProperty(String key) {
            // Device tree strings carry a trailing NUL
            return "compatible".equals(key) ? (compatible + "\0").getBytes(StandardCharsets.UTF_8) : new byte[0];
        }

        @Override
        public String getStringProperty(String key) {
            return "";
        }

        @Override
        public Integer getIntegerProperty(String key) {
            return 0;
        }

        @Override
        public Boolean getBooleanProperty(String key) {
            return Boolean.FALSE;
        }
    }

    private record IicProvider(List<IicEntry> entries) implements IOKitProvider {

        @Override
        public <T> @Nullable T withMatchingService(String serviceName, Function<RegistryEntry, @Nullable T> extractor) {
            return entries.isEmpty() ? null : extractor.apply(entries.get(0));
        }

        @Override
        public void forEachMatchingService(String serviceName, Consumer<RegistryEntry> consumer) {
            entries.forEach(consumer);
        }

        @Override
        public void forEachMatchingServiceUntil(String serviceName, Predicate<RegistryEntry> visitor) {
            for (IicEntry entry : entries) {
                if (visitor.test(entry)) {
                    break;
                }
            }
        }
    }
}
