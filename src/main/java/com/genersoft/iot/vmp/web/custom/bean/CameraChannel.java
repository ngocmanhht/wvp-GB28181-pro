package com.genersoft.iot.vmp.web.custom.bean;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Schema(description = "Camera information")
public class CameraChannel extends CommonGBChannel {

    @Schema(description = "Camera equipment national standard number")
    private String deviceCode;


    @Schema(description = "icon path")
    private String icon;

    /**
     * Group alias
     */
    @Schema(description = "Alias of the organizational structure to which it belongs")
    private String groupAlias;

    /**
     * Alias of the business group to which the group belongs
     */
    @Schema(description = "Alias of the business group to which it belongs")
    private String topGroupGAlias;
}
