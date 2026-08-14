package com.genersoft.iot.vmp.streamPush.bean;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.ObjectUtils;


@Data
@Schema(description = "push information")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class StreamPush extends CommonGBChannel implements Comparable<StreamPush>{

    /**
     * id
     */
    @Schema(description = "id")
    private Integer id;

    /**
     * Application name
     */
    @Schema(description = "Application name")
    private String app;

    /**
     * flowid
     */
    @Schema(description = "flowid")
    private String stream;

    /**
     * Streaming media usedID
     */
    @Schema(description = "Streaming media usedID")
    private String mediaServerId;

    /**
     * Services usedID
     */
    @Schema(description = "Services usedID")
    private String serverId;

    /**
     * Push time
     */
    @Schema(description = "Push time")
    private String pushTime;

    /**
     * Update time
     */
    @Schema(description = "Update time")
    private String updateTime;

    /**
     * creation time
     */
    @Schema(description = "creation time")
    private String createTime;

    /**
     * Whether streaming is being pushed
     */
    @Schema(description = "Whether streaming is being pushed")
    private boolean pushing;

    /**
     * Pull up offline push flow
     */
    @Schema(description = "Pull up offline push flow")
    private boolean startOfflinePush;

    /**
     * speed, unit:km/h (Optional)
     */
    @Schema(description = "GPSspeed")
    private Double gpsSpeed;

    /**
     * Direction, the value is the clockwise angle between the current camera direction and true north, the value range is 0°~360°, unit:(°)(Optional)
     */
    @Schema(description = "GPSdirection")
    private Double gpsDirection;

    /**
     * Altitude, unit:m(Optional)
     */
    @Schema(description = "GPSaltitude")
    private Double gpsAltitude;

    /**
     * GPSupdate time
     */
    @Schema(description = "GPSupdate time")
    private String gpsTime;

    private String uniqueKey;

    private Integer dataType = ChannelDataType.STREAM_PUSH;


    @Override
    public int compareTo(@NotNull StreamPush streamPushItem) {
        return Long.valueOf(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(this.createTime)
                - DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(streamPushItem.getCreateTime())).intValue();
    }

    public static StreamPush getInstance(StreamInfo streamInfo) {
        StreamPush streamPush = new StreamPush();
        streamPush.setApp(streamInfo.getApp());
        if (streamInfo.getMediaServer() != null) {
            streamPush.setMediaServerId(streamInfo.getMediaServer().getId());
        }

        streamPush.setStream(streamInfo.getStream());
        streamPush.setCreateTime(DateUtil.getNow());
        streamPush.setServerId(streamInfo.getServerId());
        return streamPush;

    }

    public static StreamPush getInstance(MediaArrivalEvent event, String serverId){
        StreamPush streamPushItem = new StreamPush();
        streamPushItem.setApp(event.getApp());
        streamPushItem.setMediaServerId(event.getMediaServer().getId());
        streamPushItem.setStream(event.getStream());
        streamPushItem.setCreateTime(DateUtil.getNow());
        streamPushItem.setServerId(serverId);
        return streamPushItem;
    }

    public CommonGBChannel buildCommonGBChannel() {
        if (ObjectUtils.isEmpty(this.getGbDeviceId())) {
            return null;
        }
        if (ObjectUtils.isEmpty(this.getGbName())) {
            this.setGbName( app+ "-" +stream);
        }
        this.setDataType(ChannelDataType.STREAM_PUSH);
        this.setDataDeviceId(this.getId());
        return this;
    }


}

