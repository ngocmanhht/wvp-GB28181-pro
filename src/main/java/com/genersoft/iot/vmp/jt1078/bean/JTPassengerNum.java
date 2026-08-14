package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.config.JTDeviceSubConfig;
import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * Terminal uploads passenger traffic
 */
@Setter
@Getter
public class JTPassengerNum implements JTDeviceSubConfig {

    /**
     * start time, YY-MM-DD-HH-MM-SS( GMT + 8 Time, all subsequent times in this standard are in this time zone.)
     */
    private String startTime;

    /**
     * end time, YY-MM-DD-HH-MM-SS( GMT + 8 Time, all subsequent times in this standard are in this time zone.)
     */
    private String endTime;

    /**
     * Number of people on board
     */
    private int getIn;

    /**
     * Number of people getting off the bus
     */
    private int getOut;

    @Override
    public ByteBuf encode() {
        return null;
    }

    public static JTPassengerNum decode(ByteBuf buf) {
        JTPassengerNum jtPassengerNum = new JTPassengerNum();
        byte[] bytes = new byte[6];
        buf.readBytes(bytes);
        jtPassengerNum.setStartTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(bytes)));
        buf.readBytes(bytes);
        jtPassengerNum.setEndTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(bytes)));
        jtPassengerNum.setGetIn(buf.readUnsignedShort());
        jtPassengerNum.setGetOut(buf.readUnsignedShort());
        return jtPassengerNum;
    }

    @Override
    public String toString() {
        return "Terminal uploads passenger traffic：" +
                " time： " + startTime + " Arrive "  + endTime +
                ", Get on the bus：" + getIn +
                ", Get off the car：" + getOut
                ;
    }
}
