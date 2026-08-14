package com.genersoft.iot.vmp.gb28181.bean;

/**
 * Include industry codes
 */
public enum DeviceTypeEnum {
    DVR("111", "DVRencoding", "Front-end main device"),
    VIDEO_SERVER("112", "Video server encoding", "Front-end main device"),
    ENCODER("113", "encoder encoding", "Front-end main device"),
    DECODER("114", "decoder encoding", "Front-end main device"),
    VIDEO_SWITCHING_MATRIX("115", "Video switching matrix encoding", "Front-end main device"),
    AUDIO_SWITCHING_MATRIX("116", "audio switching matrix encoding", "Front-end main device"),
    ALARM_CONTROLLER("117", "Alarm controller code", "Front-end main device"),
    NVR("118", "Network Video Recorder (NVR) Encoding", "Front-end main device"),
    RESERVE("119", "reserved", "Front-end main device"),
    ONLINE_VIDEO_IMAGE_INFORMATION_ACQUISITION_SYSTEM("120", "Online video image information collection system coding", "Front-end main device"),
    VIDEO_CHECKPOINT("121", "Video card encoding", "Front-end main device"),
    MULTI_CAMERA_DEVICE("122", "Multi-viewer device encoding", "Front-end main device"),
    PARKING_LOT_ENTRANCE_AND_EXIT_CONTROL_EQUIPMENT("123", "Parking lot entrance and exit control equipment coding", "Front-end main device"),
    PERSONNEL_ACCESS_CONTROL_EQUIPMENT("124", "Personnel entrance and exit control equipment coding", "Front-end main device"),
    SECURITY_INSPECTION_EQUIPMENT("125", "Security equipment coding", "Front-end main device"),
    HVR("130", "Hybrid Hard Disk Recorder (HVR) Encoding", "Front-end main device"),
    CAMERA("131", "camera encoding", "front-end peripherals"),
    IPC("132", "IP camera（IPC）/Online video image information collection equipment coding", "front-end peripherals"),
    MONITOR("133", "Monitor encoding", "front-end peripherals"),
    ALARM_INPUT_DEVICE("134", "Alarm input device code (such as infrared, smoke detector, access control and other alarm equipment）", "front-end peripherals"),
    ALARM_OUTPUT_DEVICE("135", "Alarm output device code(Equipment such as police lights and bells)", "front-end peripherals"),
    VOICE_INPUT_DEVICE("136", "Voice input device encoding", "front-end peripherals"),
    VOICE_OUTPUT_DEVICE("137", "speech output device", "front-end peripherals"),
    MOBILE_TRANSMISSION_EQUIPMENT("138", "Mobile transmission equipment coding", "front-end peripherals"),
    OTHER_PERIPHERAL_DEVICES("139", "Other peripheral device encoding", "front-end peripherals"),
    ALARM_OUTPUT_DEVICE2("140", "Alarm output device code(Devices controlled by relays or triggers)", "front-end peripherals"),
    BARRIER_GATE("141", "gate(Control vehicle traffic)", "front-end peripherals"),
    SMART_DOOR("142", "smart door(Controlling personnel access)", "front-end peripherals"),
    VOUCHER_RECOGNITION_UNIT("143", "Voucher recognition unit", "front-end peripherals"),
    CENTRAL_SIGNALING_CONTROL_SERVER("200", "Central signaling control server encoding", "Platform equipment"),
    WEB_APPLICATION_SERVER("201", "Webapplication server encoding", "Platform equipment"),
    PROXY_SERVER("203", "Proxy server encoding", "Platform equipment"),
    SECURITY_SERVER("204", "Secure server encoding", "Platform equipment"),
    ALARM_SERVER("205", "Alarm server code", "Platform equipment"),
    DATABASE_SERVER("206", "Database server encoding", "Platform equipment"),
    GIS_SERVER("207", "GISServer encoding", "Platform equipment"),
    MANAGER_SERVER("208", "Management server encoding", "Platform equipment"),
    ACCESS_GATEWAY("209", "Access gateway code", "Platform equipment"),
    MEDIA_STORAGE_SERVER("210", "Media storage server encoding", "Platform equipment"),
    SIGNALING_SECURITY_ROUTING_GATEWAY("211", "Signaling secure routing gateway encoding", "Platform equipment"),
    BUSINESS_GROUP("215", "Business group coding", "Platform equipment"),
    VIRTUAL_ORGANIZATION("216", "virtual organization coding", "Platform equipment"),
    CENTRAL_USER("300", "central user", "central user"),
    END_USER("400", "end user", "end user"),
    VIDEO_IMAGE_INFORMATION_SYNTHESIS("500", "Video image information comprehensive application platform", "Platform external server"),
    VIDEO_IMAGE_INFORMATION_OPERATION_AND_MAINTENANCE_MANAGEMENT("501", "Video image information operation and maintenance management platform", "Platform external server"),
    VIDEO_IMAGE_ANALYSIS("502", "Video image analysis system", "Platform external server"),
    VIDEO_IMAGE_INFORMATION_DATABASE("503", "Video image information database", "Platform external server"),
    VIDEO_IMAGE_ANALYSIS_EQUIPMENT("505", "Video image analysis equipment", "Platform external server"),
    ;

    /**
     * No.
     */
    private final String name;

    /**
     * Name
     */
    private String code;

    /**
     * attributed name
     */
    private String ownerName;

    DeviceTypeEnum(String code, String name, String ownerName) {
        this.name = name;
        this.code = code;
        this.ownerName = ownerName;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}
