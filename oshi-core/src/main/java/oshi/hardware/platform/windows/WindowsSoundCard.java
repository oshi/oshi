package oshi.hardware.platform.windows;

import oshi.hardware.common.AbstractSoundCard;

/**
 * @author : BilalAM
 */
public class WindowsSoundCard extends AbstractSoundCard {
        public WindowsSoundCard(String kernelVersion, String name, String codec) {
                super(kernelVersion, name, codec);
        }
}
