package com.genersoft.iot.vmp.gb28181.task.platformStatus;

import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Platform registration task serializable information
 */
@Slf4j
@Data
public class PlatformRegisterTaskInfo{

    private String platformServerId;

    private SipTransactionInfo sipTransactionInfo;

    /**
     * Expiration time, unit: milliseconds
     */
    private long expireTime;
}
