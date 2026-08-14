package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTPositionAdditionalInfo;
import com.genersoft.iot.vmp.jt1078.bean.JTPositionBaseInfo;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

/**
 * Location information reporting
 *
 */
@Slf4j
@MsgId(id = "0200")
public class J0200 extends Re {

    private JTPositionBaseInfo positionInfo;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        positionInfo = JTPositionBaseInfo.decode(buf);
        if (log.isDebugEnabled()) {
            log.debug("[JT-location report]: phoneNumber={}  {}", header.getPhoneNumber(), positionInfo.toSimpleString());
        }
        // Read additional information
//        JTPositionAdditionalInfo positionAdditionalInfo = new JTPositionAdditionalInfo();
//        Map<Integer, byte[]> additionalMsg = new HashMap<>();
//        getAdditionalMsg(buf, positionAdditionalInfo);
//        log.info("[JT-location report]: phoneNumber={}  {}", header.getPhoneNumber(), positionInfo.toSimpleString());
        return null;
    }

    private void getAdditionalMsg(ByteBuf buf, JTPositionAdditionalInfo additionalInfo) {

        if (buf.isReadable()) {
            int msgId = buf.readUnsignedByte();
            int length = buf.readUnsignedByte();
            ByteBuf byteBuf = buf.readBytes(length);
            switch (msgId) {
                case 1:
                    // mileage
                    long mileage = byteBuf.readUnsignedInt();
                    log.info("[JT-location report]: mileage： {} km", (double)mileage/10);
                    break;
                case 2:
                    // Oil volume
                    int oil = byteBuf.readUnsignedShort();
                    log.info("[JT-location report]: Oil volume： {} L", (double)oil/10);
                    break;
                case 3:
                    // speed
                    int speed = byteBuf.readUnsignedShort();
                    log.info("[JT-location report]: speed： {} km/h", (double)speed/10);
                    break;
                case 4:
                    // Alarm events need to be manually confirmed ID
                    int alarmId = byteBuf.readUnsignedShort();
                    log.info("[JT-location report]: Alarm events need to be manually confirmed ID： {}", alarmId);
                    break;
                case 5:
                    byte[] tirePressureBytes = new byte[30];
                    // tire pressure
                    byteBuf.readBytes(tirePressureBytes);
                    log.info("[JT-location report]: tire pressure {}", tirePressureBytes);
                    break;
                case 6:
                    // Cabin temperature
                    short carriageTemperature = byteBuf.readShort();
                    log.info("[JT-location report]: Cabin temperature {}degrees celsius", carriageTemperature);
                    break;
                case 11:
                    // speed alarm
                    short positionType = byteBuf.readUnsignedByte();
                    long positionId = byteBuf.readUnsignedInt();
                    log.info("[JT-location report]: Speed alarm, location type: {}, area or segment ID: {}", positionType, positionId);
                    break;
                default:
                    log.info("[JT-location report]: additional messageID： {}， Message length： {}", msgId, length);
                    break;

            }
            getAdditionalMsg(buf, additionalInfo);
        }
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        j8001.setResult(J8001.SUCCESS);
        service.updateDevicePosition(header.getPhoneNumber(), positionInfo.getLongitude(), positionInfo.getLatitude());
        return j8001;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
