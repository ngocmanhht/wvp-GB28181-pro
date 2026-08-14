package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import io.netty.buffer.ByteBuf;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

@Setter
@Getter
@Slf4j
@Schema(description = "Basic location information")
public class JTPositionBaseInfo {

    /**
     * Alarm sign
     */
    @Schema(description = "Alarm sign")
    private JTAlarmSign alarmSign;

    /**
     * Status
     */
    @Schema(description = "Status")
    private JTStatus status;

    /**
     * longitude
     */
    @Schema(description = "longitude")
    private Double longitude;

    /**
     * Latitude
     */
    @Schema(description = "Latitude")
    private Double latitude;

    /**
     * elevation
     */
    @Schema(description = "elevation")
    private Integer altitude;

    /**
     * speed
     */
    @Schema(description = "speed")
    private Integer speed;

    /**
     * direction
     */
    @Schema(description = "direction")
    private Integer direction;

    /**
     * time
     */
    @Schema(description = "time")
    private String time;

    /**
     * Video alarm
     */
    @Schema(description = "Video alarm")
    private JTVideoAlarm videoAlarm;

    public static JTPositionBaseInfo decode(ByteBuf buf) {
        JTPositionBaseInfo positionInfo = new JTPositionBaseInfo();
        if (buf.readableBytes() < 17) {
            log.error("[Basic location information] Decoding failed, insufficient length: {}", buf.readableBytes());
            return positionInfo;
        }
        positionInfo.setAlarmSign(new JTAlarmSign(buf.readUnsignedInt()));

        positionInfo.setStatus(new JTStatus(buf.readUnsignedInt()));

        positionInfo.setLatitude(buf.readInt() * 0.000001D);
        positionInfo.setLongitude(buf.readInt() *  0.000001D);
        positionInfo.setAltitude(buf.readUnsignedShort());
        positionInfo.setSpeed(buf.readUnsignedShort());
        positionInfo.setDirection(buf.readUnsignedShort());
        byte[] timeBytes = new byte[6];
        buf.readBytes(timeBytes);
        positionInfo.setTime(BCDUtil.transform(timeBytes));
        return positionInfo;
    }


    public String toSimpleString() {
        return "Brief location reporting information： " +
                " \n longitude：" + longitude +
                " \n Latitude：" + latitude +
                " \n elevation： " + altitude +
                " \n speed： " + speed +
                " \n direction： " + direction +
                " \n time： " + time +
                " \n";
    }

    @Override
    public String toString() {
        return "Location reporting information： " +
                " \n Alarm sign：" + alarmSign.toString() +
                " \n Status：" + status.toString() +
                " \n longitude：" + longitude +
                " \n Latitude：" + latitude +
                " \n elevation： " + altitude +
                " \n speed： " + speed +
                " \n direction： " + direction +
                " \n time： " + time +
                " \n";
    }
}
