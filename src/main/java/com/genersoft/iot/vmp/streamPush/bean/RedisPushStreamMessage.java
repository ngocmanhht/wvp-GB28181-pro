package com.genersoft.iot.vmp.streamPush.bean;

import lombok.Data;

@Data
public class RedisPushStreamMessage {

    private String gbId;
    private String app;
    private String stream;
    private String name;
    private Boolean status;
    // The virtual organization to which the terminal belongs
    private String groupGbId;
    // The virtual organization alias to which the terminal belongs is optional and can be used as an association when synchronizing the organization structure to wvp.
    private String groupAlias;
    // manufacturer
    private String manufacturer;
    // Device model
    private String model;
    // Camera type
    private Integer ptzType;

    public StreamPush buildstreamPush() {
        StreamPush push = new StreamPush();
        push.setApp(app);
        push.setStream(stream);
        push.setGbName(name);
        push.setGbDeviceId(gbId);
        push.setStartOfflinePush(true);
        push.setGbManufacturer(manufacturer);
        push.setGbModel(model);
        push.setGbPtzType(ptzType);
        if (status != null) {
            push.setGbStatus(status?"ON":"OFF");
        }
        push.setEnableBroadcast(0);
        return push;
    }
}
