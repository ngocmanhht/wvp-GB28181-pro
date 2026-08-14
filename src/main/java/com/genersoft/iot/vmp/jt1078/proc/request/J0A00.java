package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * Terminal RSA public key
 */
@Setter
@Getter
@MsgId(id = "0900")
public class J0A00 extends Re {

    /**
     * Transparent transmission message type, 0x00: GNSS module detailed positioning data, 0X0B: Road transportation certificate IC card information, 0X41: Serial port 1 transparent transmission, 0X42: Serial port 2 transparent transmission, 0XF0 ~ 0XFF: User-defined transparent transmission
     */

    private Integer type;

    /**
     * Transparent message content
     */
    private byte[] content;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        type = (int)buf.readUnsignedByte();
        byte[] content = new byte[buf.readableBytes()];
        buf.readBytes(content);
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        return null;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }

}
