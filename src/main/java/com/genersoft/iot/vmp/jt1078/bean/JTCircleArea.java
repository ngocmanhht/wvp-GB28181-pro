package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.Charset;
import java.util.Date;

@Setter
@Getter
@Schema(description = "circular area")
public class JTCircleArea implements JTAreaOrRoute{

    @Schema(description = "area ID")
    private long id;

    @Schema(description = "")
    private JTAreaAttribute attribute;

    @Schema(description = "Center point latitude")
    private Double latitude;

    @Schema(description = "Center point longitude")
    private Double longitude;

    @Schema(description = "Radius in meters(m)")
    private long radius;

    @Schema(description = "start time, yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "end time, yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @Schema(description = "Maximum speed in kilometers per hour(km/h)")
    private int maxSpeed;

    @Schema(description = "Overspeed duration, in seconds(s)")
    private int overSpeedDuration;

    @Schema(description = "Maximum speed at night in kilometers per hour(km/h)")
    private int nighttimeMaxSpeed;

    @Schema(description = "The name of the area")
    private String name;

    public ByteBuf encode(){
         ByteBuf byteBuf = Unpooled.buffer();
         byteBuf.writeInt((int) (id & 0xffffffffL));
         byteBuf.writeBytes(attribute.encode());
         byteBuf.writeInt((int) (Math.round((latitude * 1000000)) & 0xffffffffL));
         byteBuf.writeInt((int) (Math.round((longitude * 1000000)) & 0xffffffffL));
         byteBuf.writeInt((int) (radius & 0xffffffffL));
         byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime)));
         byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime)));
         byteBuf.writeShort((short)(maxSpeed & 0xffff));
         byteBuf.writeByte(overSpeedDuration);
         byteBuf.writeShort((short)(nighttimeMaxSpeed & 0xffff));
         byteBuf.writeShort((short)(name.getBytes(Charset.forName("GBK")).length & 0xffff));
         byteBuf.writeCharSequence(name, Charset.forName("GBK"));
         return byteBuf;
     }

    public static JTCircleArea decode(ByteBuf buf) {

        JTCircleArea area = new JTCircleArea();
        area.setId(buf.readUnsignedInt());
        int attributeInt = buf.readUnsignedShort();
        JTAreaAttribute areaAttribute = JTAreaAttribute.decode(attributeInt);
        area.setAttribute(areaAttribute);

        area.setLatitude(buf.readUnsignedInt()/1000000D);
        area.setLongitude(buf.readUnsignedInt()/1000000D);
        area.setRadius(buf.readUnsignedInt());
        byte[] startTimeBytes = new byte[6];
        buf.readBytes(startTimeBytes);
        area.setStartTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(startTimeBytes)));
        byte[] endTimeBytes = new byte[6];
        buf.readBytes(endTimeBytes);
        area.setEndTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(endTimeBytes)));
        area.setMaxSpeed(buf.readUnsignedShort());
        area.setOverSpeedDuration(buf.readUnsignedByte());
        area.setNighttimeMaxSpeed(buf.readUnsignedShort());
        int nameLength = buf.readUnsignedShort();
        area.setName(buf.readCharSequence(nameLength, Charset.forName("GBK")).toString().trim());
        return area;
    }

}
