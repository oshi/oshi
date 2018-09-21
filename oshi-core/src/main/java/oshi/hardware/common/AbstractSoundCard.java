package oshi.hardware.common;

import oshi.hardware.SoundCard;

public abstract class AbstractSoundCard implements SoundCard {

    private String kernelVersion;


    private String name;
    private String codec;
    private SoundCard[] devices;

    public AbstractSoundCard(String kernelVersion, String name, String codec, SoundCard[] devices) {
        this.kernelVersion = kernelVersion;
        this.name = name;
        this.codec = codec;
        this.devices = devices;
    }

    @Override
    public String getDriverVersion() {
        return kernelVersion;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    @Override
    public SoundCard[] getDevices() {
        return devices;
    }

    public void setDevices(SoundCard[] devices) {
        this.devices = devices;
    }


}
