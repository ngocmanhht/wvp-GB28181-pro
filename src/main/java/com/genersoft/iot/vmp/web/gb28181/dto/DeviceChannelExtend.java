package com.genersoft.iot.vmp.web.gb28181.dto;

import lombok.Data;

@Data
public class DeviceChannelExtend {


	/**
	 * Database auto-incrementID
	 */
	private int id;

	/**
	 * channelid
	 */
	private String channelId;

	/**
	 * Equipmentid
	 */
	private String deviceId;
	
	/**
	 * Channel name
	 */
	private String name;

	private String deviceName;

	private boolean deviceOnline;
	
	/**
	 * Manufacturer
	 */
	private String manufacture;
	
	/**
	 * Model
	 */
	private String model;
	
	/**
	 * Equipment ownership
	 */
	private String owner;
	
	/**
	 * Administrative region
	 */
	private String civilCode;
	
	/**
	 * police district
	 */
	private String block;

	/**
	 * Installation address
	 */
	private String address;
	
	/**
	 * Whether there are sub-devices: 1 yes, 0 no
	 */
	private int parental;
	
	/**
	 * parentid
	 */
	private String parentId;
	
	/**
	 * Signaling security mode The default is0; 0:Not adopted; 2: S/MIMESignature method; 3: S/ MIMEEncrypted signature simultaneous use method; 4:digital summary method
	 */
	private int safetyWay;
	
	/**
	 * Registration method Default is1;1:Certification registration model that complies with IETFRFC3261 standard; 2:Password-based two-way authentication registration mode; 3:Two-way authentication registration mode based on digital certificate
	 */
	private int registerWay;
	
	/**
	 * Certificate serial number
	 */
	private String certNum;
	
	/**
	 * Certificate valid ID Default is0;Certificate valid identification: 0: invalid 1: valid
	 */
	private int certifiable;
	
	/**
	 * Certificate invalid reason code
	 */
	private int errCode;
	
	/**
	 * Certificate expiry date
	 */
	private String endTime;
	
	/**
	 * Confidential attribute defaults to0; 0:Not confidential, 1: confidential
	 */
	private String secrecy;
	
	/**
	 * IPaddress
	 */
	private String ipAddress;
	
	/**
	 * port number
	 */
	private int port;
	
	/**
	 * Password
	 */
	private String password;

	/**
	 * PTZ type
	 */
	private int PTZType;

	/**
	 * PTZ type description string
	 */
	private String PTZTypeText;

	/**
	 * creation time
	 */
	private String createTime;

	/**
	 * Update time
	 */
	private String updateTime;
	
	/**
	 * online/Offline
	 * 1Online, 0 offline
	 * Default online
	 * signaling:
	 * <Status>ON</Status>
	 * <Status>OFF</Status>
	 * I have encountered that the IPC signaling under NVR can push the stream, but the Status response OFF
	 */
	private String status;

	/**
	 * longitude
	 */
	private double longitude;
	
	/**
	 * Latitude
	 */
	private double latitude;

	/**
	 * longitude GCJ02
	 */
	private double longitudeGcj02;

	/**
	 * Latitude GCJ02
	 */
	private double latitudeGcj02;

	/**
	 * longitude WGS84
	 */
	private double longitudeWgs84;

	/**
	 * Latitude WGS84
	 */
	private double latitudeWgs84;

	/**
	 * Number of sub-devices
	 */
	private int subCount;

	/**
	 * The unique number of the stream. If it exists, it means it is live broadcasting.
	 */
	private String  streamId;

	/**
	 *  Does it contain audio
	 */
	private boolean hasAudio;

	/**
	 * Mark channel type，0->National standard channel 1->Live streaming channel 2->business grouping/virtual organization/Administrative division
	 */
	private int channelType;

	/**
	 * business grouping
	 */
	private String businessGroupId;

	/**
	 * GPSupdate time
	 */
	private String gpsTime;


	public void setPTZType(int PTZType) {
		this.PTZType = PTZType;
		switch (PTZType) {
			case 0:
				this.PTZTypeText = "unknown";
				break;
			case 1:
				this.PTZTypeText = "ball machine";
				break;
			case 2:
				this.PTZTypeText = "hemisphere";
				break;
			case 3:
				this.PTZTypeText = "Fixed bolt";
				break;
			case 4:
				this.PTZTypeText = "remote control gun";
				break;
		}
	}
}
