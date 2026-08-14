package com.genersoft.iot.vmp.streamPush.bean;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StreamPushExcelDto {

    @ExcelProperty("Name")
    private String name;

    @ExcelProperty("Application name")
    private String app;

    @ExcelProperty("flowID")
    private String stream;

    @ExcelProperty("National standardID")
    private String gbDeviceId;

    @ExcelProperty("online status")
    private boolean status;

    @Schema(description = "longitude WGS-84coordinate system")
    private Double longitude;

    @Schema(description = "Latitude WGS-84coordinate system")
    private Double latitude;
}
