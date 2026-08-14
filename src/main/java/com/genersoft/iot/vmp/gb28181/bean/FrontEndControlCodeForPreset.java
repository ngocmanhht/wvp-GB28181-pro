package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForPreset implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.PRESET;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * Preset position instructions: 1 is to set the preset position, 2 is to call the preset position, 3 is to delete the preset position
     */
    @Getter
    @Setter
    private Integer code;

    /**
     * Preset number
     */
    @Getter
    @Setter
    private Integer presetId;

    /**
     * Preset position name
     */
    @Getter
    @Setter
    private String presetName;


    @Override
    public String encode() {
        return "";
    }
}
