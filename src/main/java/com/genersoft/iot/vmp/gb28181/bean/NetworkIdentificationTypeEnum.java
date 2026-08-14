package com.genersoft.iot.vmp.gb28181.bean;

/**
 * Include industry codes
 */
public enum NetworkIdentificationTypeEnum {
    PUBLIC_SECURITY_VIDEO_TRANSMISSION_NETWORK("0", "Public Security Video Transmission Network"),
    PUBLIC_SECURITY_VIDEO_TRANSMISSION_NETWORK2("1", "Public Security Video Transmission Network"),
    INDUSTRY_SPECIFIC_NETWORK("2", "Industry private network"),
    POLITICAL_AND_LEGAL_INFORMATION_NETWORK("3", "Political and Legal Information Network"),
    PUBLIC_SECURITY_MOBILE_INFORMATION_NETWORK("4", "Public Security Mobile Information Network"),
    PUBLIC_SECURITY_INFORMATION_NETWORK("5", "Public Security Information Network"),
    ELECTRONIC_GOVERNMENT_EXTRANET("6", "E-Government Extranet"),
    PUBLIC_NETWORKS_SUCH_AS_THE_INTERNET("7", "Public networks such as the Internet"),
    Dedicated_Line("8", "dedicated line"),
    RESERVE("9", "reserved"),
    ;

    /**
     * Access type code
     */
    private String name;

    /**
     * Name
     */
    private String code;


    NetworkIdentificationTypeEnum(String code, String name) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
