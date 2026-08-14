package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class VectorTileSource implements Delayed {

    /**
     * Thinning layer data
     */
    private Map<String, byte[]> vectorTileMap = new ConcurrentHashMap<>();

    /**
     * Raw data of thinning
     */
    private List<CommonGBChannel> channelList = new ArrayList<>();

    private String id;

    /**
     * Creation time, deleted after more than 6 hours
     */
    private long time;

    public VectorTileSource() {
        this.time = System.currentTimeMillis();
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(time + 6 * 60 * 60 * 1000 - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }
}
