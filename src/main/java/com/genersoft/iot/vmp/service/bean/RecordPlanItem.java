package com.genersoft.iot.vmp.service.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Recording schedule items")
public class RecordPlanItem {

    @Schema(description = "Plan item databaseID")
    private int id;

    @Schema(description = "The sequence number of the planned start time, starting from 0 o'clock, increasing every half hour1")
    private Integer start;

    @Schema(description = "The serial number of the planned end time, starting from 0 o'clock, increasing every half hour1")
    private Integer stop;

    @Schema(description = "Planned days of the week to be executed")
    private Integer weekDay;

    @Schema(description = "ProjectID")
    private Integer planId;

}
