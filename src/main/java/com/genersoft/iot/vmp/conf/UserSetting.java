package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.service.bean.AlarmType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration file user-settings Mapping configuration information
 */
@Component
@ConfigurationProperties(prefix = "user-settings", ignoreInvalidFields = true)
@Order(0)
@Data
public class UserSetting {

    /**
     * Whether to save location history(trajectory)
     */
    private Boolean savePositionHistory = Boolean.FALSE;

    /**
     * Whether to start automatic on-demand playback: When the request stream is an unpulled stream, on-demand playback will be started automatically. Requiredrtp.enable=true
     */
    private Boolean autoApplyPlay = Boolean.FALSE;

    /**
     * [Optional] Some devices need to expand SDP and need to turn on this setting. General equipment does not need to turn it on.
     */
    private Boolean seniorSdp = Boolean.FALSE;

    /**
     * on demand/Video playback waiting timeout, unit: milliseconds
     */
    private Integer playTimeout = 10000;

    /**
     * Timeout for obtaining device recording data, unit: milliseconds
     */
    private Integer recordInfoTimeout = 15000;

    /**
     * Superior on-demand waiting timeout, unit: milliseconds
     */
    private int platformPlayTimeout = 20000;

    /**
     * Whether to enable interface authentication
     */
    private Boolean interfaceAuthentication = Boolean.TRUE;

    /**
     * Interfaces with exceptions to interface authentication, that is, interfaces that do not perform interface authentication, should be written in as much detail as possible and should be used as little as possible./**，At least two levels of directories
     */
    private List<String> interfaceAuthenticationExcludes = new ArrayList<>();

    /**
     * Whether to record live push streaming
     */
    private Boolean recordPushLive = Boolean.TRUE;

    /**
     * Is the national standard recorded?
     */
    private Boolean recordSip = Boolean.TRUE;

    /**
     * Use push flow status as push flow channel status
     */
    private Boolean usePushingAsStatus = Boolean.FALSE;

    /**
     * Use the source request ip as streamIp, and enable it if and only if you only have the zlm node and it is together with wvp
     */
    private Boolean useSourceIpAsStreamIp = Boolean.FALSE;

    /**
     * Whether to use the device source IP as the reply IP, if not set, it will be false
     */
    private Boolean sipUseSourceIpAsRemoteAddress = Boolean.FALSE;

    /**
     * National standard on-demand streaming on demand, true: someone is watching the stream, no one is watching it, it is released, false: it is not automatically released after it is pulled up.
     */
    private Boolean streamOnDemand = Boolean.TRUE;

    /**
     * Push authentication, enabled by default
     */
    private Boolean pushAuthority = Boolean.TRUE;

    /**
     * Whether to automatically synchronize channels when the device goes online
     */
    private Boolean syncChannelOnDeviceOnline = Boolean.FALSE;

    /**
     * Whether to enable sip logs
     */
    private Boolean sipLog = Boolean.FALSE;

    /**
     * Whether to turn onmybatis-sqlLog
     */
    private Boolean sqlLog = Boolean.FALSE;

    /**
     * Message channel function-Whether to send a message to all superiors if the national standard ID is missing
     */
    private Boolean sendToPlatformsWhenIdLost = Boolean.FALSE;

    /**
     * Keep channel status, do not accept notify channel status changes, compatible with Hikvision platform to send error messages
     */
    private Boolean refuseChannelStatusChannelFormNotify = Boolean.FALSE;

    /**
     * Equipment/Send messages when channel status changes
     */
    private Boolean deviceStatusNotify = Boolean.TRUE;

    /**
     * The upper-level platform does not use the ssrc specified by the upper-level platform when on-demand. Use the customized ssrc. Please refer to the national standard document.-On-demand external domain device media stream SSRC processing method
     */
    private Boolean useCustomSsrcForParentInvite = Boolean.TRUE;

    /**
     * Multi-port mode uses random SSRC, port differentiation is performed, and SSRC is allowed to be repeated.
     */
    private Boolean ssrcRandom = Boolean.FALSE;

    /**
     * Open the interface documentation page. It is turned on by default. It is recommended to turn it off in production environments. It can also be turned off when encountering swagger-related vulnerabilities.
     */
    private Boolean docEnable = Boolean.TRUE;

    /**
     * Service ID, if not written, it will be000000
     */
    private String serverId = "000000";


    /**
     * National standard cascade voice broadcast streaming mode * UDP:udptransmission TCP-ACTIVE：tcpActive mode TCP-PASSIVE：tcppassive mode
     */
    private String broadcastForPlatform = "UDP";

    /**
     * Administrative division information files will be loaded into the system when the system starts.
     */
    private String civilCodeFile = "classpath:civilCode.csv";

    /**
     * Cross-domain configuration. If this is not configured, all cross-domain requests will be allowed. After configuration, only address requests for the configured page will be allowed. Multiple configurations can be configured.
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * Set the maximum length of the notify cache queue. Data exceeding this length will return 486 BUSY_HERE and the message will be discarded. Default100000
     */
    private int maxNotifyCountQueue = 100000;

    /**
     * How long does it take to retry registration after the GB cascade is offline?
     */
    private int registerAgainAfterTime = 60;

    /**
     * National standard renewal method, true means renewal, each registration is in the same session, false means re-registration, each time a new session is used
     */
    private boolean registerKeepIntDialog = false;

    /**
     * Online strategy for national standard equipment after it goes offline，
     * 0： The national standard is implemented. The device will not reply to the heartbeat after it is offline until the device is re-registered and online.，
     * 1（Default): For offline devices, when a heartbeat is received, the device is set online and the registration time is updated to the time of the last heartbeat. Prevent expiration time judgment exceptions
     */
    private int gbDeviceOnline = 1;

    /**
     *    Login timeout(minutes)，
     */
    private long loginTimeout = 60;

    /**
     * jwkFile path, if not specified, the file path in the resources directory will be used.jwk.json
     */
    private String jwkFile = null;

    /**
     * wvpIn cluster mode, if the registration fails with the superior wvp, another wvp will be automatically selected to continue registering with the superior.
     */
    private boolean autoRegisterPlatform = false;

    /**
     * Send the location of the push device on demand. By default, when sending mobile location subscriptions, if the location remains unchanged, it will not be sent. Set to false to continue sending at the national standard interval.
     */
    private boolean sendPositionOnDemand = true;

    /**
     * Some devices will send a large number of registrations in a short period of time, causing the protocol stack memory to overflow. Turning this on can prevent these devices from registering and avoid service crashes, but it will reduce system performance, as described below.
     * The default value is true。
     * Setting this to false will cause the Stack to Server Transaction Close the server socket after entering the TERMINATED state。
     * This allows the server to prevent client-initiated TCP-based denial-of-service attacks (i.e., initiating hundreds of client transactions）。
     * If true (the default), the stack will keep the socket open to maximize performance at the expense of thread and memory resources - Make yourself vulnerable to DOS attacks。
     */
    private boolean sipCacheServerConnections = true;

    /**
     * Disable the date header, disabling time adjustment in disguise
     */
    private boolean disableDateHeader = false;

    /**
     * When synchronizing business groups, the template of the group's national standard number is automatically generated. If not configured, it will be generated by referring to the current SIP domain information by default.
     */
    private String groupSyncDeviceTemplate;

    /**
     * When synchronizing groups with third parties, use aliases instead of group IDs. If this is not set to true, the group number must be passed. If set to true, a new number will be automatically generated for the alias group.
     */
    private boolean useAliasForGroupSync = false;

    /**
     * Device ID strict mode, when enabled, will reject the registration if the device ID does not meet the specifications when registering the device. It is enabled by default.
     */
    private boolean deviceIdStrict = true;

    /**
     * For national standard devices identified as devices, whether to enable location subscription by default
     */
    private boolean subscribeMobilePosition = false;

    /**
     * When processing alarm messages, the channel data will be cached. If it exceeds the limit, the low-heat messages will be discarded. The discarded channel will need to re-query the database next time. The default10000，
     * It is recommended to adjust it according to the actual situation. If it is too large, it may occupy more memory. If it is too small, it may increase the pressure of database query.
     */
    private long alarmCatchSize = 10000;

    /**
     * Whether to use the pull method to obtain snapshots. The default is false to avoid large-scale consumption of traffic. When enabled, the pull method will be used to obtain snapshots.
     */
    private boolean alarmSnapByStream = false;

    /**
     * Alarm subscription whitelist. After setting, only upper-level platforms in this list will receive alarm subscription messages. If not set by default, there will be no restriction.
     */
    private List<AlarmType> allowedAlarmType = new ArrayList<>();

    /**
     * The number of days that alarm records are retained. Alarm records that exceed this number of days will be automatically cleared in the early morning of every day. The default is 30 days. If set to 0, they will not be automatically cleared.
     */
    private int alarmKeepDays = 7;

    /**
     * The maximum number of entries in the device registration time list, default100
     */
    private int deviceRegisterTimeMaxCount = 100;

    /**
     * Device registration time list expiration time (hours), default 3, set to 0 to never expire
     */
    private int deviceRegisterTimeTtlHours = 3;

    /**
     * The maximum number of entries retained in the device heartbeat time list, default100
     */
    private int deviceKeepaliveTimeMaxCount = 100;

    /**
     * Device heartbeat time list expiration time (hours), default 1, set to 0 to never expire
     */
    private int deviceKeepaliveTimeTtlHours = 1;

}
