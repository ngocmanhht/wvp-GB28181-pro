package com.genersoft.iot.vmp.web.custom.bean;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CameraStreamContent extends StreamContent {

    public CameraStreamContent(StreamInfo streamInfo) {
        super(streamInfo);
    }

    private String name;

    // 0Immovable, 1 movable
    private Integer controlType;


}
