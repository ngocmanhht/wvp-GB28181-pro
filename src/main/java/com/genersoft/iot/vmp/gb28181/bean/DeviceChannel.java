package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.utils.MessageElementForCatalog;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.InvocationTargetException;

@Data
@Slf4j
@Schema(description = "Channel information")
@EqualsAndHashCode(callSuper = true)
public class DeviceChannel extends CommonGBChannel {

	@Schema(description = "Database auto-incrementID")
	private int id;

	@Schema(description = "Parent device encoding")
	private String parentDeviceId;

	@Schema(description = "Parent device name")
	private String parentName;

	@MessageElementForCatalog("DeviceID")
	@Schema(description = "encoding")
	private String deviceId;

	@MessageElementForCatalog("Name")
	@Schema(description = "Name")
	private String name;

	@MessageElementForCatalog("Manufacturer")
	@Schema(description = "Equipment manufacturer")
	private String manufacturer;

	@MessageElementForCatalog("Model")
	@Schema(description = "Device model")
	private String model;

	// 2016
	@MessageElementForCatalog("Owner")
	@Schema(description = "Equipment ownership")
	private String owner;

	@MessageElementForCatalog("CivilCode")
	@Schema(description = "Administrative region")
	private String civilCode;

	@MessageElementForCatalog("Block")
	@Schema(description = "police district")
	private String block;

	@MessageElementForCatalog("Address")
	@Schema(description = "Installation address")
	private String address;

	@MessageElementForCatalog("Parental")
	@Schema(description = "Is there a sub-device?(Required)1Yes, 0 No")
	private Integer parental;


	@MessageElementForCatalog("ParentID")
	@Schema(description = "parent nodeID")
	private String parentId;

	// 2016
	@MessageElementForCatalog("SafetyWay")
	@Schema(description = "Signaling security mode")
	private Integer safetyWay;

	@MessageElementForCatalog("RegisterWay")
	@Schema(description = "Registration method")
	private Integer registerWay;

	// 2016
	@MessageElementForCatalog("CertNum")
	@Schema(description = "Certificate serial number")
	private String certNum;

	// 2016
	@MessageElementForCatalog("Certifiable")
	@Schema(description = "Certificate valid ID, default is0;Certificate valid identification: 0: invalid 1: valid")
	private Integer certifiable;

	// 2016
	@MessageElementForCatalog("ErrCode")
	@Schema(description = "Invalid reason code(Required for devices with certificates and invalid certificates)")
	private Integer errCode;

	// 2016
	@MessageElementForCatalog("EndTime")
	@Schema(description = "Certificate expiry date(Required for devices with certificates and invalid certificates)")
	private String endTime;

	@MessageElementForCatalog("Secrecy")
	@Schema(description = "Confidential attribute(Required)The default is0;0-Not confidential,1-Confidential")
	private Integer secrecy;

	@MessageElementForCatalog("IPAddress")
	@Schema(description = "Equipment/systemIPv4/IPv6address")
	private String ipAddress;

	@MessageElementForCatalog("Port")
	@Schema(description = "Equipment/system port")
	private Integer port;

	@MessageElementForCatalog("Password")
	@Schema(description = "Device password")
	private String password;

	@MessageElementForCatalog("Status")
	@Schema(description = "Device status")
	private String status;

	@MessageElementForCatalog("Longitude")
	@Schema(description = "longitude WGS-84coordinate system")
	private Double longitude;

	@MessageElementForCatalog("Latitude")
	@Schema(description = ",Latitude WGS-84coordinate system")
	private Double latitude;

	@MessageElementForCatalog("Info.PTZType")
	@Schema(description = "Camera structure type, identifying camera type: 1-ball machine; 2-hemisphere; 3-Fixed bolt; 4-remote control gun;5-remote controlled hemisphere;6-Panoramic view of multi-view equipment/Splicing channel;7-Split channels for multi-channel equipment")
	private Integer ptzType;

	@MessageElementForCatalog("Info.PositionType")
	@Schema(description = "Camera position type extension。1-interprovincial checkpoint、2-Party and government organs、3-Station Pier、4-central square、5-sports venues、" +
			"6-business center、7-religious place、8-Around campus、9-Complex security area、10-traffic arteries")
	private Integer positionType;

	@MessageElementForCatalog("Info.RoomType")
	@Schema(description = "Camera installation location outdoor and indoor attributes。1-outdoor、2-indoor。")
	private Integer roomType;

	@MessageElementForCatalog("Info.UseType")
	@Schema(description = "Usage attribute， 1-law and order、2-transportation、3-focus。")
	private Integer useType;

	@MessageElementForCatalog("Info.SupplyLightType")
	@Schema(description = "Camera fill light properties。1-No fill light;2-Infrared fill light;3-white light fill light;4-Laser fill light;9-Others")
	private Integer supplyLightType;

	@MessageElementForCatalog("Info.DirectionType")
	@Schema(description = "Camera surveillance location(Optical axis direction)Properties。1-East(west to east)、2-west(east to west)、3-South(north to south)、4-north(south to north)、" +
			"5-Southeast(northwest to southeast)、6-Northeast(Southwest to Northeast)、7-Southwest(Northeast to Southwest)、8-Northwest(southeast to northwest)")
	private Integer directionType;

	@MessageElementForCatalog("Info.Resolution")
	@Schema(description = "The resolution supported by the camera, which can be multi-value")
	private String resolution;

	@MessageElementForCatalog({"BusinessGroupID","Info.BusinessGroupID"})
	@Schema(description = "The business group to which the virtual organization belongsID")
	private String businessGroupId;

	@MessageElementForCatalog("Info.DownloadSpeed")
	@Schema(description = "Download twice as fast(Optional),Can be multi-valued")
	private String downloadSpeed;

	@MessageElementForCatalog("Info.SVCSpaceSupportMode")
	@Schema(description = "Airspace coding capability, value0-Not supported;1-1level enhancement(1enhancement layer);2-2level enhancement(2enhancement layer);3-3level enhancement(3enhancement layer)")
	private Integer svcSpaceSupportMod;

	@MessageElementForCatalog("Info.SVCTimeSupportMode")
	@Schema(description = "Time domain coding capability, value0-Not supported;1-1level enhancement;2-2level enhancement;3-3level enhancement(Optional)")
	private Integer svcTimeSupportMode;

	@Schema(description = "PTZ type description string")
	private String ptzTypeText;

	@Schema(description = "Number of sub-devices")
	private int subCount;

	@Schema(description = "Does it contain audio")
	private boolean hasAudio;

	@Schema(description = "GPSupdate time")
	private String gpsTime;

	@Schema(description = "The code stream identifier has a higher priority than the code stream identifier in the device.，" +
			"Used to form a code stream identifier when selecting a code stream. Defaults to null, not set. Optional value: stream/streamnumber/streamprofile/streamMode")
	private String streamIdentification;

	@Schema(description = "Channel type, default 0, 0: ordinary channel, 1 administrative division 2 business grouping/virtual organization")
	private int channelType;

	private String dbKey;

	private Integer dataType = ChannelDataType.GB28181;

	public void setPtzType(int ptzType) {
		this.ptzType = ptzType;
		switch (ptzType) {
			case 0:
				this.ptzTypeText = "unknown";
				break;
			case 1:
				this.ptzTypeText = "ball machine";
				break;
			case 2:
				this.ptzTypeText = "hemisphere";
				break;
			case 3:
				this.ptzTypeText = "Fixed bolt";
				break;
			case 4:
				this.ptzTypeText = "remote control gun";
				break;
			case 5:
				this.ptzTypeText = "remote controlled hemisphere";
				break;
			case 6:
				this.ptzTypeText = "Panoramic view of multi-view equipment/Splicing channel";
				break;
			case 7:
				this.ptzTypeText = "Split channels for multi-channel equipment";
				break;
		}
	}

	public static DeviceChannel decode(Element element) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		DeviceChannel deviceChannel = XmlUtil.elementDecode(element, DeviceChannel.class);
		if(deviceChannel.getCivilCode() != null ) {
			if (ObjectUtils.isEmpty(deviceChannel.getCivilCode())
					|| deviceChannel.getCivilCode().length() > 8 ){
				deviceChannel.setCivilCode(null);
			}
			// Here, administrative divisions that are not in the wvp cache are stored directly by default. This ensures that even if the administrative division cache of wvp is too old, the system can still be used normally through user-created methods.
		}
		GbCode gbCode = GbCode.decode(deviceChannel.getDeviceId());
		if (gbCode != null && "138".equals(gbCode.getTypeCode())) {
			deviceChannel.setHasAudio(true);
            if (deviceChannel.getEnableBroadcast() == null && "138".equals(gbCode.getTypeCode())) {
                deviceChannel.setEnableBroadcast(1);
            }
		}

		return deviceChannel;
	}

	public static DeviceChannel decodeWithOnlyDeviceId(Element element) {
		Element deviceElement = element.element("DeviceID");
		DeviceChannel deviceChannel = new DeviceChannel();
		deviceChannel.setDeviceId(deviceElement.getText());
		deviceChannel.setDataType(ChannelDataType.GB28181);
		return deviceChannel;
	}

	public CommonGBChannel buildCommonGBChannelForStatus() {
		CommonGBChannel commonGBChannel = new CommonGBChannel();
		commonGBChannel.setGbId(id);
		commonGBChannel.setGbDeviceId(deviceId);
		commonGBChannel.setGbName(name);
		commonGBChannel.setDataType(ChannelDataType.GB28181);
		commonGBChannel.setDataDeviceId(getDataDeviceId());
		return commonGBChannel;
	}


}
