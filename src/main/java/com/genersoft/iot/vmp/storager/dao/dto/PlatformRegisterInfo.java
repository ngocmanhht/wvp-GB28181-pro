package com.genersoft.iot.vmp.storager.dao.dto;

/**
 * Platform sends registration/Cache this message when logging out
 * @author lin
 */
public class PlatformRegisterInfo {

    /**
     * platformId
     */
    private String platformId;

    /**
     * Whether to register, false to log out
     */
    private boolean register;

    public static PlatformRegisterInfo getInstance(String platformId, boolean register) {
        PlatformRegisterInfo platformRegisterInfo = new PlatformRegisterInfo();
        platformRegisterInfo.setPlatformId(platformId);
        platformRegisterInfo.setRegister(register);
        return platformRegisterInfo;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public boolean isRegister() {
        return register;
    }

    public void setRegister(boolean register) {
        this.register = register;
    }
}
