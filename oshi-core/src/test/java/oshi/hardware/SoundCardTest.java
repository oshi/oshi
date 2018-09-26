package oshi.hardware;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

import oshi.SystemInfo;

public class SoundCardTest {


    @Test
    public void testSoundCards() {
        SystemInfo info = new SystemInfo();
        for (SoundCard soundCard : info.getHardware().getSoundCards()) {
            assertNotNull(soundCard.toString());
        }
    }

}
