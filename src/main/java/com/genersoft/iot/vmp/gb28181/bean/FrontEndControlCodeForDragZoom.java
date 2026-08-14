package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Getter;
import lombok.Setter;

public class FrontEndControlCodeForDragZoom implements  IFrontEndControlCode {

    private final FrontEndControlType type =  FrontEndControlType.DRAG_ZOOM;

    @Override
    public FrontEndControlType getType() {
        return type;
    }

    /**
     * Auxiliary switch control instructions: 1 is zoomIn to zoom in, 2 is zoomOut to zoom out.
     */
    @Getter
    @Setter
    private Integer code;

    /**
     * Play window length pixel value(Required)
     */
    @Getter
    @Setter
    protected Integer length;

    /**
     * Play window length pixel value(Required)
     */
    @Getter
    @Setter
    protected Integer width;

    /**
     * The horizontal axis coordinate pixel value of the center of the pull box(Required)
     */
    @Getter
    @Setter
    protected Integer midPointX;

    /**
     * The vertical axis coordinate pixel value of the center of the pull box(Required)
     */
    @Getter
    @Setter
    protected Integer midPointY;

    /**
     * Frame length in pixels(Required)
     */
    @Getter
    @Setter
    protected Integer lengthX;

    /**
     * Pull box width pixel value(Required)
     */
    @Getter
    @Setter
    protected Integer lengthY;


    @Override
    public String encode() {
        return "";
    }
}
