package com.genersoft.iot.vmp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implementation reference fromxiaozhangnomoneyOriginal article，
 * Copyright statement: This article isxiaozhangnomoneyOriginal article, follow CC 4.0 BY-SA Copyright agreement, please attach the original source link and this statement when reprinting
 * Original source link：https://blog.csdn.net/xiaozhangnomoney/article/details/107769147
 */
@Slf4j
public class SystemInfoUtils {

    /**
     * Get cpu information
     * @return
     * @throws InterruptedException
     */
    public static double getCpuInfo() throws InterruptedException {
        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        // sleep1s
        TimeUnit.SECONDS.sleep(1);
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;
        return 1.0-(idle * 1.0 / totalCpu);
    }

    /**
     * Get memory usage
     * @return
     */
    public static double getMemInfo(){
        SystemInfo systemInfo = new SystemInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        //total memory
        long totalByte = memory.getTotal();
        //Remaining
        long acaliableByte = memory.getAvailable();
        return (totalByte-acaliableByte)*1.0/totalByte;
    }

    /**
     * Get network uploads and downloads
     * @return
     */
    public static Map<String,Double> getNetworkInterfaces() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        List<NetworkIF> beforeRecvNetworkIFs = hal.getNetworkIFs();
        NetworkIF beforeBet= beforeRecvNetworkIFs.get(beforeRecvNetworkIFs.size() - 1);
        long beforeRecv = beforeBet.getBytesRecv();
        long beforeSend = beforeBet.getBytesSent();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("[Thread sleep failed] : {}", e.getMessage());
        }
        List<NetworkIF> afterNetworkIFs = hal.getNetworkIFs();
        NetworkIF afterNet = afterNetworkIFs.get(afterNetworkIFs.size() - 1);

        HashMap<String, Double> map = new HashMap<>();
        // speed unit: Mbps
        map.put("in",formatUnits(afterNet.getBytesRecv()-beforeRecv, 1048576L));
        map.put("out",formatUnits(afterNet.getBytesSent()-beforeSend, 1048576L));
        return map;
    }

    /**
     * Get the total bandwidth value
     * @return
     */
    public static long getNetworkTotal() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        List<NetworkIF> recvNetworkIFs = hal.getNetworkIFs();
        NetworkIF networkIF= recvNetworkIFs.get(recvNetworkIFs.size() - 1);

        return networkIF.getSpeed()/1048576L/8L;
    }

    public static double formatUnits(long value, long prefix) {
        return (double)value / (double)prefix;
    }

    /**
     * Get the number of processes
     * @return
     */
    public static int getProcessesCount(){
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();

        int processCount = os.getProcessCount();
        return processCount;
    }

    public static List<Map<String, Object>> getDiskInfo() {
        List<Map<String, Object>> result = new ArrayList<>();

        String osName = System.getProperty("os.name");
        List<String> pathArray = new ArrayList<>();
        if (osName.startsWith("Mac OS")) {
            // apple
            pathArray.add("/");
        } else if (osName.startsWith("Windows")) {
            // windows
            pathArray.add("C:");
        } else {
            pathArray.add("/");
            pathArray.add("/home");
        }
        for (String path : pathArray) {
            Map<String, Object> infoMap = new HashMap<>();
            infoMap.put("path", path);
            File partitionFile = new File(path);
            // unit： GB
            infoMap.put("use", (partitionFile.getTotalSpace() - partitionFile.getFreeSpace())/1024/1024/1024D);
            infoMap.put("free", partitionFile.getFreeSpace()/1024/1024/1024D);
            result.add(infoMap);
        }
        return result;
    }

    public static String getHardwareId(){
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        // CPU ID
        String cpuId = hardware.getProcessor().getProcessorIdentifier().getProcessorID();
        // Motherboard serial number
        String serialNumber = hardware.getComputerSystem().getSerialNumber();

        return DigestUtils.md5DigestAsHex(
                (
                        DigestUtils.md5DigestAsHex(cpuId.getBytes(StandardCharsets.UTF_8)) +
                        DigestUtils.md5DigestAsHex(serialNumber.getBytes(StandardCharsets.UTF_8))
                ).getBytes(StandardCharsets.UTF_8));
    }
}
