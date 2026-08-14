package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

import java.util.List;

/**
 * Record the status of national standard on-demand videos, including real-time preview, download, and video playback
 */
public interface IInviteStreamService {

    /**
     * Update on-demand status information
     */
    void updateInviteInfo(InviteInfo inviteInfo);

    void updateInviteInfo(InviteInfo inviteInfo, Long time);

    InviteInfo updateInviteInfoForStream(InviteInfo inviteInfo, String stream);

    /**
     * Get on-demand status information
     */
    InviteInfo getInviteInfo(InviteSessionType type, Integer channelId, String stream);

    /**
     * Remove on-demand status information
     */
    void removeInviteInfo(InviteSessionType type, Integer channelId, String stream);
    /**
     * Remove on-demand status information
     */
    void removeInviteInfo(InviteInfo inviteInfo);
    /**
     * Remove on-demand status information
     */
    void removeInviteInfoByDeviceAndChannel(InviteSessionType inviteSessionType, Integer channelId);

    List<InviteInfo> getAllInviteInfo();

    /**
     * Get on-demand status information
     */
    InviteInfo getInviteInfoByDeviceAndChannel(InviteSessionType type, Integer channelId);

    /**
     * Get on-demand status information
     */
    InviteInfo getInviteInfoByStream(InviteSessionType type, String stream);


    /**
     * Add an invite callback
     */
    void once(InviteSessionType type, Integer channelId, String stream,  ErrorCallback<StreamInfo> callback);

    /**
     * Call an invite callback
     */
    void call(InviteSessionType type,  Integer channelId, String stream,  int code, String msg, StreamInfo data);

    /**
     * Clear all invite information of a device
     */
    void clearInviteInfo(String deviceId);

    /**
     * Count the number of national standard collections under the same zlm
     */
    int getStreamInfoCount(String mediaServerId);


    /**
     * Get stream information under MediaServer
     */
    InviteInfo getInviteInfoBySSRC(String ssrc);

    /**
     * updatessrc
     */
    InviteInfo updateInviteInfoForSSRC(InviteInfo inviteInfo, String ssrcInResponse);
}
