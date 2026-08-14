package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForFI implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.FI;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * Aperture, 0 means zoom out, 1 means zoom in
     */
    @Getter
    @Setter
    private Integer iris;

    /**
     * Focus 0 near, 1 far
     */
    @Getter
    @Setter
    private Integer focus;

    /**
     * focus speed
     */
    @Getter
    @Setter
    private Integer focusSpeed;

    /**
     * aperture speed
     */
    @Getter
    @Setter
    private Integer irisSpeed;

    @Override
    public String encode() {
        return "";
    }
}
