package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlatformChannel extends CommonGBChannel {

    @Schema(description = "Id")
    private int id;

    @Schema(description = "platformID")
    private int platformId;

    @Schema(description = "National standard-encoding")
    private String customDeviceId;

    @Schema(description = "National standard-Name")
    private String customName;

    @Schema(description = "National standard-Equipment manufacturer")
    private String customManufacturer;

    @Schema(description = "National standard-Device model")
    private String customModel;

    // 2016
    @Schema(description = "National standard-Equipment ownership")
    private String customOwner;

    @Schema(description = "National standard-Administrative region")
    private String customCivilCode;

    @Schema(description = "National standard-police district")
    private String customBlock;

    @Schema(description = "National standard-Installation address")
    private String customAddress;

    @Schema(description = "National standard-Is there a sub-device?")
    private Integer customParental;

    @Schema(description = "National standard-parent nodeID")
    private String customParentId;

    // 2016
    @Schema(description = "National standard-Signaling security mode")
    private Integer customSafetyWay;

    @Schema(description = "National standard-Registration method")
    private Integer customRegisterWay;

    // 2016
    @Schema(description = "National standard-Certificate serial number")
    private Integer customCertNum;

    // 2016
    @Schema(description = "National standard-Certificate valid identifier")
    private Integer customCertifiable;

    // 2016
    @Schema(description = "National standard-Invalid reason code(Required for devices with certificates and invalid certificates)")
    private Integer customErrCode;

    // 2016
    @Schema(description = "National standard-Certificate expiry date(Required for devices with certificates and invalid certificates)")
    private Integer customEndTime;

    // 2022
    @Schema(description = "National standard-Camera security capability level code")
    private String customSecurityLevelCode;

    @Schema(description = "National standard-Confidential attribute(Required)The default is0;0-Not confidential,1-Confidential")
    private Integer customSecrecy;

    @Schema(description = "National standard-Equipment/systemIPv4/IPv6address")
    private String customIpAddress;

    @Schema(description = "National standard-Equipment/system port")
    private Integer customPort;

    @Schema(description = "National standard-Device password")
    private String customPassword;

    @Schema(description = "National standard-Device status")
    private String customStatus;

    @Schema(description = "National standard-longitude WGS-84coordinate system")
    private Double customLongitude;

    @Schema(description = "National standard-Latitude WGS-84coordinate system")
    private Double customLatitude;

    @Schema(description = "National standard-The business group to which the virtual organization belongsID")
    private String customBusinessGroupId;

    @Schema(description = "National standard-Camera structure type, identifying camera type: 1-ball machine; 2-hemisphere; 3-Fixed bolt; 4-remote control gun;5-remote controlled hemisphere;6-Panoramic view of multi-view equipment/Splicing channel;7-Split channels for multi-channel equipment")
    private Integer customPtzType;

    // 2016
    @Schema(description = "-Camera position type extension。1-interprovincial checkpoint、2-Party and government organs、3-Station Pier、4-central square、5-sports venues、6-business center、7-religious place、" +
            "8-Around campus、9-Complex security area、10-Traffic arteries. Optional when the directory item is a camera。")
    private Integer customPositionType;

    @Schema(description = "National standard-Camera photoelectric imaging type。1-visible light imaging;2-thermal imaging;3-radar imaging;4-Xlight imaging;5-Deep light field imaging;9-Others. Can be multi-valued,")
    private String customPhotoelectricImagingTyp;

    @Schema(description = "National standard-Camera acquisition part type")
    private String customCapturePositionType;

    @Schema(description = "National standard-Camera installation location outdoor and indoor attributes。1-outdoor、2-indoor。")
    private Integer customRoomType;

    // 2016
    @Schema(description = "National standard-Usage attribute")
    private Integer customUseType;

    @Schema(description = "National standard-Camera fill light properties。1-No fill light;2-Infrared fill light;3-white light fill light;4-Laser fill light;9-Others")
    private Integer customSupplyLightType;

    @Schema(description = "National standard-Camera surveillance location(Optical axis direction)Properties。1-East(west to east)、2-west(east to west)、3-South(north to south)、4-north(south to north)、" +
            "5-Southeast(northwest to southeast)、6-Northeast(Southwest to Northeast)、7-Southwest(Northeast to Southwest)、8-Northwest(southeast to northwest)")
    private Integer customDirectionType;

    @Schema(description = "National standard-The resolution supported by the camera, which can be multi-value")
    private String customResolution;

    // 2022
    @Schema(description = "National standard-List of code stream numbers supported by the camera, used to specify the code stream number during real-time on-demand playback(Optional)")
    private String customStreamNumberList;

    @Schema(description = "National standard-Download twice as fast(Optional),Can be multi-valued")
    private String customDownloadSpeed;

    @Schema(description = "National standard-Airspace coding capability, value0-Not supported;1-1level enhancement(1enhancement layer);2-2level enhancement(2enhancement layer);3-3level enhancement(3enhancement layer)")
    private Integer customSvcSpaceSupportMod;

    @Schema(description = "National standard-Time domain coding capability, value0-Not supported;1-1level enhancement;2-2level enhancement;3-3level enhancement(Optional)")
    private Integer customSvcTimeSupportMode;

    // 2022
    @Schema(description = "National standard- SSVCEnhancement layer and base layer ratio capabilities ")
    private String customSsvcRatioSupportList;

    // 2022
    @Schema(description = "National standard-Mobile collection device type(Only applicable to mobile collection devices, required);1-Mobile robot carrying camera;2-law enforcement recorder;3-Mobile individual equipment;" +
            "4-Vehicle-mounted video recording equipment;5-drone mounted camera;9-Others")
    private Integer customMobileDeviceType;

    // 2022
    @Schema(description = "National standard-Camera horizontal field of view(Optional),The value range is greater than 0 degrees and less than or equal to 360 degrees.")
    private Double customHorizontalFieldAngle;

    // 2022
    @Schema(description = "National standard-Camera vertical field of view(Optional),The value range is greater than 0 degrees and less than or equal to 360 degrees. ")
    private Double customVerticalFieldAngle;

    // 2022
    @Schema(description = "National standard-Camera viewing distance(Optional),Unit: meter")
    private Double customMaxViewDistance;

    // 2022
    @Schema(description = "National standard-grassroots organization code(Required, if not for grassroots construction“000000”)")
    private String customGrassrootsCode;

    // 2022
    @Schema(description = "National standard-Monitoring point type(Required when it is a camera),1-A type of video surveillance point;2-Class II video surveillance point;3-Three types of video surveillance points;9-Other points。")
    private Integer customPoType;

    // 2022
    @Schema(description = "National standard-Point commonly known as")
    private String customPoCommonName;

    // 2022
    @Schema(description = "National standard-Device MAC address(Optional),use“XX-XX-XX-XX-XX-XX”format expression")
    private String customMac;

    // 2022
    @Schema(description = "National standard-Camera mount function type,01-Face mount;02-Personnel bayonet;03-Motor vehicle bayonet;04-Non-motor vehicle bayonet;05-Item bay;99-Others")
    private String customFunctionType;

    // 2022
    @Schema(description = "National standard-Camera video encoding format")
    private String customEncodeType;

    // 2022
    @Schema(description = "National standard-Camera installation and usage time")
    private String customInstallTime;

    // 2022
    @Schema(description = "National standard-The name of the management unit to which the camera belongs")
    private String customManagementUnit;

    // 2022
    @Schema(description = "National standard-Contact information of the contact person of the management unit to which the camera belongs(Phone number, can be multiple values, use English half-width“/”split)")
    private String customContactInfo;

    // 2022
    @Schema(description = "National standard-Video storage days(Optional)")
    private Integer customRecordSaveDays;

    // 2022
    @Schema(description = "National standard-National Economic Industry Classification Code(Optional)")
    private String customIndustrialClassification;
    
}
