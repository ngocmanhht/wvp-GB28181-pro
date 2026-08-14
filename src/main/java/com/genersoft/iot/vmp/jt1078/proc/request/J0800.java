package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTMediaEventInfo;
import com.genersoft.iot.vmp.jt1078.bean.JTPositionBaseInfo;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Multimedia event information upload
 *
 */
@Slf4j
@MsgId(id = "0800")
public class J0800 extends Re {

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        JTMediaEventInfo mediaEventInfo = JTMediaEventInfo.decode(buf);
        log.info("[JT-Multimedia event information upload]: {}", mediaEventInfo);
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        return j8001;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
