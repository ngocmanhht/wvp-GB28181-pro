package com.genersoft.iot.vmp.common;

import com.alibaba.fastjson2.annotation.JSONField;

public class VersionPo {
    /**
     * gitfull version number
     */
    @JSONField(name="GIT_Revision")
    private String GIT_Revision;
    /**
     * mavenversion
     */
    @JSONField(name = "Create_By")
    private String Create_By;
    /**
     * gitbranches
     */
    @JSONField(name = "GIT_BRANCH")
    private String GIT_BRANCH;
    /**
     * gitofurl
     */
    @JSONField(name = "GIT_URL")
    private String GIT_URL;
    /**
     * Build date
     */
    @JSONField(name = "BUILD_DATE")
    private String BUILD_DATE;
    /**
     * Build date
     */
    @JSONField(name = "GIT_DATE")
    private String GIT_DATE;
    /**
     * Project name used with pom
     */
    @JSONField(name = "artifactId")
    private String artifactId;
    /**
     * gitlocal version number
     */
    @JSONField(name = "GIT_Revision_SHORT")
    private String GIT_Revision_SHORT;
    /**
     * The project version is such as 2.0.1.0, used with pom
     */
    @JSONField(name = "version")
    private String version;
    /**
     * Subsystem name
     */
    @JSONField(name = "project")
    private String project;
    /**
     * jdkversion
     */
    @JSONField(name="Build_Jdk")
    private String Build_Jdk;

    public void setGIT_Revision(String GIT_Revision) {
        this.GIT_Revision = GIT_Revision;
    }

    public void setCreate_By(String create_By) {
        Create_By = create_By;
    }

    public void setGIT_BRANCH(String GIT_BRANCH) {
        this.GIT_BRANCH = GIT_BRANCH;
    }

    public void setGIT_URL(String GIT_URL) {
        this.GIT_URL = GIT_URL;
    }

    public void setBUILD_DATE(String BUILD_DATE) {
        this.BUILD_DATE = BUILD_DATE;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setGIT_Revision_SHORT(String GIT_Revision_SHORT) {
        this.GIT_Revision_SHORT = GIT_Revision_SHORT;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public void setBuild_Jdk(String build_Jdk) {
        Build_Jdk = build_Jdk;
    }

    public String getGIT_Revision() {
        return GIT_Revision;
    }

    public String getCreate_By() {
        return Create_By;
    }

    public String getGIT_BRANCH() {
        return GIT_BRANCH;
    }

    public String getGIT_URL() {
        return GIT_URL;
    }

    public String getBUILD_DATE() {
        return BUILD_DATE;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGIT_Revision_SHORT() {
        return GIT_Revision_SHORT;
    }

    public String getVersion() {
        return version;
    }

    public String getProject() {
        return project;
    }

    public String getBuild_Jdk() {
        return Build_Jdk;
    }

    public String getGIT_DATE() {
        return GIT_DATE;
    }

    public void setGIT_DATE(String GIT_DATE) {
        this.GIT_DATE = GIT_DATE;
    }
}
