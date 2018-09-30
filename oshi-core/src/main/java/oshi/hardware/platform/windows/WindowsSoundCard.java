package oshi.hardware.platform.windows;

import oshi.hardware.common.AbstractSoundCard;
import oshi.jna.platform.windows.WbemcliUtil;
import oshi.util.platform.windows.WmiUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : BilalAM
 */
public class WindowsSoundCard extends AbstractSoundCard {

    enum AudioCardName {
        MANUFACTURER, NAME
    }

    private static final String AUDIO_CARD = "Win32_SoundDevice";
    private static final WbemcliUtil.WmiQuery<AudioCardName> AUDIO_CARD_QUERY =
            new WbemcliUtil.WmiQuery<>(AUDIO_CARD, AudioCardName.class);
    private static final WbemcliUtil.WmiResult<AudioCardName> AUDIO_CARD_QUERY_RESULT = WmiUtil.queryWMI(AUDIO_CARD_QUERY);


    public WindowsSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    private static String getAudioCardCompleteName() {
        return String.valueOf(AUDIO_CARD_QUERY_RESULT.getValue(AudioCardName.MANUFACTURER, 0)) + " " + String.valueOf(AUDIO_CARD_QUERY_RESULT.getValue(AudioCardName.NAME, 0));
    }


    public List<WindowsSoundCard> getSoundCards() {
        List<WindowsSoundCard> cards = new ArrayList<>();
        for (int i = 0; i < AUDIO_CARD_QUERY_RESULT.getResultCount(); i++) {
            cards.add(new WindowsSoundCard(null, getAudioCardCompleteName(), null));
        }
        return cards;
    }

    public static void main(String[] args) {
        System.out.println(getAudioCardCompleteName());
    }

}
