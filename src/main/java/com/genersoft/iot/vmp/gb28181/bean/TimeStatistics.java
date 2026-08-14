package com.genersoft.iot.vmp.gb28181.bean;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "time statistics")
public class TimeStatistics {

    @Schema(description = "time")
    private String time;

    @Schema(description = "time difference")
    private Long timeDiff;
}
