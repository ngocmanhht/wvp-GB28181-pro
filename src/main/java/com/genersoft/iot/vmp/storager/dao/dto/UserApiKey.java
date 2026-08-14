package com.genersoft.iot.vmp.storager.dao.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * User information
 */
@Schema(description = "UserApiKey information")
public class UserApiKey implements Serializable {

    /**
     * Id
     */
    @Schema(description = "Id")
    private int id;

    /**
     * UserId
     */
    @Schema(description = "UserId")
    private int userId;

    /**
     * Application name
     */
    @Schema(description = "Application name")
    private String app;

    /**
     * ApiKey
     */
    @Schema(description = "ApiKey")
    private String apiKey;

    /**
     * Expiration time（null=never expires）
     */
    @Schema(description = "Expiration time（null=never expires）")
    private long expiredAt;

    /**
     * Remarks
     */
    @Schema(description = "Remarks")
    private String remark;

    /**
     * Whether to enable
     */
    @Schema(description = "Whether to enable")
    private boolean enable;

    /**
     * creation time
     */
    @Schema(description = "creation time")
    private String createTime;

    /**
     * Update time
     */
    @Schema(description = "Update time")
    private String updateTime;

    /**
     * Username
     */
    private String username;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public long getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(long expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
