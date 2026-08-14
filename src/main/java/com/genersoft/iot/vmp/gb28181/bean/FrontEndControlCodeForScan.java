package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForScan implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.SCAN;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * Preset position instructions: 1 is to start automatic scanning, 2 is to set the automatic scanning left boundary, 3 is to set the automatic scanning right boundary, 4 is to set the automatic scanning speed, 5 is to stop the automatic scanning
     */
    @Getter
    @Setter
    private Integer code;

    /**
     * Auto scan speed
     */
    @Getter
    @Setter
    private Integer scanSpeed;

    /**
     * Scan group number
     */
    @Getter
    @Setter
    private Integer scanId;

    @Override
    public String encode() {
        return "";
    }
}
