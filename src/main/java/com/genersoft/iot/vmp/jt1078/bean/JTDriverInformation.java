package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

@Data
@Slf4j
@Schema(description = "driver identification information")
public class JTDriverInformation {

    @Schema(description = "0x01:Business qualification certificate IC card insertion( Driver goes to work)；0x02:Pull out the professional qualification certificate IC card(Driver off work)")
    private int status;

    @Schema(description = "plug-in card/Card removal time, the following fields are only valid and filled when the status is 0x01")
    private String time;

    @Schema(description = "ICCard reading result:" +
            "0x00:ICCard reading successful；" +
            "0x01:The card reading failed because the card key authentication failed.；" +
            "0x02:Card reading failed because the card has been locked；" +
            "0x03:Card reading failed because the card was pulled out；" +
            "0x04:Card reading failed due to data verification error。" +
            "The following fields are valid only when the IC card reading result is equal to 0x00")
    private Integer result;

    @Schema(description = "driver name")
    private String name;

    @Schema(description = "Professional qualification certificate code")
    private String certificateCode;

    @Schema(description = "Name of issuing authority")
    private String certificateIssuanceMechanismName;

    @Schema(description = "Certificate validity period")
    private String expire;

    @Schema(description = "Driver ID number")
    private String driverIdNumber;

    public static JTDriverInformation decode(ByteBuf buf, boolean is2019) {
        JTDriverInformation jtDriverInformation = new JTDriverInformation();
        jtDriverInformation.setStatus(buf.readUnsignedByte());
        byte[] bytes = new byte[6];
        buf.readBytes(bytes);
        String timeStr = BCDUtil.transform(bytes);
        try {
            jtDriverInformation.setTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(timeStr));
        }catch (Exception e) {
            log.error("[JT-driver identification information] Unable to format time when decoding： {}", timeStr);
        }

        if (jtDriverInformation.getStatus() == 1) {
            int result = (int)buf.readUnsignedByte();
            jtDriverInformation.setResult(result);
            if (result == 0) {
                // ICCard reading successful
                int nameLength = buf.readUnsignedByte();
                jtDriverInformation.setName(buf.readCharSequence(nameLength, Charset.forName("GBK")).toString().trim());
                jtDriverInformation.setCertificateCode(buf.readCharSequence(20, Charset.forName("GBK")).toString().trim());
                int certificateIssuanceMechanismNameLength = buf.readUnsignedByte();
                jtDriverInformation.setCertificateIssuanceMechanismName(buf.readCharSequence(
                        certificateIssuanceMechanismNameLength, Charset.forName("GBK")).toString().trim());
                byte[] bytesForExpire = new byte[4];
                buf.readBytes(bytesForExpire);
                String bytesForExpireStr = BCDUtil.transform(bytesForExpire);
                try {
                    jtDriverInformation.setExpire(DateUtil.jt1078dateToyyyy_MM_dd(bytesForExpireStr));
                }catch (Exception e) {
                    log.error("[JT-driver identification information] Unable to format time when decoding： {}", bytesForExpireStr);
                }
                if (is2019) {
                    jtDriverInformation.setDriverIdNumber(buf.readCharSequence(20, Charset.forName("GBK")).toString().trim());
                }
            }
        }
        return jtDriverInformation;
    }

    @Override
    public String toString() {
        return "JTDriverInformation{" +
                "status=" + status +
                ", time='" + time + '\'' +
                ", result=" + result +
                ", name='" + name + '\'' +
                ", certificateCode='" + certificateCode + '\'' +
                ", certificateIssuanceMechanismName='" + certificateIssuanceMechanismName + '\'' +
                ", expire='" + expire + '\'' +
                ", driverIdNumber='" + driverIdNumber + '\'' +
                '}';
    }
}
