package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

/**
 * Resource capability access-real time video
 */
public interface ISourcePlayService {

    void play(CommonGBChannel channel, Platform platform, Boolean record, ErrorCallback<StreamInfo> callback);

    void stopPlay(CommonGBChannel channel);

    void getSnap(CommonGBChannel channel, ErrorCallback<byte[]> callback);

}
