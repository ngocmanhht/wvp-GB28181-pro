package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.vmanager.bean.AudioTalkResult;

/**
 * Resource capability access-Voice intercom
 */
public interface ISourceBroadcastService {

    AudioTalkResult startTalk(CommonGBChannel channel);

    void stopTalk(CommonGBChannel channel);

    AudioTalkResult startBroadcast(CommonGBChannel channel);

    void stopBroadcast(CommonGBChannel channel);
}
