package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "National standard shared channel")
public class ShareGBChannel extends CommonGBChannel{

    @Schema(description = "platformID")
    private int platformId;


}
