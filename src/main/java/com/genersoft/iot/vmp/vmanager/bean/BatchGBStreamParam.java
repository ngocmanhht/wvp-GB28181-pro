package com.genersoft.iot.vmp.vmanager.bean;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * @author lin
 */
@Schema(description = "Multiple push information")
public class BatchGBStreamParam {
    @Schema(description = "Push information list")
    private List<GbStream> gbStreams;

    public List<GbStream> getGbStreams() {
        return gbStreams;
    }

    public void setGbStreams(List<GbStream> gbStreams) {
        this.gbStreams = gbStreams;
    }
}
