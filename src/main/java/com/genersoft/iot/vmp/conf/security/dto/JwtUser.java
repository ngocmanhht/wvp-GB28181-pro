package com.genersoft.iot.vmp.conf.security.dto;

public class JwtUser {

    public enum TokenStatus{
        /**
         * normal usage status
         */
        NORMAL,
        /**
         * invalid after expiration
         */
        EXPIRED,
        /**
         * Expires soon
         */
        EXPIRING_SOON,
        /**
         * Abnormal
         */
        EXCEPTION
    }

    private int userId;
    private String userName;

    private String password;

    private int roleId;

    private TokenStatus status;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public TokenStatus getStatus() {
        return status;
    }

    public void setStatus(TokenStatus status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }
}
