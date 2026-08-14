package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Query area or line data response
 */
@Slf4j
@MsgId(id = "0608")
public class J0608 extends Re {

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        int type = buf.readByte();
        long dataLength = buf.readUnsignedInt();
        log.info("[JT-Query area or line data response]: Type： {}， Quantity： {}", type, dataLength);
        List<JTAreaOrRoute> areaOrRoutes = new ArrayList<>();
        if (dataLength == 0) {
            SessionManager.INSTANCE.response(header.getPhoneNumber(), "0608", null, areaOrRoutes);
            return null;
        }
        switch (type) {
            case 1:
                buf.readUnsignedByte();
                int areaLengthForCircleArea = buf.readUnsignedByte();
                List<JTCircleArea> jtCircleAreas = new ArrayList<>();
                for (int i = 0; i < areaLengthForCircleArea; i++) {
                    // Query circular area data
                    JTCircleArea jtCircleArea = JTCircleArea.decode(buf);
                    jtCircleAreas.add(jtCircleArea);
                }
                SessionManager.INSTANCE.response(header.getPhoneNumber(), "0608", null, jtCircleAreas);
                break;
            case 2:
                buf.readUnsignedByte();
                int areaLengthForRectangleArea = buf.readUnsignedByte();
                // Query rectangular area data
                List<JTRectangleArea> jtRectangleAreas = new ArrayList<>();
                for (int i = 0; i < areaLengthForRectangleArea; i++) {
                    // Query circular area data
                    JTRectangleArea jtRectangleArea = JTRectangleArea.decode(buf);
                    jtRectangleAreas.add(jtRectangleArea);
                }
                SessionManager.INSTANCE.response(header.getPhoneNumber(), "0608", null, jtRectangleAreas);
                break;
            case 3:
                // Query polygon area data
                List<JTPolygonArea> jtPolygonAreas = new ArrayList<>();
                for (int i = 0; i < dataLength; i++) {
                    // Query circular area data
                    JTPolygonArea jtRectangleArea = JTPolygonArea.decode(buf);
                    jtPolygonAreas.add(jtRectangleArea);
                }
                SessionManager.INSTANCE.response(header.getPhoneNumber(), "0608", null, jtPolygonAreas);
                break;
            case 4:
                // Query line data
                List<JTRoute> jtRoutes = new ArrayList<>();
                for (int i = 0; i < dataLength; i++) {
                    // Query circular area data
                    JTRoute jtRoute = JTRoute.decode(buf);
                    jtRoutes.add(jtRoute);
                }
                SessionManager.INSTANCE.response(header.getPhoneNumber(), "0608", null, jtRoutes);
                break;
            default:
                break;
        }

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
