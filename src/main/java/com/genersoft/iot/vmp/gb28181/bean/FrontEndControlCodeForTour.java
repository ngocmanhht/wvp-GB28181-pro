package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForTour implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.TOUR;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * Cruise command: 1 is to add a cruise point, 2 is to delete a cruise point, 3 is to set the cruise speed, 4 is to set the cruise stay time, 5 is to start cruising, 6 is to stop cruising.
     */
    @Getter
    @Setter
    private Integer code;

    /**
     * cruising point
     */
    @Getter
    @Setter
    private Integer tourId;

    /**
     * Cruise stop time
     */
    @Getter
    @Setter
    private Integer tourTime;

    /**
     * cruising speed
     */
    @Getter
    @Setter
    private Integer tourSpeed;

    /**
     * Preset number
     */
    @Getter
    @Setter
    private Integer presetId;

    @Override
    public String encode() {
        return "";
    }
}
