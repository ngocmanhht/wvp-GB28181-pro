package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * National standard encoding object
 */
@Data
@Schema(description = "National standard encoding object")
public class GbCode {

    @Schema(description = "The center code is determined by the administrative division code where the monitoring center is located and conforms toGB/T2260—2007requirements")
    private String centerCode;

    @Schema(description = "Industry code")
    private String industryCode;

    @Schema(description = "type encoding")
    private String typeCode;

    @Schema(description = "Network ID")
    private String netCode;

    @Schema(description = "serial number")
    private String sn;

    /**
     * Analyze the national standard number
     */
    public static GbCode decode(String code){
        if (code == null || code.trim().length() != 20 || !code.matches("\\d{20}")) {
            return null;
        }
        code = code.trim();
        GbCode gbCode = new GbCode();
        gbCode.setCenterCode(code.substring(0, 8));
        gbCode.setIndustryCode(code.substring(8, 10));
        gbCode.setTypeCode(code.substring(10, 13));
        gbCode.setNetCode(code.substring(13, 14));
        gbCode.setSn(code.substring(14));
        return gbCode;
    }

    public String ecode(){
        return centerCode + industryCode + typeCode + netCode + sn;
    }
}
