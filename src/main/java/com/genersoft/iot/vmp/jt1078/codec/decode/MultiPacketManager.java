package com.genersoft.iot.vmp.jt1078.codec.decode;

import com.genersoft.iot.vmp.jt1078.proc.Header;
import io.netty.buffer.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public enum MultiPacketManager {
    INSTANCE;
    // caching of messages
    private final Map<String, CompositeByteBuf> packetMap = new ConcurrentHashMap<>();
    private final Map<String, Long> packetTimeMap = new ConcurrentHashMap<>();

    MultiPacketManager() {
        startLister();
    }

    /**
     * Add subpackages to be merged. If the subpackages are accepted, the complete data packet will be returned.
     */
    public ByteBuf add(Header header, Integer count, ByteBuf byteBuf) {
        String key = header.getMsgId() + "/" + header.getPhoneNumber();
        CompositeByteBuf compositeBuf = packetMap.computeIfAbsent(key, k -> new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, false, count));
//        compositeBuf.addComponent(true, byteBuf.readSlice(byteBuf.readableBytes()));
        compositeBuf.addComponent(true, byteBuf);
        packetTimeMap.put(key, System.currentTimeMillis());
        if (count == compositeBuf.numComponents()) {
            packetMap.remove(key);
            packetTimeMap.remove(key);
            compositeBuf.retain();
            return compositeBuf;
        }
        return null;
    }

    private void startLister(){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long expireTime = System.currentTimeMillis() - 20 * 1000;
                if (!packetTimeMap.isEmpty()) {
                    for (String key : packetTimeMap.keySet()) {
                        if (packetTimeMap.get(key) < expireTime) {
                            log.info("Subpackage message timeout key: {}", key);
                            packetTimeMap.remove(key);
                            packetMap.remove(key);
                        }
                    }
                }
            }
        }, 2000L, 2000L);
    }
}
