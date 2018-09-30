package oshi.hardware.platform.windows;

import oshi.hardware.common.AbstractSoundCard;
import oshi.jna.platform.windows.WbemcliUtil;
import oshi.util.platform.windows.WmiUtil;

/**
 * @author : BilalAM
 */
public class WindowsSoundCard extends AbstractSoundCard {

    enum AudioCardCaption {
        MANUFACTURER, NAME
    }

    private static final WbemcliUtil.WmiQuery<AudioCardCaption> AUDIO_CARD_QUERY =
            new WbemcliUtil.WmiQuery<>("Win32_SoundDevice", AudioCardCaption.class);

    public WindowsSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    private static String getAudioCardCompleteName() {
        WbemcliUtil.WmiResult<AudioCardCaption> r = WmiUtil.queryWMI(AUDIO_CARD_QUERY);
        return String.valueOf(r.getValue(AudioCardCaption.MANUFACTURER, 0)) + " " + String.valueOf(r.getValue(AudioCardCaption.NAME, 0));
    }

}
