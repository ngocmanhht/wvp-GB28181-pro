package com.genersoft.iot.vmp.gb28181.task.platformStatus;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Platform registration tasks
 */
@Slf4j
public class PlatformRegisterTask implements Delayed {

    @Getter
    private String platformServerId;

    /**
     * timeout(Unit: millisecond)
     */
    @Getter
    @Setter
    private long delayTime;

    @Getter
    private SipTransactionInfo sipTransactionInfo;

    /**
     * Expiration callback
     */
    @Getter
    private CommonCallback<String> callback;


    public PlatformRegisterTask(String platformServerId, long delayTime, SipTransactionInfo sipTransactionInfo, CommonCallback<String> callback) {
        this.platformServerId = platformServerId;
        this.delayTime = System.currentTimeMillis() + delayTime;
        this.callback = callback;
        this.sipTransactionInfo = sipTransactionInfo;
    }

    public void expired() {
        if (callback == null) {
            log.info("[Platform registration expires] Expiration processing callback not found, platform superior number： {}", platformServerId);
            return;
        }
        getCallback().run(platformServerId);
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(delayTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }

    public PlatformRegisterTaskInfo getInfo() {
        PlatformRegisterTaskInfo taskInfo = new PlatformRegisterTaskInfo();
        taskInfo.setPlatformServerId(platformServerId);
        taskInfo.setSipTransactionInfo(sipTransactionInfo);
        return taskInfo;
    }
}
