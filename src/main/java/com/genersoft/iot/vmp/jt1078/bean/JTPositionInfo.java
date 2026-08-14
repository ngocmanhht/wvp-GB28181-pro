package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "location information")
public class JTPositionInfo {

    /**
     * Basic location information
     */
    @Schema(description = "Basic location information")
    private JTPositionBaseInfo base;

    /**
     * Basic location information
     */
    @Schema(description = "Location extensions")
    private JTPositionAdditionalInfo additional;

    public void setBase(JTPositionBaseInfo base) {
        this.base = base;
    }

    public void setAdditional(JTPositionAdditionalInfo additional) {
        this.additional = additional;
    }
}
