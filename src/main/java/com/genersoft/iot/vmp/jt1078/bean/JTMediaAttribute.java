package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.config.JTDeviceSubConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Terminal upload audio and video attributes
 */
@Setter
@Getter
public class JTMediaAttribute implements JTDeviceSubConfig {

    /**
     * Enter audio encoding method:
     * 1 G. 721
     * 2 G. 722
     * 3 G. 723
     * 4 G. 728
     * 5 G. 729
     * 6 G. 711A
     * 7 G. 711U
     * 8 G. 726
     * 9 G. 729A
     * 10 DVI4_3
     * 11 DVI4_4
     * 12 DVI4_8K
     * 13 DVI4_16K
     * 14 LPC
     * 15 S16BE_STEREO
     * 16 S16BE_MONO
     * 17 MPEGAUDIO
     * 18 LPCM
     * 19 AAC
     * 20 WMA9STD
     * 21 HEAAC
     * 22 PCM_VOICE
     * 23 PCM_AUDIO
     * 24 AACLC
     * 25 MP3
     * 26 ADPCMA
     * 27 MP4AUDIO
     * 28 AMR
     */
    private int audioEncoder;

    /**
     * Enter the number of audio channels
     */
    private int audioChannels;

    /**
     * Input audio sample rate:
     * 0:8 kHz;
     * 1:22. 05 kHz;
     * 2:44. 1 kHz;
     * 3:48 kHz
     */
    private int audioSamplingRate;

    /**
     * Input audio sample bits:
     * 0:8 Bit;
     * 1:16 Bit;
     * 2:32 Bit
     */
    private int audioSamplingBits;

    /**
     * Audio frame length: range 1 ~ 4 294 967 295
     */
    private int audioFrameLength;

    /**
     * Whether to support audio output:
     * 0:Not supported;1:support
     */
    private int audioOutputEnable;

    /**
     * Video encoding method:
     * 98 H. 264
     * 99 H. 265
     * 100 AVS
     * 101 SVAC
     */
    private int videoEncoder;

    /**
     * The maximum number of audio physical channels supported by the terminal:
     */
    private int audioChannelMax;

    /**
     * The maximum number of video physical channels supported by the terminal:
     */
    private int videoChannelMax;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(audioEncoder);
        byteBuf.writeByte(audioChannels);
        byteBuf.writeByte(audioSamplingRate);
        byteBuf.writeByte(audioSamplingBits);
        byteBuf.writeShort(audioFrameLength);
        byteBuf.writeByte(audioOutputEnable);
        byteBuf.writeByte(videoEncoder);
        byteBuf.writeByte(audioChannelMax);
        byteBuf.writeByte(videoChannelMax);
        return byteBuf;
    }

    public static JTMediaAttribute decode(ByteBuf byteBuf) {
        JTMediaAttribute jtMediaAttribute = new JTMediaAttribute();
        jtMediaAttribute.setAudioEncoder(byteBuf.readUnsignedByte());
        jtMediaAttribute.setAudioChannels(byteBuf.readUnsignedByte());
        jtMediaAttribute.setAudioSamplingRate(byteBuf.readUnsignedByte());
        jtMediaAttribute.setAudioSamplingBits(byteBuf.readUnsignedByte());
        jtMediaAttribute.setAudioFrameLength(byteBuf.readUnsignedShort());
        jtMediaAttribute.setAudioOutputEnable(byteBuf.readUnsignedByte());
        jtMediaAttribute.setVideoEncoder(byteBuf.readUnsignedByte());
        jtMediaAttribute.setAudioChannelMax(byteBuf.readUnsignedByte());
        jtMediaAttribute.setVideoChannelMax(byteBuf.readUnsignedByte());
        return jtMediaAttribute;
    }
}
