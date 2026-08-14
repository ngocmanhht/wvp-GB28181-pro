package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTTextSign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Text message delivery parameters
 */
@Setter
@Getter
@Schema(description = "Manually confirm alarm message parameters")
public class TextMessageParam {

    @Schema(description = "Terminal mobile phone number")
    private String phoneNumber;
    @Schema(description = "logo")
    private JTTextSign sign;
    @Schema(description = "text type,1 = Notification ，2 = service")
    private int textType;
    @Schema(description = "Message content, up to 1024 bytes")
    private String content;

    @Override
    public String toString() {
        return "TextMessageParam{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", sign=" + sign +
                ", textType=" + textType +
                ", content='" + content + '\'' +
                '}';
    }
}
