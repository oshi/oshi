/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.common;

import oshi.hardware.SoundCard;

/**
 * An abstract Sound Card
 */
public abstract class AbstractSoundCard implements SoundCard {

    private String kernelVersion;
    private String name;
    private String codec;

    /**
     * <p>
     * Constructor for AbstractSoundCard.
     * </p>
     *
     * @param kernelVersion
     *            a {@link java.lang.String} object.
     * @param name
     *            a {@link java.lang.String} object.
     * @param codec
     *            a {@link java.lang.String} object.
     */
    public AbstractSoundCard(String kernelVersion, String name, String codec) {
        this.kernelVersion = kernelVersion;
        this.name = name;
        this.codec = codec;
    }

    /** {@inheritDoc} */
    @Override
    public String getDriverVersion() {
        return this.kernelVersion;
    }

    /**
     * <p>
     * Setter for the field <code>kernelVersion</code>.
     * </p>
     *
     * @param kernelVersion
     *            a {@link java.lang.String} object.
     */
    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * Setter for the field <code>name</code>.
     * </p>
     *
     * @param name
     *            a {@link java.lang.String} object.
     */
    public void setName(String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String getCodec() {
        return this.codec;
    }

    /**
     * <p>
     * Setter for the field <code>codec</code>.
     * </p>
     *
     * @param codec
     *            a {@link java.lang.String} object.
     */
    public void setCodec(String codec) {
        this.codec = codec;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SoundCard@");
        builder.append(Integer.toHexString(hashCode()));
        builder.append(" [kernelVersion=");
        builder.append(this.kernelVersion);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", codec=");
        builder.append(this.codec);
        builder.append(']');
        return builder.toString();
    }

}
