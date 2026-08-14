package com.genersoft.iot.vmp.vmanager.recordPlan.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Recording plan-add/Edit parameters")
public class RecordPlanParam {

    @Schema(description = "associated channelID")
    private List<Integer> channelIds;

    @Schema(description = "The associated device ID will be associated with this recording plan for all channels under the device. This item will not take effect if the channelId exists.，")
    private List<Integer> deviceDbIds;

    @Schema(description = "All related/Unlink all")
    private Boolean allLink;

    @Schema(description = "Recording plan ID. If the ID is empty, the associated plan will be deleted.")
    private Integer planId;
}
