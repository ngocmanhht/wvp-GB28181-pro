package com.genersoft.iot.vmp.gb28181.task.platformStatus;

import com.genersoft.iot.vmp.gb28181.bean.PlatformKeepaliveCallback;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Platform heartbeat task
 */
@Slf4j
public class PlatformKeepaliveTask implements Delayed {

    @Getter
    private String platformServerId;

    /**
     * timeout(Unit: millisecond)
     */
    @Getter
    @Setter
    private long delayTime;

    /**
     * Expiration callback
     */
    @Getter
    private PlatformKeepaliveCallback callback;

    /**
     * Number of heartbeat sending failures
     */
    @Getter
    @Setter
    private int failCount;

    public PlatformKeepaliveTask(String platformServerId, long delayTime, PlatformKeepaliveCallback callback) {
        this.platformServerId = platformServerId;
        this.delayTime = System.currentTimeMillis() + delayTime;
        this.callback = callback;
    }

    public void expired() {
        if (callback == null) {
            log.info("[Platform heartbeat expires] Expiration processing callback not found, platform superior number： {}", platformServerId);
            return;
        }
        getCallback().run(platformServerId, failCount);
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(delayTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }
}
