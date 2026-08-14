package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.MessageElement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Scroll down to enlarge/Narrow control parameters")
public class DragZoomParam {

    @MessageElement("Length")
    @Schema(description = "Play window length pixel value(Required)")
    protected Integer length;

    @MessageElement("Width")
    @Schema(description = "Play window width pixel value(Required)")
    protected Integer width;

    @MessageElement("MidPointX")
    @Schema(description = "The horizontal axis coordinate pixel value of the center of the pull box(Required)")
    protected Integer midPointX;

    @MessageElement("MidPointY")
    @Schema(description = "The vertical axis coordinate pixel value of the center of the pull box(Required)")
    protected Integer midPointY;

    @MessageElement("LengthX")
    @Schema(description = "Frame length in pixels(Required)")
    protected Integer lengthX;

    @MessageElement("LengthY")
    @Schema(description = "Pull box width pixel value(Required)")
    protected Integer lengthY;
}
