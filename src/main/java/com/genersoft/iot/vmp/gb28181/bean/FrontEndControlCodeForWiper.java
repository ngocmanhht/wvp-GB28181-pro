package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForWiper implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.AUXILIARY;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * Auxiliary switch control instructions: 1 is on, 2 is off
     */
    @Getter
    @Setter
    private Integer code;

    @Override
    public String encode() {
        return "";
    }
}
