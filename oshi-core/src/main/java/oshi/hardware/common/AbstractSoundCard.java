package oshi.hardware.common;

import oshi.hardware.SoundCard;

public abstract class AbstractSoundCard implements SoundCard {

    private String kernelVersion;
    private String name;
    private String codec;

    public AbstractSoundCard(String kernelVersion, String name, String codec) {
        this.kernelVersion = kernelVersion;
        this.name = name;
        this.codec = codec;
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SOUND CARDS DRIVER VERSION : " + kernelVersion);
        builder.append("\n");
        builder.append("NAME  ---: " + name);
        builder.append("\n");
        builder.append("CODEC ---: " + codec);
        builder.append("\n");
        return builder.toString();
    }

}
