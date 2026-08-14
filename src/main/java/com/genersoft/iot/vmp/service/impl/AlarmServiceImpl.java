package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarmNotify;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.alarm.DeviceAlarmEvent;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.service.IAlarmService;
import com.genersoft.iot.vmp.service.bean.Alarm;
import com.genersoft.iot.vmp.service.bean.AlarmType;
import com.genersoft.iot.vmp.storager.dao.AlarmMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements IAlarmService {

    private final AlarmMapper alarmMapper;

    private final UserSetting userSetting;

    private final SipConfig sipConfig;

    private final IDeviceChannelService deviceChannelService;

    private final IGbChannelPlayService gbChannelPlayService;

    private final IGbChannelService gbChannelService;

    // Use Caffeine to cache device channel information to avoid frequent database queries and improve performance
    private Cache<String, DeviceChannel> channelCache = null;

    private final ConcurrentLinkedQueue<Alarm> alarmQueue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() {
        // Initialize the Caffeine cache and set a reasonable expiration time and maximum capacity
        channelCache = Caffeine.newBuilder()
                .maximumSize(userSetting.getAlarmCatchSize()) // fixed capacity
                .build();
    }

    @Async
    @EventListener
    public void onApplicationEvent(DeviceAlarmEvent event) {
        if (channelCache == null || !sipConfig.isAlarm()) {
            return;
        }
        // Process national standard alarm events, convert them into general Alarm objects, cache them, and save them to the database in batches in scheduled tasks.
        if (event.getDeviceAlarmList().isEmpty()) {
            return;
        }
        log.info("Device alarm events received, quantity：{}", event.getDeviceAlarmList().size());
        for (DeviceAlarmNotify notify : event.getDeviceAlarmList()) {
            Alarm alarm = Alarm.buildFromDeviceAlarmNotify(notify);
            if (!userSetting.getAllowedAlarmType().isEmpty() && !userSetting.getAllowedAlarmType().contains(alarm.getAlarmType())) {
                log.debug("Alarm type is not within the allowed range，alarmType：{}，alarmId：{}", alarm.getAlarmType(), alarm.getId());
                continue;
            }
            String key = notify.getDeviceId() + notify.getChannelId();
            DeviceChannel deviceChannel = channelCache.get(key, k -> deviceChannelService.getOneForSource(notify.getDeviceId(), notify.getChannelId()));
            if (deviceChannel == null) {
                continue;
            }
            alarm.setChannelId(deviceChannel.getId());
            // Assign a snapshot path and add the snapshot file later.
            alarm.setSnapPath("snap/alarm_" + notify.getChannelId() + "_" + System.currentTimeMillis() + ".jpg");
            alarmQueue.offer(alarm);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanExpiredAlarms() {
        int keepDays = userSetting.getAlarmKeepDays();
        if (keepDays <= 0) {
            return;
        }
        String endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(System.currentTimeMillis() - (long) keepDays * 24 * 3600 * 1000));
        int count = clearAlarmsByCondition(null, null, endTime);
        log.info("Automatic clearing of expired alarm records is completed and the number of days to be retained is：{}，Clean quantity：{}", keepDays, count);
    }

    @Scheduled(fixedDelay = 500)
    public void executeAlarmQueue() {
        if (alarmQueue.isEmpty()) {
            return;
        }
        List<Alarm> handlerCatchDataList = new ArrayList<>();
        int size = alarmQueue.size();
        for (int i = 0; i < size; i++) {
            Alarm poll = alarmQueue.poll();
            if (poll != null) {
                handlerCatchDataList.add(poll);
            }
        }
        if (handlerCatchDataList.isEmpty()) {
            return;
        }
        // Batch save to database
        int batchSize = 1000;
        for (int i = 0; i < handlerCatchDataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, handlerCatchDataList.size());
            List<Alarm> batchList = handlerCatchDataList.subList(i, end);
            alarmMapper.insertAlarms(batchList);
        }
        // Group according to channel ID to supplement snapshot files
        handlerCatchDataList.forEach(this::getSnapByAlarm);
    }

    @Async
    public void getSnapByAlarm(Alarm alarm) {
        CommonGBChannel channel = gbChannelService.getOne(alarm.getChannelId());
        if (channel == null) {
            log.warn("The channel information associated with the alarm was not found，alarmId：{}，channelId：{}", alarm.getId(), alarm.getChannelId());
            return;
        }
        gbChannelPlayService.getSnap(channel, (code, msg, data) -> {
            if (data == null) {
                return;
            }
            File file = new File(alarm.getSnapPath());
            if(file.exists()) {
                file.delete();
            }
            try {
                FileUtils.writeByteArrayToFile(file, data);
            } catch (Exception e) {
                log.warn("Failed to save alarm snapshot，alarmId：{}，channelId：{}", alarm.getId(), alarm.getChannelId(), e);
            }
        });
    }

    @Override
    public void saveAlarmInfo(Alarm alarm) {

    }

    @Override
    public PageInfo<Alarm> getAlarms(int page, int count, List<AlarmType> alarmType, String beginTime, String endTime) {
        PageHelper.startPage(page, count);
        Long beginTimeLong = null;
        Long endTimeLong = null;
        if (beginTime != null) {
            beginTimeLong = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(beginTime);
        }
        if (endTime != null) {
            endTimeLong = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(endTime);
        }
        List<Alarm> alarmList = alarmMapper.getAlarms(alarmType, beginTimeLong, endTimeLong);
        return new PageInfo<>(alarmList);
    }

    @Override
    public void deleteAlarmInfo(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            alarmMapper.deleteAlarms(ids);
        }
    }

    @Override
    public int clearAlarmsByCondition(List<AlarmType> alarmType, String beginTime, String endTime) {
        Long beginTimeLong = null;
        Long endTimeLong = null;
        if (beginTime != null) {
            beginTimeLong = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(beginTime);
        }
        if (endTime != null) {
            endTimeLong = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(endTime);
        }
        // Cache data before cleaning. After the database is deleted, the related alarm snapshot files also need to be cleaned up.
        alarmMapper.getAlarms(alarmType, beginTimeLong, endTimeLong).forEach(alarm -> {
            String snapPath = alarm.getSnapPath();
            if (snapPath != null) {
                File file = new File(snapPath);
                if (file.exists()) {
                    file.delete();
                }
            }
        });
        return alarmMapper.deleteAlarmsByCondition(alarmType, beginTimeLong, endTimeLong);
    }

    @Override
    public String getAlarmSnapById(Long id) {
        return alarmMapper.getSnapPathById(id);
    }

    @Override
    public StreamInfo getAlarmRecordById(Long id) {
        return null;
    }
}
