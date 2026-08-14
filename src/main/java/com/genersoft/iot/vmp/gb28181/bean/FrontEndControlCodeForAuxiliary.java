package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForAuxiliary implements  IFrontEndControlCode {

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

    /**
     * Auxiliary switch number
     */
    @Getter
    @Setter
    private Integer auxiliaryId;

    @Override
    public String encode() {
        return "";
    }
}
