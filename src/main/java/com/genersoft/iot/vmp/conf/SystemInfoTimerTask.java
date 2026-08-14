package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.SystemInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Get system information writtenredis
 */
@Slf4j
@Component
public class SystemInfoTimerTask {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Scheduled(fixedRate = 2000)   //Executed every 1 second
    public void execute(){
        try {
            double cpuInfo = SystemInfoUtils.getCpuInfo();
            redisCatchStorage.addCpuInfo(cpuInfo);
            double memInfo = SystemInfoUtils.getMemInfo();
            redisCatchStorage.addMemInfo(memInfo);
            Map<String, Double> networkInterfaces = SystemInfoUtils.getNetworkInterfaces();
            redisCatchStorage.addNetInfo(networkInterfaces);
            List<Map<String, Object>> diskInfo =SystemInfoUtils.getDiskInfo();
            redisCatchStorage.addDiskInfo(diskInfo);
        } catch (InterruptedException e) {
            log.error("[Failed to obtain system information] {}", e.getMessage());
        }

    }


}
