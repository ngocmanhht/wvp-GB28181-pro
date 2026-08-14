package com.genersoft.iot.vmp.web.custom.conf;

import java.util.HashMap;
import java.util.Map;

public enum SyTokenManager {
    INSTANCE;

    /**
     * Ordinary user app Key and secret
     */
    public final Map<String, String> appMap = new HashMap<>();


    /**
     * Administrator onlytoken
     */
    public String adminToken;

    /**
     * sm4key
     */
    public String sm4Key;

    /**
     * Interface validity period, in minutes
     */
    public Long expires;


}
