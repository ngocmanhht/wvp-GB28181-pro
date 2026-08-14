package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarmNotify;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import gov.nist.javax.sip.message.SIPRequest;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * @description:Device capability interface, used to define the control and query capabilities of the device
 * @author: swwheihei
 * @date:   2020May 3rd, afternoon9:16:34
 */
public interface ISIPCommander {

	/**
	 * PTZ control, supports direction and zoom control
	 *
	 * @param device  control equipment
	 * @param channelId  Preview channel
	 * @param leftRight  Camera moves left and right 0: Stop 1: Move left 2: Move right
     * @param upDown     Lens moves up and down 0: Stop 1: Move up 2: Move down
     * @param inOut      Lens zoom in and out 0: Stop 1: Zoom out 2: Zoom in
     * @param moveSpeed  Lens movement speed
     * @param zoomSpeed  Lens zoom speed
	 */
	void ptzCmd(Device device,String channelId,int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Front-end control, including PTZ command, FI command, preset position command, cruise command, scan command and auxiliary switch command
	 *
	 * @param device  		control equipment
	 * @param channelId		Preview channel
	 * @param cmdCode		Script code
     * @param parameter1	data1
     * @param parameter2	data2
     * @param combineCode2	Combination code2
	 */
	void frontEndCmd(Device device, String channelId, int cmdCode, int parameter1, int parameter2, int combineCode2) throws SipException, InvalidArgumentException, ParseException;

	/**
	 * Front-end control instructions (used to forward superior instructions）
	 * @param device		control equipment
	 * @param channelId		Preview channel
	 * @param cmdString		Front-end control command string
	 */
	void fronEndCmd(Device device, String channelId, String cmdString, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Request a preview of a video stream
	 * @param device  video equipment
	 * @param channel  Preview channel
	 */
	void playStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, Device device, DeviceChannel channel, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Request playback of a video stream
	 *
	 * @param device  video equipment
	 * @param channel  Preview channel
	 * @param startTime Start time, format requirements：yyyy-MM-dd HH:mm:ss
	 * @param endTime End time, format requirements：yyyy-MM-dd HH:mm:ss
	 */
	void playbackStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInf, Device device, DeviceChannel channel, String startTime, String endTime, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Request historical media downloads
	 *
	 * @param device  video equipment
	 * @param channel  Preview channel
	 * @param startTime Start time, format requirements：yyyy-MM-dd HH:mm:ss
	 * @param endTime End time, format requirements：yyyy-MM-dd HH:mm:ss
	 * @param downloadSpeed Download speed parameters
	 */
	void downloadStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, Device device, DeviceChannel channel,
                           String startTime, String endTime, int downloadSpeed,
                           SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException;


	/**
	 * Video streaming stopped
	 */
	void streamByeCmd(Device device, String channelId, String app, String stream, String callId, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

	void talkStreamCmd(MediaServer mediaServerItem, SendRtpInfo sendRtpItem, String ySsrc, Device device, DeviceChannel channelId, String callId, HookSubscribe.Event event, HookSubscribe.Event eventForPush, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException;

	void streamByeCmd(Device device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

	/**
	 * Playback paused
	 */
	void playPauseCmd(Device device, DeviceChannel channel, StreamInfo streamInfo) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * Playback resume
	 */
	void playResumeCmd(Device device, DeviceChannel channel, StreamInfo streamInfo) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * Playback drag and play
	 */
	void playSeekCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, long seekTime) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * Playback at double speed
	 */
	void playSpeedCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, Double speed) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * Playback control
	 * @param device
	 * @param streamInfo
	 * @param content
	 */
	void playbackControlCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, String content,SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException;


    void streamByeCmdForDeviceInvite(Device device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

    /**
	 * /**
	 * voice broadcast
	 *
	 * @param device video equipment
	 */
	void audioBroadcastCmd(Device device, String channelId, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Audio and video recording control
	 *
	 * @param device  		video equipment
	 * @param channelId  	Preview channel
	 * @param recordCmdStr	Recording command：Record / StopRecord
	 */
	void recordCmd(Device device, String channelId, String recordCmdStr, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Remote start control command
	 *
	 * @param device	video equipment
	 */
	void teleBootCmd(Device device) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Alarm arming/disarm order
	 *
	 * @param device  	video equipment
	 */
	void guardCmd(Device device, String guardCmdStr, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Alarm reset command
	 *
	 * @param device		video equipment
	 * @param alarmMethod	Alarm mode (optional）
	 * @param alarmType		Alarm type (optional）
	 */
	void alarmResetCmd(Device device, String alarmMethod, String alarmType, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Force key frame command, the device should immediately send an IDR frame after receiving this command
	 *
	 * @param device  video equipment
	 * @param channelId  Preview channel
	 */
	void iFrameCmd(Device device, String channelId) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * guard position control command
	 *
	 */
	void homePositionCmd(Device device, String channelId, Boolean enabled, Integer resetTime, Integer presetIndex, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Device configuration commands
	 *
	 * @param device  video equipment
	 */
	void deviceConfigCmd(Device device);

	/**
	 * Device configuration commands：basicParam
	 */
	void deviceBasicConfigCmd(Device device, BasicParam basicParam, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Device configuration commands：VideoParamOpt
	 */
	void deviceVideoParamConfigCmd(Device device, VideoParamOpt videoParamOpt, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Query device status
	 *
	 * @param device video equipment
	 */
	void deviceStatusQuery(Device device, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Query device information
	 *
	 * @param device   video equipment
	 * @param callback
	 * @return
	 */
	void deviceInfoQuery(Device device, ErrorCallback<Object> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Query directory listing
	 *
	 * @param device video equipment
	 */
	void catalogQuery(Device device, int sn, SipSubscribe.Event errorEvent) throws SipException, InvalidArgumentException, ParseException;

	/**
	 * Query video information
	 *
	 * @param device video equipment
	 * @param startTime Start time, format requirements：yyyy-MM-dd HH:mm:ss
	 * @param endTime End time, format requirements：yyyy-MM-dd HH:mm:ss
	 * @param sn
	 */
	void recordInfoQuery(Device device, String channelId, String startTime, String endTime, int sn,  Integer Secrecy, String type, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Query alarm information
	 *
	 * @param device		video equipment
	 * @param startPriority	Alarm starting level (optional）
	 * @param endPriority	Alarm termination level (optional）
	 * @param alarmMethod	Alarm mode conditions (optional）
	 * @param alarmType		Alarm type
	 * @param startTime		Start time of alarm occurrence (optional）
	 * @param endTime		Alarm occurrence end time (optional）
	 * @return				true = Command sent successfully
	 */
	void alarmInfoQuery(Device device, String startPriority, String endPriority, String alarmMethod,
							String alarmType, String startTime, String endTime,  ErrorCallback<Object> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Query device configuration
	 *
	 * @param device 		video equipment
	 * @param channelId		Channel encoding (optional）
	 * @param configClass	Configuration type：
	 */
	<T extends DeviceConfigAware> void deviceConfigQuery(Device device, String channelId, Class<T> configClass, ErrorCallback<T> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Query device preset location
	 *
	 * @param device video equipment
	 */
	void presetQuery(Device device, String channelId, ErrorCallback<List<Preset>> callback) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Query mobile device location data
	 *
	 * @param device video equipment
	 */
	void mobilePositionQuery(Device device, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Subscribe, unsubscribe mobile location
	 *
	 * @param device	video equipment
	 * @return			true = Command sent successfully
	 */
	SIPRequest mobilePositionSubscribe(Device device, SipTransactionInfo transactionInfo, SipSubscribe.Event okEvent , SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Subscribe and unsubscribe alarm information
	 */
	SIPRequest alarmSubscribe(Device device, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Subscribe and unsubscribe directory information
	 * @param device		video equipment
	 * @return				true = Command sent successfully
	 */
	SIPRequest catalogSubscribe(Device device, SipTransactionInfo transactionInfo, SipSubscribe.Event okEvent ,SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * Pull box control command
	 *
	 * @param device    control equipment
	 * @param channelId channelid
	 * @param cmdString Front-end control command string
	 */
	void dragZoomCmd(Device device, String channelId, String cmdString) throws InvalidArgumentException, SipException, ParseException;


    void playbackControlCmd(Device device, DeviceChannel channel, String stream, String content, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException;

    /**
	 * Send an alarm NOTIFY message to the device, which is used in the interconnection structure. At this time, the device is treated as a flat platform.
	 * @param device Equipment
	 * @param deviceAlarm Alarm information
	 * @return
	 */
	void sendAlarmMessage(Device device, DeviceAlarmNotify deviceAlarm) throws InvalidArgumentException, SipException, ParseException;


}
