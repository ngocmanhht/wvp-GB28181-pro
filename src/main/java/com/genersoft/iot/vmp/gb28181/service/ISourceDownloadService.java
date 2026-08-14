package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

/**
 * Resource capability access-Video download
 */
public interface ISourceDownloadService {

    void download(CommonGBChannel channel, Long startTime, Long stopTime, Integer downloadSpeed, ErrorCallback<StreamInfo> callback);

    void stopDownload(CommonGBChannel channel, String stream);
}
