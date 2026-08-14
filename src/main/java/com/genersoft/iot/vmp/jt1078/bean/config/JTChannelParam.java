package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Individual video channel parameter settings
 */
@Setter
@Getter
public class JTChannelParam implements JTDeviceSubConfig {

    /**
     * Individual channel video parameter setting list
     */
    private List<JTAloneChanel> jtAloneChanelList;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(jtAloneChanelList.size());
        for (JTAloneChanel jtAloneChanel : jtAloneChanelList) {
            if (jtAloneChanel == null) {
                continue;
            }
            byteBuf.writeBytes(jtAloneChanel.encode());
        }
        return byteBuf;
    }

    public static JTChannelParam decode(ByteBuf byteBuf) {
        JTChannelParam channelParam = new JTChannelParam();
        int length = byteBuf.readUnsignedByte();
        List<JTAloneChanel> jtAloneChanelList = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            jtAloneChanelList.add(JTAloneChanel.decode(byteBuf));
        }
        channelParam.setJtAloneChanelList(jtAloneChanelList);
        return channelParam;
    }
}
