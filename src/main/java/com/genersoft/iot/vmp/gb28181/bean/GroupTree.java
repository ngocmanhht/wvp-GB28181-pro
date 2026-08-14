package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * business grouping
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "business grouping tree")
public class GroupTree extends Group{

    @Schema(description = "tree nodeID")
    private String treeId;

    @Schema(description = "Whether there are child nodes")
    private boolean isLeaf;

    @Schema(description = "Type, Administrative Division:0 Cameras: 1")
    private int type;

    @Schema(description = "online status")
    private String status;

}
