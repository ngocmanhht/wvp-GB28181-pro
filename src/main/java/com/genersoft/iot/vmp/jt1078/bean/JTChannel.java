package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.ObjectUtils;

/**
 * JT channel
 */
@Data
@Schema(description = "jt808channel")
@EqualsAndHashCode(callSuper = true)
public class JTChannel extends CommonGBChannel {

    @Schema(description = "Database auto-incrementID")
    private int id;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Device databaseID")
    private int terminalDbId;

    @Schema(description = "channelID")
    private Integer channelId;

    @Schema(description = "Does it contain audio")
    private boolean hasAudio;

    @Schema(description = "creation time")
    private String createTime;

    @Schema(description = "Update time")
    private String updateTime;

    @Schema(description = "flow information")
    private String stream;

    private Integer dataType = ChannelDataType.JT_1078;

    @Override
    public String toString() {
        return "JTChannel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", terminalDbId=" + terminalDbId +
                ", channelId=" + channelId +
                ", createTime='" + createTime + '\'' +
                ", updateTime='" + updateTime + '\'' +
                ", hasAudio='" + hasAudio + '\'' +
                '}';
    }

    public CommonGBChannel buildCommonGBChannel() {
        if (ObjectUtils.isEmpty(this.getGbDeviceId())) {
            return null;
        }
        if (ObjectUtils.isEmpty(this.getGbName())) {
            this.setGbName(this.getName());
        }
        return this;

    }
}
