package com.genersoft.iot.vmp.service.bean;

import lombok.Data;

import java.util.List;

/**
 * Receive redis notification to modify push channel status
 * @author lin
 */
@Data
public class PushStreamStatusChangeFromRedisDto {

    private boolean setAllOffline;

    private List<StreamPushItemFromRedis> onlineStreams;

    private List<StreamPushItemFromRedis> offlineStreams;
}
