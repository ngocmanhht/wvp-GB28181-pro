package com.genersoft.iot.vmp.streamProxy.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageInfo;

import java.util.Map;

public interface IStreamProxyService {

    /**
     * Page query
     * @param page
     * @param count
     * @return
     */
    PageInfo<StreamProxy> getAll(Integer page, Integer count, String query, Boolean pulling,String mediaServerId);

    /**
     * Delete video proxy
     * @param app
     * @param stream
     */
    void delteByAppAndStream(String app, String stream);

    /**
     * Enable video proxy
     * @param app
     * @param stream
     * @return
     */
    void startByAppAndStream(String app, String stream, ErrorCallback<StreamInfo> callback);

    /**
     * Disable video proxy
     * @param app
     * @param stream
     * @return
     */
    void stopByAppAndStream(String app, String stream);

    /**
     * Get ffmpeg.cmd template
     *
     * @return
     */
    Map<String, String> getFFmpegCMDs(MediaServer mediaServerItem);

    /**
     * Get based on app and streamstreamProxy
     * @return
     */
    StreamProxy getStreamProxyByAppAndStream(String app, String streamId);


    /**
     * New node added
     * @param mediaServer
     * @return
     */
    void zlmServerOnline(MediaServer mediaServer);

    /**
     * Node offline
     * @param mediaServer
     * @return
     */
    void zlmServerOffline(MediaServer mediaServer);

    /**
     * Update agent flow
     */
    boolean update(StreamProxy streamProxyItem);

    /**
     * Get statistics
     * @return
     */
    ResourceBaseInfo getOverview();

    void add(StreamProxy streamProxy);

    StreamProxy getStreamProxy(int id);

    void delete(int id);

}
