package oshi.hardware.platform.windows;

import oshi.hardware.SoundCard;
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

    private static WbemcliUtil.WmiQuery<AudioCardName> AUDIO_CARD_QUERY;

    private static final String AUDIO_CARD = "Win32_SoundDevice";
    private static WbemcliUtil.WmiQuery<AudioCardKernel> AUDIO_CARD_KERNAL_QUERY;
    private static WbemcliUtil.WmiResult<AudioCardName> AUDIO_CARD_QUERY_RESULT;
    private static WbemcliUtil.WmiResult<AudioCardKernel> AUDIO_CARD_KERNEL_QUERY_RESULT;

    static {
        AUDIO_CARD_QUERY =
                new WbemcliUtil.WmiQuery<>(AUDIO_CARD, AudioCardName.class);
        AUDIO_CARD_QUERY_RESULT = WmiUtil.queryWMI(AUDIO_CARD_QUERY);
    }

    private static String getAudioCardCompleteName(int index) {
        return String.valueOf(AUDIO_CARD_QUERY_RESULT.getValue(AudioCardName.MANUFACTURER, 0)) + " " + String.valueOf(AUDIO_CARD_QUERY_RESULT.getValue(AudioCardName.NAME, 0));
    }

    public WindowsSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    private static String getAudioCardKernelVersion(int index) {
        String audioCardKernel = "";
        if (AUDIO_CARD_QUERY_RESULT.getResultCount() == 0) {
            // log warning.
        } else {
            AUDIO_CARD_KERNAL_QUERY = new WbemcliUtil.WmiQuery<>("Win32_PnPSignedDriver WHERE DeviceName=\""
                    + AUDIO_CARD_QUERY_RESULT.getValue(AudioCardName.NAME, index)
                    + "\"", AudioCardKernel.class);
            AUDIO_CARD_KERNEL_QUERY_RESULT = WmiUtil.queryWMI(AUDIO_CARD_KERNAL_QUERY);
            audioCardKernel =
                    String.valueOf(AUDIO_CARD_KERNEL_QUERY_RESULT.getValue(AudioCardKernel.DRIVERPROVIDERNAME, index)) + " "
                            + String.valueOf(AUDIO_CARD_KERNEL_QUERY_RESULT.getValue(AudioCardKernel.DEVICENAME, index)) + " "
                            + String.valueOf(AUDIO_CARD_KERNEL_QUERY_RESULT.getValue(AudioCardKernel.DRIVERNAME, index)) + " "
                            + String.valueOf(AUDIO_CARD_KERNEL_QUERY_RESULT.getValue(AudioCardKernel.DRIVERVERSION, index));
        }
        return audioCardKernel;
    }

    public static List<WindowsSoundCard> getSoundCards() {
        List<WindowsSoundCard> cards = new ArrayList<>();
        for (int i = 0; i < AUDIO_CARD_QUERY_RESULT.getResultCount(); i++) {
            cards.add(new WindowsSoundCard(getAudioCardKernelVersion(i), getAudioCardCompleteName(i), "bla"));
        }
        return cards;
    }

    public static void main(String[] args) {
        for (SoundCard cards : getSoundCards()) {
            System.out.println(cards.toString());
        }
    }

    enum AudioCardKernel {
        DRIVERPROVIDERNAME, DRIVERNAME, DRIVERVERSION, DEVICENAME
    }

}
