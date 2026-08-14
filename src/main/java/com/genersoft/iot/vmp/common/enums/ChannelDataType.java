package com.genersoft.iot.vmp.common.enums;

/**
 * Supported channel data types
 */

public class ChannelDataType {

    public final static int GB28181 = 1;
    public final static int STREAM_PUSH = 2;
    public final static int STREAM_PROXY = 3;
    public final static int JT_1078 = 200;

    public final static String PLAY_SERVICE = "sourceChannelPlayService";
    public final static String PLAYBACK_SERVICE = "sourceChannelPlaybackService";
    public final static String DOWNLOAD_SERVICE = "sourceChannelDownloadService";
    public final static String PTZ_SERVICE = "sourceChannelPTZService";
    public final static String OTHER_SERVICE = "sourceChannelOtherService";
    public final static String BROADCAST_SERVICE = "sourceChannelBroadcastService";


    public static String getDateTypeDesc(Integer dataType) {
        if (dataType == null) {
            return "unknown";
        }
        return switch (dataType) {
            case ChannelDataType.GB28181 -> "National standard28181";
            case ChannelDataType.STREAM_PUSH -> "Push streaming equipment";
            case ChannelDataType.STREAM_PROXY -> "Streaming agent";
            case ChannelDataType.JT_1078 -> "Ministry standard equipment";
            default -> "unknown";
        };
    }


}
