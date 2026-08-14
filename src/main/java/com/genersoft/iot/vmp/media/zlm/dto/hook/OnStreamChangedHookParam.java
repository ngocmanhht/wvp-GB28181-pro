package com.genersoft.iot.vmp.media.zlm.dto.hook;

import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * @author lin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OnStreamChangedHookParam extends HookParam{

    /**
     * Register/Log out
     */
    private boolean regist;

    /**
     * Application name
     */
    private String app;

    /**
     * flowid
     */
    private String stream;

    /**
     * Push authenticationId
     */
    private String callId;

    /**
     * Total number of viewers, includinghls/rtsp/rtmp/http-flv/ws-flv
     */
    private int totalReaderCount;

    /**
     * Agreement includeshls/rtsp/rtmp/http-flv/ws-flv
     */
    private String schema;


    /**
     * Generate source type，
     * unknown = 0,
     * rtmp_push=1,
     * rtsp_push=2,
     * rtp_push=3,
     * pull=4,
     * ffmpeg_pull=5,
     * mp4_vod=6,
     * device_chn=7
     */
    private int originType;

    /**
     * Client and server network information, possibly null
     */
    private OriginSock originSock;

    /**
     * Produces a string description of the source type
     */
    private String originTypeStr;

    /**
     * originatingurl
     */
    private String originUrl;

    /**
     * serverid
     */
    private String severId;

    /**
     * GMT unixSystem timestamp in seconds
     */
    private Long createStamp;

    /**
     * Survival time in seconds
     */
    private Long aliveSecond;

    /**
     * Data generation speed, unitbyte/s
     */
    private Long bytesSpeed;

    /**
     * Audio and video tracks
     */
    private List<MediaTrack> tracks;

    /**
     * Audio and video tracks
     */
    private String vhost;

    /**
     * extra parameter string
     */
    private String params;

    /**
     * additional parameters
     */
    private Map<String, String> paramMap;

    /**
     * Whether it is docker deployment, docker deployment will not automatically update the port used by zlm, and you need to modify it manually.
     */
    private boolean docker;

    @Data
    public static class MediaTrack {
        /**
         * Number of audio channels
         */
        private int channels;

        /**
         *  H264 = 0, H265 = 1, AAC = 2, G711A = 3, G711U = 4
         */
        private int codec_id;

        /**
         * Encoding type name CodecAAC CodecH264
         */
        private String codec_id_name;

        /**
         * Video = 0, Audio = 1
         */
        private int codec_type;

        /**
         * Is the track ready?
         */
        private boolean ready;

        /**
         * audio sample bits
         */
        private int sample_bit;

        /**
         * Audio sample rate
         */
        private int sample_rate;

        /**
         * videofps
         */
        private float fps;

        /**
         * video high
         */
        private int height;

        /**
         * video width
         */
        private int width;

        /**
         * Frames
         */
        private int frames;

        /**
         * Number of key frames
         */
        private int key_frames;

        /**
         * GOPsize
         */
        private int gop_size;

        /**
         * GOPInterval duration(ms)
         */
        private int gop_interval_ms;

        /**
         * frame loss rate
         */
        private float loss;
    }

    @Data
    public static class OriginSock{
        private String identifier;
        private String local_ip;
        private int local_port;
        private String peer_ip;
        private int peer_port;

    }

    private StreamContent streamInfo;

    @Override
    public String toString() {
        return "OnStreamChangedHookParam{" +
                "regist=" + regist +
                ", app='" + app + '\'' +
                ", stream='" + stream + '\'' +
                ", severId='" + severId + '\'' +
                '}';
    }
}
