package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Audio and video channels
 */
@Setter
@Getter
public class JTChanelConfig implements JTDeviceSubConfig{

    /**
     * Physical channel number alone
     */
    private int physicalChannelId;

    /**
     * Logical channel number
     */
    private int logicChannelId;

    /**
     * Channel type:
     * 0:Audio and video;
     * 1:Audio
     * 2:video
     */
    private int channelType;
    /**
     * Whether to connect to the PTZ: This field is valid when the channel type is 0 and 2
     * 0:Not connected;1:connect
     */
    private int ptzEnable;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(physicalChannelId);
        byteBuf.writeByte(logicChannelId);
        byteBuf.writeByte(channelType);
        byteBuf.writeByte(ptzEnable);
        return byteBuf;
    }

    public static JTChanelConfig decode(ByteBuf byteBuf) {
        JTChanelConfig jtChanel = new JTChanelConfig();
        jtChanel.setPhysicalChannelId(byteBuf.readUnsignedByte());
        jtChanel.setLogicChannelId(byteBuf.readUnsignedByte());
        jtChanel.setChannelType(byteBuf.readUnsignedByte());
        jtChanel.setPtzEnable(byteBuf.readUnsignedByte());
        return jtChanel;
    }
}
