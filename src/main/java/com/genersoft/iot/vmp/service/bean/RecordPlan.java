package com.genersoft.iot.vmp.service.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Recording plan")
public class RecordPlan {

    @Schema(description = "planning databaseID")
    private int id;

    @Schema(description = "Plan name")
    private String name;

    @Schema(description = "Number of planned associated channels")
    private int channelCount;

    @Schema(description = "Whether to enable scheduled screenshots")
    private Boolean snap;

    @Schema(description = "creation time")
    private String createTime;

    @Schema(description = "Update time")
    private String updateTime;

    @Schema(description = "Plan content")
    private List<RecordPlanItem> planItemList;
}
