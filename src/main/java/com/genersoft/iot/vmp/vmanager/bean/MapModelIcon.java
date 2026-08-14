package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MapModelIcon {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Alias")
    private String alias;

    @Schema(description = "path")
    private String path;


    public static MapModelIcon getInstance(String name, String alias, String path) {
        MapModelIcon mapModelIcon = new MapModelIcon();
        mapModelIcon.setAlias(alias);
        mapModelIcon.setName(name);
        mapModelIcon.setPath(path);
        return mapModelIcon;
    }
}
