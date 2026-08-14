package com.genersoft.iot.vmp.vmanager.bean;

public enum PlayTypeEnum {

    PLAY("0", "Live broadcast"),
    PLAY_BACK("1", "Playback");

    private String value;
    private String name;

    PlayTypeEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
