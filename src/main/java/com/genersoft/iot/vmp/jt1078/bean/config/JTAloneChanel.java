package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * separate channel video
 */
@Setter
@Getter
public class JTAloneChanel implements JTDeviceSubConfig{

    /**
     * Logical channel number
     */
    private int logicChannelId;

    /**
     * Live streaming encoding mode
     * 0:CBR( Fixed code rate) ;
     * 1:VBR( variable code rate) ;
     * 2:ABR( average code rate) ;
     * 100 ~ 127:Customize
     */
    private int liveStreamCodeRateType;

    /**
     * Live stream resolution
     * 0:QCIF;
     * 1:CIF;
     * 2:WCIF;
     * 3:D1;
     * 4:WD1;
     * 5:720P;
     * 6:1 080P;
     * 100 ~ 127:Customize
     */
    private int liveStreamResolving;

    /**
     * Live stream keyframe interval, range(1 ~ 1 000) frame
     */
    private int liveStreamIInterval;

    /**
     * Live streaming target frame rate, range(1 ~ 120) frame / s
     */
    private int liveStreamFrameRate;

    /**
     * Real-time streaming target bitrate, in kilobits per second( kbps)
     */
    private long liveStreamCodeRate;


    /**
     * Storage stream encoding mode
     * 0:CBR( Fixed code rate) ;
     * 1:VBR( variable code rate) ;
     * 2:ABR( average code rate) ;
     * 100 ~ 127:Customize
     */
    private int storageStreamCodeRateType;

    /**
     * Storage stream resolution
     * 0:QCIF;
     * 1:CIF;
     * 2:WCIF;
     * 3:D1;
     * 4:WD1;
     * 5:720P;
     * 6:1 080P;
     * 100 ~ 127:Customize
     */
    private int storageStreamResolving;

    /**
     * Storage stream keyframe interval, range(1 ~ 1 000) frame
     */
    private int storageStreamIInterval;

    /**
     * Storage stream target frame rate, range(1 ~ 120) frame / s
     */
    private int storageStreamFrameRate;

    /**
     * Storage stream target bit rate, in kilobits per second( kbps)
     */
    private long storageStreamCodeRate;

    /**
     * Subtitle overlay settings
     */
    private JTOSDConfig osd;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(logicChannelId);
        byteBuf.writeByte(liveStreamCodeRateType);
        byteBuf.writeByte(liveStreamResolving);
        byteBuf.writeShort((short)(liveStreamIInterval & 0xffff));
        byteBuf.writeByte(liveStreamFrameRate);
        byteBuf.writeInt((int) (liveStreamCodeRate & 0xffffffffL));

        byteBuf.writeByte(storageStreamCodeRateType);
        byteBuf.writeByte(storageStreamResolving);
        byteBuf.writeShort((short)(storageStreamIInterval & 0xffff));
        byteBuf.writeByte(storageStreamFrameRate);
        byteBuf.writeInt((int) (storageStreamCodeRate & 0xffffffffL));
        byteBuf.writeBytes(osd.encode());
        return byteBuf;
    }

    public static JTAloneChanel decode(ByteBuf buf) {
        JTAloneChanel jtAloneChanel = new JTAloneChanel();
        jtAloneChanel.setLogicChannelId(buf.readByte());
        jtAloneChanel.setLiveStreamCodeRateType(buf.readByte());
        jtAloneChanel.setLiveStreamResolving(buf.readByte());
        jtAloneChanel.setLiveStreamIInterval(buf.readUnsignedShort());
        jtAloneChanel.setLiveStreamFrameRate(buf.readByte());
        jtAloneChanel.setLiveStreamCodeRate(buf.readUnsignedInt());

        jtAloneChanel.setStorageStreamCodeRateType(buf.readByte());
        jtAloneChanel.setStorageStreamResolving(buf.readByte());
        jtAloneChanel.setStorageStreamIInterval(buf.readUnsignedShort());
        jtAloneChanel.setStorageStreamFrameRate(buf.readByte());
        jtAloneChanel.setStorageStreamCodeRate(buf.readUnsignedInt());
        jtAloneChanel.setOsd(JTOSDConfig.decode(buf));
        return null;
    }
}
