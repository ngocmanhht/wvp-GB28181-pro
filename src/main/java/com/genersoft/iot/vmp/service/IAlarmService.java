package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.bean.Alarm;
import com.genersoft.iot.vmp.service.bean.AlarmType;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface IAlarmService {
    // Save alarm information
    void saveAlarmInfo(Alarm alarm);

    // Get alarm information by page
    PageInfo<Alarm> getAlarms(int page, int size, List<AlarmType> alarmType, String beginTime, String endTime);

    // Delete alarm information
    void deleteAlarmInfo(List<Long> ids);

    // Clear alarm information according to filter conditions
    int clearAlarmsByCondition(List<AlarmType> alarmType, String beginTime, String endTime);

    // Get alarm snapshot based on ID
    String getAlarmSnapById(Long id);

    // Get alarm video based on ID
    StreamInfo getAlarmRecordById(Long id);
}
