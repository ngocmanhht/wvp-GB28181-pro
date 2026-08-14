package com.genersoft.iot.vmp.gb28181.bean;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * National standard equipment/platform
 * @author lin
 */
@Data
@Schema(description = "National standard equipment/platform")
public class Device {

	@Schema(description = "Database auto-incrementID")
	private int id;

	/**
	 * Equipment national standard number
	 */
	@Schema(description = "Equipment national standard number")
	private String deviceId;

	/**
	 * Device name
	 */
	@Schema(description = "Name")
	private String name;

	/**
	 * Manufacturer
	 */
	@Schema(description = "Manufacturer")
	private String manufacturer;

	/**
	 * Model
	 */
	@Schema(description = "Model")
	private String model;

	/**
	 * Firmware version
	 */
	@Schema(description = "Firmware version")
	private String firmware;

	/**
	 * transport protocol
	 * UDP/TCP
	 */
	@Schema(description = "transport protocol（UDP/TCP）")
	private String transport;

	/**
	 * Data streaming mode
	 * UDP:udptransmission
	 * TCP-ACTIVE：tcpActive mode
	 * TCP-PASSIVE：tcppassive mode
	 */
	@Schema(description = "Data streaming mode")
	private String streamMode;

	/**
	 * wanaddress_ip
	 */
	@Schema(description = "IP")
	private String  ip;

	/**
	 * wanaddress_port
	 */
	@Schema(description = "port")
	private int port;

	/**
	 * wanaddress
	 */
	@Schema(description = "wanaddress")
	private String  hostAddress;

	/**
	 * online
	 */
	@Schema(description = "Whether it is online, true means online, false means offline")
	private boolean onLine;


	/**
	 * Registration time
	 */
	@Schema(description = "Registration timestamp")
	private Long registerTimeStamp;


	/**
	 * heartbeat time
	 */
	@Schema(description = "heartbeat time")
	private Long keepaliveTimeStamp;


	/**
	 * heartbeat interval
	 */
	@Schema(description = "heartbeat interval")
	private Integer heartBeatInterval;


	/**
	 * Number of heartbeat timeouts
	 */
	@Schema(description = "Number of heartbeat timeouts")
	private Integer heartBeatCount;


	/**
	 * Positioning function support
	 */
	@Schema(description = "Positioning feature support. value:0-Not supported;1-Support GPS positioning;2-Support Beidou positioning(Optional, the default value is0")
	private Integer positionCapability;

	/**
	 * Number of channels
	 */
	@Schema(description = "Number of channels")
	private int channelCount;

	/**
	 * Registration validity period
	 */
	@Schema(description = "Registration validity period")
	private int expires;

	/**
	 * creation time
	 */
	@Schema(description = "creation time")
	private String createTime;

	/**
	 * Update time
	 */
	@Schema(description = "Update time")
	private String updateTime;

	/**
	 * Media ID used by the device, default isnull
	 */
	@Schema(description = "Media ID used by the device, default isnull")
	private String mediaServerId;

	/**
	 * character set, support UTF-8 with GB2312
	 */
	@Schema(description = "symbol set, support UTF-8 with GB2312")
	private String charset ;

	/**
	 * Directory subscription period, 0 means no subscription
	 */
	@Schema(description = "Directory subscription period, o means no subscription")
	private int subscribeCycleForCatalog;

	/**
	 * Mobile device location subscription period, 0 means no subscription
	 */
	@Schema(description = "Mobile device location subscription period, 0 means no subscription")
	private int subscribeCycleForMobilePosition;

	/**
	 * Mobile device location information reporting time interval, unit: seconds, default value5
	 */
	@Schema(description = "Mobile device location information reporting time interval, unit: seconds, default value5")
	private int mobilePositionSubmissionInterval = 5;

	/**
	 * Alarm subscription period, 0 means no subscription
	 */
	@Schema(description = "Alarm heartbeat time subscription period, 0 means no subscription")
	private int subscribeCycleForAlarm;

	/**
	 * Whether to enable ssrc verification. It is turned off by default. Turning it on can prevent streaming.
	 */
	@Schema(description = "Whether to enable ssrc verification. It is turned off by default. Turning it on can prevent streaming.")
	private boolean ssrcCheck = false;

	/**
	 * Geographic coordinate system, currently supports WGS84, GCJ02, this field is reserved and is currently unavailable
	 */
	@Schema(description = "Geographic coordinate system, currently supported WGS84,GCJ02")
	private String geoCoordSys;

	@Schema(description = "Password")
	private String password;

	@Schema(description = "collect flowIP")
	private String sdpIp;

	@Schema(description = "SIPInteractive IP (device access platformIP）")
	private String localIp;

	@Schema(description = "Whether to serve as a message channel")
	private boolean asMessageChannel;

	@Schema(description = "Device registration transaction information")
	private SipTransactionInfo sipTransactionInfo;

	@Schema(description = "Control the voice intercom process and release the stream after receiving ACK")
	private boolean broadcastPushAfterAck;

	@Schema(description = "ServicesId")
	private String serverId;

    public boolean checkWgs84() {
        return geoCoordSys.equalsIgnoreCase("WGS84");
    }

	public Integer getHeartBeatCount() {
		if (heartBeatCount == null) {
			return 3;
		}
		return heartBeatCount;
	}

	public Integer getHeartBeatInterval() {
		if (heartBeatCount == null) {
			return 60;
		}
		return heartBeatInterval;
	}
}
