package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author lin
 */
@Data
@Schema(description = "Platform information")
public class Platform {

    @Schema(description = "ID(in database)")
    private Integer id;

    @Schema(description = "Whether to enable")
    private boolean enable;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "SIPService national standard code")
    private String serverGBId;

    @Schema(description = "SIPService national standard domain")
    private String serverGBDomain;

    @Schema(description = "SIPserviceIP")
    private String serverIp;

    @Schema(description = "SIPservice port")
    private int serverPort;

    @Schema(description = "Equipment national standard number")
    private String deviceGBId;

    @Schema(description = "Equipmentip")
    private String deviceIp;

    @Schema(description = "Device port")
    private int devicePort;

    @Schema(description = "SIPAuthentication username(The equipment national standard number is used by default.)")
    private String username;

    @Schema(description = "SIPAuthentication password")
    private String password;

    @Schema(description = "Registration cycle (seconds)")
    private int expires;

    @Schema(description = "heartbeat cycle(seconds)")
    private int keepTimeout;

    @Schema(description = "transport protocol")
    private String transport;

    @Schema(description = "character set")
    private String characterSet;

    @Schema(description = "Allow PTZ control")
    private boolean ptz;

    @Schema(description = "RTCPSurvival of refugees")
    private boolean rtcp;

    @Schema(description = "online status")
    private boolean status;

    @Schema(description = "Number of channels")
    private int channelCount;

    @Schema(description = "Directory information has been subscribed")
    private boolean catalogSubscribe;

    @Schema(description = "Already subscribed to alarm information")
    private boolean alarmSubscribe;

    @Schema(description = "has been subscribed to mobile location information")
    private boolean mobilePositionSubscribe;

    @Schema(description = "directory grouping-The number of channels carried in a single packet each time channel information is sent to the superior, value1,2,4,8")
    private int catalogGroup;

    @Schema(description = "Update time")
    private String updateTime;

    @Schema(description = "creation time")
    private String createTime;

    @Schema(description = "Whether to serve as a message channel")
    private boolean asMessageChannel;

    @Schema(description = "Used for on-demand reply 200OKIP")
    private String sendStreamIp;

    @Schema(description = "Whether to automatically push channel changes")
    private Boolean autoPushChannel;

    @Schema(description = "Directory information contains platform information, 0: closed, 1: open")
    private int catalogWithPlatform;

    @Schema(description = "Directory information contains grouping information, 0: closed, 1: open")
    private int catalogWithGroup;

    @Schema(description = "Directory information includes administrative division, 0: closed, 1: open")
    private int catalogWithRegion;

    @Schema(description = "Administrative division")
    private String civilCode;

    @Schema(description = "Platform vendors")
    private String manufacturer;

    @Schema(description = "Platform model")
    private String model;

    @Schema(description = "Platform installation address")
    private String address;

    @Schema(description = "Registration method (required), the default is1； " +
            "1-Certification registration model compliant with IETF RFC 3261 standard；" +
            "2-Password-based two-way authentication registration mode；" +
            "3-Two-way authentication registration mode based on digital certificate(High security level requirements)；" +
            "4-One-way authentication registration mode based on digital certificate (high security level requirements）")
    private int registerWay = 1;

    @Schema(description = "Confidentiality attribute (required) defaults to0；0-Not confidential，1-Confidential")
    private int secrecy = 0;

    @Schema(description = "Execute registered servicesID")
    private String serverId;
}
