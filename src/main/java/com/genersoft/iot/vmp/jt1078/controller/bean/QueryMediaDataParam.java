package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTQueryMediaDataCommand;
import com.genersoft.iot.vmp.jt1078.bean.JTShootingCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Store multimedia data parameters")
public class QueryMediaDataParam {

    @Schema(description = "Terminal mobile phone number")
    private String phoneNumber;

    @Schema(description = "Multimedia ID, valid when retrieving and uploading a single piece of stored multimedia data")
    private Long mediaId;

    @Schema(description = "Delete flag, valid when retrieving and uploading a single piece of stored multimedia data")
    private int delete;

    @Schema(description = "Store multimedia data parameters")
    private JTQueryMediaDataCommand queryMediaDataCommand;

    @Override
    public String toString() {
        return "QueryMediaDataParam{" +
                "Device mobile phone number='" + phoneNumber + '\'' +
                ", mediaId=" + mediaId +
                ", queryMediaDataCommand=" + queryMediaDataCommand +
                '}';
    }
}
