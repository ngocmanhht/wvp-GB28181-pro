package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * area
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "area tree")
public class RegionTree extends Region {

    @Schema(description = "tree nodeID")
    private String treeId;

    @Schema(description = "Whether there are child nodes")
    private boolean isLeaf;

    @Schema(description = "Type, Administrative Division:0 Cameras: 1")
    private int type;

    @Schema(description = "online status")
    private String status;
}
