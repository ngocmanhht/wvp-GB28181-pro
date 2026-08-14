package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Terminal universal response
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:04
 * @email qingtaij@163.com
 */
@Getter
@MsgId(id = "0001")
public class J0001 extends Re {
    int respNo;
    String respId;
    /**
     * 0：success/Confirm; 1: Failed; 2: The message is wrong; 3: Not supported
     */
    int result;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        respNo = buf.readUnsignedShort();
        respId = ByteBufUtil.hexDump(buf.readSlice(2));
        result = buf.readUnsignedByte();
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        SessionManager.INSTANCE.response(header.getPhoneNumber(), "0001", (long) respNo, result);
        return null;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
