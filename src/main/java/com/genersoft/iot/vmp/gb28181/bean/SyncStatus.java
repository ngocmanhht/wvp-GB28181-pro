package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

/**
 * Camera sync status
 * @author lin
 */
@Data
@Schema(description = "Camera sync status")
public class SyncStatus {

    @Schema(description = "total")
    private Integer total;

    @Schema(description = "How many updates are currently")
    private Integer current;

    @Schema(description = "Error description")
    private String errorMsg;

    @Schema(description = "Is syncing")
    private Boolean syncIng;

    @Schema(description = "time")
    private Instant time;

}
