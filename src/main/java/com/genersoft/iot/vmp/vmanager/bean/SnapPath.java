package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Screenshot address information")
public class SnapPath {

    @Schema(description = "relative address")
    private String path;

    @Schema(description = "absolute address")
    private String absoluteFilePath;

    @Schema(description = "Request address")
    private String url;


    public static SnapPath getInstance(String path, String absoluteFilePath, String url) {
        SnapPath snapPath = new SnapPath();
        snapPath.setPath(path);
        snapPath.setAbsoluteFilePath(absoluteFilePath);
        snapPath.setUrl(url);
        return snapPath;
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAbsoluteFilePath() {
        return absoluteFilePath;
    }

    public void setAbsoluteFilePath(String absoluteFilePath) {
        this.absoluteFilePath = absoluteFilePath;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
