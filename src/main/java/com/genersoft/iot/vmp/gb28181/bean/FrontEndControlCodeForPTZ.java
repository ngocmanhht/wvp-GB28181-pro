package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForPTZ implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.PTZ;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * Lens zoom, 0 means zoom out, 1 means zoom in
     */
    @Getter
    @Setter
    private Integer zoom;

    /**
     * PTZ vertical direction control 0 means up, 1 means down
     */
    @Getter
    @Setter
    private Integer tilt;

    /**
     * PTZ horizontal direction control: 0 is left, 1 is right
     */
    @Getter
    @Setter
    private Integer pan;

    /**
     * Horizontal control speed relative value
     */
    @Getter
    @Setter
    private Integer panSpeed;

    /**
     * Vertical control speed relative value
     */
    @Getter
    @Setter
    private Integer tiltSpeed;

    /**
     * Relative value of zoom control speed
     */
    @Getter
    @Setter
    private Integer zoomSpeed;

    @Override
    public String encode() {
        return "";
    }
}
