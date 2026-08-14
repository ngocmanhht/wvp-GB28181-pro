package com.genersoft.iot.vmp.common;

/**
 * @description: Define constants
 * @author: swwheihei
 * @date:   2019May 30, 2019, afternoon3:04:04
 *
 */
public class VideoManagerConstants {

	public static final String WVP_SERVER_PREFIX = "VMP_SIGNALLING_SERVER_INFO_";

	public static final String WVP_SERVER_LIST = "VMP_SERVER_LIST";

	public static final String WVP_SERVER_STREAM_PREFIX = "VMP_SIGNALLING_STREAM_";

	public static final String MEDIA_SERVER_PREFIX = "VMP_MEDIA_SERVER_INFO:";

	public static final String ONLINE_MEDIA_SERVERS_PREFIX = "VMP_ONLINE_MEDIA_SERVERS:";

	public static final String DEVICE_PREFIX = "VMP_DEVICE_INFO";
	public static final String DEVICE_KEEPALIVE_PREFIX = "VMP_DEVICE_KEEPALIVE:";
	public static final String DEVICE_REGISTER_PREFIX = "VMP_DEVICE_REGISTER:";

	public static final String INVITE_PREFIX = "VMP_GB_INVITE_INFO";

	public static final String SEND_RTP_PORT = "VM_SEND_RTP_PORT:";
	public static final String SEND_RTP_INFO_CALLID = "VMP_SEND_RTP_INFO:CALL_ID:";
	public static final String SEND_RTP_INFO_STREAM = "VMP_SEND_RTP_INFO:STREAM:";
	public static final String SEND_RTP_INFO_CHANNEL = "VMP_SEND_RTP_INFO:CHANNEL:";

	public static final String SIP_INVITE_SESSION = "VMP_SIP_INVITE_SESSION_INFO:";
	public static final String SIP_INVITE_SESSION_CALL_ID = SIP_INVITE_SESSION + "CALL_ID:";
	public static final String SIP_INVITE_SESSION_STREAM = SIP_INVITE_SESSION + "STREAM:";

	public static final String MEDIA_STREAM_AUTHORITY = "VMP_MEDIA_STREAM_AUTHORITY";

	public static final String SIP_CSEQ_PREFIX = "VMP_SIP_CSEQ_";

	public static final String SIP_SUBSCRIBE_PREFIX = "VMP_SIP_SUBSCRIBE_";

	public static final String SYSTEM_INFO_CPU_PREFIX = "VMP_SYSTEM_INFO_CPU_";

	public static final String SYSTEM_INFO_MEM_PREFIX = "VMP_SYSTEM_INFO_MEM_";

	public static final String SYSTEM_INFO_NET_PREFIX = "VMP_SYSTEM_INFO_NET_";

	public static final String SYSTEM_INFO_DISK_PREFIX = "VMP_SYSTEM_INFO_DISK_";
	public static final String BROADCAST_WAITE_INVITE = "task_broadcast_waite_invite_";

	public static final String PUSH_STREAM_LIST = "VMP_PUSH_STREAM_LIST_";
	public static final String WAITE_SEND_PUSH_STREAM = "VMP_WAITE_SEND_PUSH_STREAM:";
	public static final String START_SEND_PUSH_STREAM = "VMP_START_SEND_PUSH_STREAM:";
	public static final String SSE_TASK_KEY = "SSE_TASK_";
	public static final String DRAW_THIN_PROCESS_PREFIX = "VMP_DRAW_THIN_PROCESS_";
	public static final String RTP_AUTHENTICATE = "VMP_RTP_AUTHENTICATE";




	//************************** redis news*********************************

	/**
	 * Stream change notifications
	 */
	public static final String WVP_MSG_STREAM_CHANGE_PREFIX = "WVP_MSG_STREAM_CHANGE_";

	/**
	 * Receive notifications of GPS changes for streaming devices
	 */
	public static final String VM_MSG_GPS = "VM_MSG_GPS";

	/**
	 * Receive notifications of GPS changes for streaming devices
	 */
	public static final String VM_MSG_PUSH_STREAM_STATUS_CHANGE = "VM_MSG_PUSH_STREAM_STATUS_CHANGE";
	/**
	 * Receive notifications of updates and changes to the streaming device list
	 */
	public static final String VM_MSG_PUSH_STREAM_LIST_CHANGE = "VM_MSG_PUSH_STREAM_LIST_CHANGE";

    /**
     * Synchronize tripartite organizational structure reply
     */
    public static final String VM_MSG_GROUP_LIST_RESPONSE = "VM_MSG_GROUP_LIST_RESPONSE";

    /**
     * Synchronize tripartite organizational structure reply
     */
    public static final String VM_MSG_GROUP_LIST_CHANGE = "VM_MSG_GROUP_LIST_CHANGE";

	/**
	 * redis Message notification device is pushed to the platform
	 */
	public static final String VM_MSG_STREAM_PUSH_REQUESTED = "VM_MSG_STREAM_PUSH_REQUESTED";

	/**
	 * redis The message notifies the superior platform to start watching the stream
	 */
	public static final String VM_MSG_STREAM_START_PLAY_NOTIFY = "VM_MSG_STREAM_START_PLAY_NOTIFY";

	/**
	 * redis The message notifies the superior platform to stop watching the stream.
	 */
	public static final String VM_MSG_STREAM_STOP_PLAY_NOTIFY = "VM_MSG_STREAM_STOP_PLAY_NOTIFY";

	/**
	 * redis Message reception closes a push stream
	 */
	public static final String VM_MSG_STREAM_PUSH_CLOSE_REQUESTED = "VM_MSG_STREAM_PUSH_CLOSE_REQUESTED";


	/**
	 * redis The message notification platform notifies the device of the streaming results
	 */
	public static final String VM_MSG_STREAM_PUSH_RESPONSE = "VM_MSG_STREAM_PUSH_RESPONSE";

	/**
	 * redis Notify the platform to shut down push streaming
	 */
	public static final String VM_MSG_STREAM_PUSH_CLOSE = "VM_MSG_STREAM_PUSH_CLOSE";

	/**
	 * redis Message requests for all online channels
	 */
	public static final String VM_MSG_GET_ALL_ONLINE_REQUESTED = "VM_MSG_GET_ALL_ONLINE_REQUESTED";

	/**
	 * Notification of alarm subscription (notification to redis upon receipt of alarm）
	 */
	public static final String VM_MSG_SUBSCRIBE_ALARM = "alarm";


	/**
	 * Sending of alarm notifications (receiving notifications from redis and forwarding them to other platforms）
	 */
	public static final String VM_MSG_SUBSCRIBE_ALARM_RECEIVE= "alarm_receive";

	/**
	 * Notifications for device status subscriptions
	 */
	public static final String VM_MSG_SUBSCRIBE_DEVICE_STATUS = "device";




	//**************************    third party  ****************************************

	public static final String WVP_STREAM_GB_ID_PREFIX = "memberNo_";
	public static final String WVP_STREAM_GPS_MSG_PREFIX = "WVP_STREAM_GPS_MSG_";
	public static final String WVP_OTHER_SEND_RTP_INFO = "VMP_OTHER_SEND_RTP_INFO_";
	public static final String WVP_OTHER_SEND_PS_INFO = "VMP_OTHER_SEND_PS_INFO_";
	public static final String WVP_OTHER_RECEIVE_RTP_INFO = "VMP_OTHER_RECEIVE_RTP_INFO_";
	public static final String WVP_OTHER_RECEIVE_PS_INFO = "VMP_OTHER_RECEIVE_PS_INFO_";

	/**
	 * Redis Const
	 * Device recording information result prefix
	 */
	public static final String REDIS_RECORD_INFO_RES_PRE = "GB_RECORD_INFO_RES_";
	/**
	 * Redis Const
	 * Device recording information result prefix
	 */
	public static final String REDIS_RECORD_INFO_RES_COUNT_PRE = "GB_RECORD_INFO_RES_COUNT:";

	//**************************    1078  ****************************************


	public static final String INVITE_INFO_1078_POSITION = "INVITE_INFO_1078_POSITION:";
	public static final String INVITE_INFO_1078_PLAY = "INVITE_INFO_1078_PLAY:";
	public static final String INVITE_INFO_1078_PLAYBACK = "INVITE_INFO_1078_PLAYBACK:";
	public static final String INVITE_INFO_1078_TALK = "INVITE_INFO_1078_TALK:";


	public static final String RECORD_LIST_1078 = "RECORD_LIST_1078:";

}
