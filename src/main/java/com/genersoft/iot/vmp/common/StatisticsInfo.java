package com.genersoft.iot.vmp.common;

import lombok.Data;

/**
 * Statistics
 */
@Data
public class StatisticsInfo {

    private long id;

    /**
     * ID
     */
    private String deviceId;

    /**
     * branch
     */
    private String branch;

    /**
     * gitcommit versionID
     */
    private String gitCommitId;

    /**
     * gitaddress
     */
    private String gitUrl;

    /**
     * build version
     */
    private String version;

    /**
     * Operating system name
     */
    private String osName;

    /**
     * Is it a docker environment?
     */
    private Boolean docker;

    /**
     * Architecture
     */
    private String arch;

    /**
     * jdkversion
     */
    private String jdkVersion;

    /**
     * redisversion
     */
    private String redisVersion;

    /**
     * sqlDatabase version
     */
    private String sqlVersion;

    /**
     * sqlDatabase type， mysql/postgresql/Jin Cang et al.
     */
    private String sqlType;

    /**
     * creation time
     */
    private String time;

    @Override
    public String toString() {
        return "StatisticsInfo{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", branch='" + branch + '\'' +
                ", gitCommitId='" + gitCommitId + '\'' +
                ", gitUrl='" + gitUrl + '\'' +
                ", version='" + version + '\'' +
                ", osName='" + osName + '\'' +
                ", docker=" + docker +
                ", arch='" + arch + '\'' +
                ", jdkVersion='" + jdkVersion + '\'' +
                ", redisVersion='" + redisVersion + '\'' +
                ", sqlVersion='" + sqlVersion + '\'' +
                ", sqlType='" + sqlType + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
