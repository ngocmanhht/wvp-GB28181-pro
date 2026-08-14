package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "National standard channel")
public class CommonGBChannel {

    @Schema(description = "National standard-Database auto-incrementID")
    private int gbId;

    @Schema(description = "National standard-encoding")
    private String gbDeviceId;

    @Schema(description = "National standard-Name")
    private String gbName;

    @Schema(description = "National standard-Equipment manufacturer")
    private String gbManufacturer;

    @Schema(description = "National standard-Device model")
    private String gbModel;

    // 2016
    @Schema(description = "National standard-Equipment ownership")
    private String gbOwner;

    @Schema(description = "National standard-Administrative region")
    private String gbCivilCode;

    @Schema(description = "National standard-police district")
    private String gbBlock;

    @Schema(description = "National standard-Installation address")
    private String gbAddress;

    @Schema(description = "National standard-Is there a sub-device?")
    private Integer gbParental;

    @Schema(description = "National standard-parent nodeID")
    private String gbParentId;

    // 2016
    @Schema(description = "National standard-Signaling security mode")
    private Integer gbSafetyWay;

    @Schema(description = "National standard-Registration method")
    private Integer gbRegisterWay;

    // 2016
    @Schema(description = "National standard-Certificate serial number")
    private String gbCertNum;

    // 2016
    @Schema(description = "National standard-Certificate valid identifier")
    private Integer gbCertifiable;

    // 2016
    @Schema(description = "National standard-Invalid reason code(Required for devices with certificates and invalid certificates)")
    private Integer gbErrCode;

    // 2016
    @Schema(description = "National standard-Certificate expiry date(Required for devices with certificates and invalid certificates)")
    private String gbEndTime;

    @Schema(description = "National standard-Confidential attribute(Required)The default is0;0-Not confidential,1-Confidential")
    private Integer gbSecrecy;

    @Schema(description = "National standard-Equipment/systemIPv4/IPv6address")
    private String gbIpAddress;

    @Schema(description = "National standard-Equipment/system port")
    private Integer gbPort;

    @Schema(description = "National standard-Device password")
    private String gbPassword;

    @Schema(description = "National standard-Device status")
    private String gbStatus;

    @Schema(description = "National standard-longitude WGS-84coordinate system")
    private Double gbLongitude;

    @Schema(description = "National standard-Latitude WGS-84coordinate system")
    private Double gbLatitude;

    private Double gpsAltitude;

    private Double gpsSpeed;

    private Double gpsDirection;

    private String gpsTime;

    @Schema(description = "National standard-The business group to which the virtual organization belongsID")
    private String gbBusinessGroupId;

    @Schema(description = "National standard-Camera structure type, identifying camera type: 1-ball machine; 2-hemisphere; 3-Fixed bolt; 4-remote control gun;5-remote controlled hemisphere;6-Panoramic view of multi-view equipment/Splicing channel;" +
            "7-Split channels for multi-channel equipment; 99-Mobile devices (non-standard）98-Conference equipment (non-standard）")
    private Integer gbPtzType;

    // 2016
    @Schema(description = "-Camera position type extension。1-interprovincial checkpoint、2-Party and government organs、3-Station Pier、4-central square、5-sports venues、6-business center、7-religious place、" +
            "8-Around campus、9-Complex security area、10-Traffic arteries. Optional when the directory item is a camera。")
    private Integer gbPositionType;

    @Schema(description = "National standard-Camera installation location outdoor and indoor attributes。1-outdoor、2-indoor。")
    private Integer gbRoomType;

    // 2016
    @Schema(description = "National standard-Usage attribute")
    private Integer gbUseType;

    @Schema(description = "National standard-Camera fill light properties。1-No fill light;2-Infrared fill light;3-white light fill light;4-Laser fill light;9-Others")
    private Integer gbSupplyLightType;

    @Schema(description = "National standard-Camera surveillance location(Optical axis direction)Properties。1-East(west to east)、2-west(east to west)、3-South(north to south)、4-north(south to north)、" +
            "5-Southeast(northwest to southeast)、6-Northeast(Southwest to Northeast)、7-Southwest(Northeast to Southwest)、8-Northwest(southeast to northwest)")
    private Integer gbDirectionType;

    @Schema(description = "National standard-The resolution supported by the camera, which can be multi-value")
    private String gbResolution;

    @Schema(description = "National standard-Download twice as fast(Optional),Can be multi-valued")
    private String gbDownloadSpeed;

    @Schema(description = "National standard-Airspace coding capability, value0-Not supported;1-1level enhancement(1enhancement layer);2-2level enhancement(2enhancement layer);3-3level enhancement(3enhancement layer)")
    private Integer gbSvcSpaceSupportMod;

    @Schema(description = "National standard-Time domain coding capability, value0-Not supported;1-1level enhancement;2-2level enhancement;3-3level enhancement(Optional)")
    private Integer gbSvcTimeSupportMode;

    @Schema(description = "Binary saved recording schedule, each bit represents the first half hour of each hour")
    private Long recordPLan;

    @Schema(description = "associated data types")
    private Integer dataType;

    @Schema(description = "Associated devicesID")
    private Integer dataDeviceId;

    @Schema(description = "creation time")
    private String createTime;

    @Schema(description = "Update time")
    private String updateTime;

    @Schema(description = "The unique number of the stream. If it exists, it means it is live broadcasting.")
    private String  streamId;

    @Schema(description = "Whether to support intercom 1 supports, 0 does not support")
    private Integer enableBroadcast;

    @Schema(description = "Layer level after thinning")
    private Integer mapLevel;

    public String encode(String serverDeviceId) {
        return encode(null, serverDeviceId);
    }

    public String encode(String event,String serverDeviceId) {
        String content;
        if (event == null) {
            return getFullContent(null, serverDeviceId);
        }
        switch (event) {
            case CatalogEvent.DEL:
            case CatalogEvent.DEFECT:
            case CatalogEvent.VLOST:
                content = "<Item>\n" +
                        "<DeviceID>" + this.getGbDeviceId() + "</DeviceID>\n" +
                        "<Event>" + event + "</Event>\n" +
                        "</Item>\n";
                break;
            case CatalogEvent.ON:
            case CatalogEvent.OFF:
                content = "<Item>\n" +
                        "<DeviceID>" + this.getGbDeviceId() + "</DeviceID>\n" +
                        "<Event>" + event + "</Event>\r\n" +
                        "</Item>\n";
                break;
            case CatalogEvent.ADD:
            case CatalogEvent.UPDATE:
                content = getFullContent(event, serverDeviceId);
                break;
            default:
                content = null;
                break;
        }
        return content;
    }

    private String getFullContent(String event, String serverDeviceId) {
        StringBuilder content = new StringBuilder();
        // Administrative division catalog items
        content.append("<Item>\n")
                .append("<DeviceID>" + this.getGbDeviceId() + "</DeviceID>\n")
                .append("<Name>" + this.getGbName() + "</Name>\n");


        if (this.getGbDeviceId().length() > 8) {

            String type = this.getGbDeviceId().substring(10, 13);
            if (type.equals("200")) {
                // business group catalog item
                if (this.getGbManufacturer() != null) {
                    content.append("<Manufacturer>" + this.getGbManufacturer() + "</Manufacturer>\n");
                }
                if (this.getGbModel() != null) {
                    content.append("<Model>" + this.getGbModel() + "</Model>\n");
                }
                if (this.getGbOwner() != null) {
                    content.append("<Owner>" + this.getGbOwner() + "</Owner>\n");
                }
                if (this.getGbCivilCode() != null) {
                    content.append("<CivilCode>" + this.getGbCivilCode() + "</CivilCode>\n");
                }
                if (this.getGbAddress() != null) {
                    content.append("<Address>" + this.getGbAddress() + "</Address>\n");
                }
                if (this.getGbRegisterWay() != null) {
                    content.append("<RegisterWay>" + this.getGbRegisterWay() + "</RegisterWay>\n");
                }
                if (this.getGbSecrecy() != null) {
                    content.append("<Secrecy>" + this.getGbSecrecy() + "</Secrecy>\n");
                }
            } else if (type.equals("215")) {
                // business grouping
                if (this.getGbCivilCode() != null) {
                    content.append("<CivilCode>" + this.getGbCivilCode() + "</CivilCode>\n");
                }
                content.append("<ParentID>" + serverDeviceId + "</ParentID>\n");
            } else if (type.equals("216")) {
                // virtual organization directory entry
                if (this.getGbCivilCode() != null) {
                    content.append("<CivilCode>" + this.getGbCivilCode() + "</CivilCode>\n");
                }
                if (this.getGbParentId() != null) {
                    content.append("<ParentID>" + this.getGbParentId() + "</ParentID>\n");
                }
                content.append("<BusinessGroupID>" + this.getGbBusinessGroupId() + "</BusinessGroupID>\n");
            } else {
                if (this.getGbManufacturer() != null) {
                    content.append("<Manufacturer>" + this.getGbManufacturer() + "</Manufacturer>\n");
                }
                if (this.getGbModel() != null) {
                    content.append("<Model>" + this.getGbModel() + "</Model>\n");
                }
                if (this.getGbOwner() != null) {
                    content.append("<Owner>" + this.getGbOwner() + "</Owner>\n");
                }
                if (this.getGbCivilCode() != null) {
                    content.append("<CivilCode>" + this.getGbCivilCode() + "</CivilCode>\n");
                }
                if (this.getGbAddress() != null) {
                    content.append("<Address>" + this.getGbAddress() + "</Address>\n");
                }
                if (this.getGbParentId() != null) {
                    content.append("<ParentID>" + this.getGbParentId() + "</ParentID>\n");
                }
                if (this.getGbParental() != null) {
                    content.append("<Parental>" + this.getGbParental() + "</Parental>\n");
                }
                if (this.getGbSafetyWay() != null) {
                    content.append("<SafetyWay>" + this.getGbSafetyWay() + "</SafetyWay>\n");
                }
                if (this.getGbRegisterWay() != null) {
                    content.append("<RegisterWay>" + this.getGbRegisterWay() + "</RegisterWay>\n");
                }
                if (this.getGbCertNum() != null) {
                    content.append("<CertNum>" + this.getGbCertNum() + "</CertNum>\n");
                }
                if (this.getGbCertifiable() != null) {
                    content.append("<Certifiable>" + this.getGbCertifiable() + "</Certifiable>\n");
                }
                if (this.getGbErrCode() != null) {
                    content.append("<ErrCode>" + this.getGbErrCode() + "</ErrCode>\n");
                }
                if (this.getGbEndTime() != null) {
                    content.append("<EndTime>" + this.getGbEndTime() + "</EndTime>\n");
                }
                if (this.getGbSecrecy() != null) {
                    content.append("<Secrecy>" + this.getGbSecrecy() + "</Secrecy>\n");
                }
                if (this.getGbIpAddress() != null) {
                    content.append("<IPAddress>" + this.getGbIpAddress() + "</IPAddress>\n");
                }
                if (this.getGbPort() != null) {
                    content.append("<Port>" + this.getGbPort() + "</Port>\n");
                }
                if (this.getGbPassword() != null) {
                    content.append("<Password>" + this.getGbPassword() + "</Password>\n");
                }
                if (this.getGbStatus() != null) {
                    content.append("<Status>" + this.getGbStatus() + "</Status>\n");
                }
                if (this.getGbLongitude() != null) {
                    content.append("<Longitude>" + this.getGbLongitude() + "</Longitude>\n");
                }
                if (this.getGbLatitude() != null) {
                    content.append("<Latitude>" + this.getGbLatitude() + "</Latitude>\n");
                }
                content.append("<Info>\n");

                if (this.getGbPtzType() != null) {
                    content.append("  <PTZType>" + this.getGbPtzType() + "</PTZType>\n");
                }
                if (this.getGbPositionType() != null) {
                    content.append("  <PositionType>" + this.getGbPositionType() + "</PositionType>\n");
                }
                if (this.getGbRoomType() != null) {
                    content.append("  <RoomType>" + this.getGbRoomType() + "</RoomType>\n");
                }
                if (this.getGbUseType() != null) {
                    content.append("  <UseType>" + this.getGbUseType() + "</UseType>\n");
                }
                if (this.getGbSupplyLightType() != null) {
                    content.append("  <SupplyLightType>" + this.getGbSupplyLightType() + "</SupplyLightType>\n");
                }
                if (this.getGbDirectionType() != null) {
                    content.append("  <DirectionType>" + this.getGbDirectionType() + "</DirectionType>\n");
                }
                if (this.getGbResolution() != null) {
                    content.append("  <Resolution>" + this.getGbResolution() + "</Resolution>\n");
                }
                if (this.getGbBusinessGroupId() != null) {
                    content.append("  <BusinessGroupID>" + this.getGbBusinessGroupId() + "</BusinessGroupID>\n");
                }
                if (this.getGbDownloadSpeed() != null) {
                    content.append("  <DownloadSpeed>" + this.getGbDownloadSpeed() + "</DownloadSpeed>\n");
                }
                if (this.getGbSvcSpaceSupportMod() != null) {
                    content.append("  <SVCSpaceSupportMode>" + this.getGbSvcSpaceSupportMod() + "</SVCSpaceSupportMode>\n");
                }
                if (this.getGbSvcTimeSupportMode() != null) {
                    content.append("  <SVCTimeSupportMode>" + this.getGbSvcTimeSupportMode() + "</SVCTimeSupportMode>\n");
                }
                if (this.getEnableBroadcast() != null) {
                    content.append("  <EnableBroadcast>" + this.getEnableBroadcast() + "</EnableBroadcast>\n");
                }
                content.append("</Info>\n");
            }
        }
        if (event != null) {
            content.append("<Event>" + event + "</Event>\n");
        }
        content.append("</Item>\n");
        return content.toString();
    }

    public static CommonGBChannel build(Group group) {
        GbCode gbCode = GbCode.decode(group.getDeviceId());
        CommonGBChannel channel = new CommonGBChannel();
        if (gbCode.getTypeCode().equals("215")) {
            // business grouping
            channel.setGbName(group.getName());
            channel.setGbDeviceId(group.getDeviceId());
            channel.setGbCivilCode(group.getCivilCode());
        } else {
            // virtual organization
            channel.setGbName(group.getName());
            channel.setGbDeviceId(group.getDeviceId());
            channel.setGbParentId(group.getParentDeviceId());
            channel.setGbBusinessGroupId(group.getBusinessGroup());
            channel.setGbCivilCode(group.getCivilCode());
        }
        return channel;
    }

    public static CommonGBChannel build(Platform platform) {
        CommonGBChannel commonGBChannel = new CommonGBChannel();
        commonGBChannel.setGbDeviceId(platform.getDeviceGBId());
        commonGBChannel.setGbName(platform.getName());
        commonGBChannel.setGbManufacturer(platform.getManufacturer());
        commonGBChannel.setGbModel(platform.getModel());
        commonGBChannel.setGbCivilCode(platform.getCivilCode());
        commonGBChannel.setGbAddress(platform.getAddress());
        commonGBChannel.setGbRegisterWay(platform.getRegisterWay());
        commonGBChannel.setGbSecrecy(platform.getSecrecy());
        commonGBChannel.setGbStatus(platform.isStatus() ? "ON" : "OFF");
        return commonGBChannel;
    }

    public static CommonGBChannel build(Region region) {
        CommonGBChannel commonGBChannel = new CommonGBChannel();
        commonGBChannel.setGbDeviceId(region.getDeviceId());
        commonGBChannel.setGbName(region.getName());
        return commonGBChannel;
    }

    @Override
    public String toString() {
        return "CommonGBChannel{" +
                "gbId=" + gbId +
                ", gbDeviceId='" + gbDeviceId + '\'' +
                ", gbName='" + gbName + '\'' +
                ", gbManufacturer='" + gbManufacturer + '\'' +
                ", gbModel='" + gbModel + '\'' +
                ", gbOwner='" + gbOwner + '\'' +
                ", gbCivilCode='" + gbCivilCode + '\'' +
                ", gbBlock='" + gbBlock + '\'' +
                ", gbAddress='" + gbAddress + '\'' +
                ", gbParental=" + gbParental +
                ", gbParentId='" + gbParentId + '\'' +
                ", gbSafetyWay=" + gbSafetyWay +
                ", gbRegisterWay=" + gbRegisterWay +
                ", gbCertNum='" + gbCertNum + '\'' +
                ", gbCertifiable=" + gbCertifiable +
                ", gbErrCode=" + gbErrCode +
                ", gbEndTime='" + gbEndTime + '\'' +
                ", gbSecrecy=" + gbSecrecy +
                ", gbIpAddress='" + gbIpAddress + '\'' +
                ", gbPort=" + gbPort +
                ", gbPassword='" + gbPassword + '\'' +
                ", gbStatus='" + gbStatus + '\'' +
                ", gbLongitude=" + gbLongitude +
                ", gbLatitude=" + gbLatitude +
                ", gpsAltitude=" + gpsAltitude +
                ", gpsSpeed=" + gpsSpeed +
                ", gpsDirection=" + gpsDirection +
                ", gpsTime='" + gpsTime + '\'' +
                ", gbBusinessGroupId='" + gbBusinessGroupId + '\'' +
                ", gbPtzType=" + gbPtzType +
                ", gbPositionType=" + gbPositionType +
                ", gbRoomType=" + gbRoomType +
                ", gbUseType=" + gbUseType +
                ", gbSupplyLightType=" + gbSupplyLightType +
                ", gbDirectionType=" + gbDirectionType +
                ", gbResolution='" + gbResolution + '\'' +
                ", gbDownloadSpeed='" + gbDownloadSpeed + '\'' +
                ", gbSvcSpaceSupportMod=" + gbSvcSpaceSupportMod +
                ", gbSvcTimeSupportMode=" + gbSvcTimeSupportMode +
                ", recordPLan=" + recordPLan +
                ", dataType=" + dataType +
                ", dataDeviceId=" + dataDeviceId +
                ", createTime='" + createTime + '\'' +
                ", updateTime='" + updateTime + '\'' +
                ", streamId='" + streamId + '\'' +
                ", enableBroadcast=" + enableBroadcast +
                ", mapLevel=" + mapLevel +
                '}';
    }
}
