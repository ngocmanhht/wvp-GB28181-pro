package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IRecordPlanService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.RecordPlan;
import com.genersoft.iot.vmp.service.bean.RecordPlanItem;
import com.genersoft.iot.vmp.storager.dao.RecordPlanMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RecordPlanServiceImpl implements IRecordPlanService {

    @Autowired
    private RecordPlanMapper recordPlanMapper;

    @Autowired
    private CommonGBChannelMapper channelMapper;

    @Autowired
    private IGbChannelPlayService channelPlayService;

    @Autowired
    private IMediaServerService mediaServerService;



    /**
     * Stream departure processing
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        // The stream is disconnected, check whether it is still in the recording state, if so, continue recording.
        Integer channelId = recording(event.getApp(), event.getStream());
        if(channelId == null) {
            return;
        }
        // Pull up again
        CommonGBChannel channel = channelMapper.queryById(channelId);
        if (channel == null) {
            log.warn("[Recording plan] When the stream that needs to be recorded is pulled up when the stream is leaving, it is found that the channel does not exist., id: {}", channelId);
            return;
        }
        // Turn on on-demand,
        channelPlayService.play(channel, null, true, ((code, msg, streamInfo) -> {
            if (code == InviteErrorCode.SUCCESS.getCode() && streamInfo != null) {
                log.info("[Video] When the stream leaves, pull up the stream that needs to be recorded, open successfully, channelID: {}", channel.getGbId());
                recordStreamMap.put(channel.getGbId(), streamInfo);
            } else {
                recordStreamMap.remove(channelId);
                log.info("[Video] Pull up the stream that needs to be recorded when the stream leaves. Failed to start. Try again after ten minutes. ChannelID: {}", channel.getGbId());
            }
        }));
    }

    Map<Integer, StreamInfo> recordStreamMap = new HashMap<>();

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void execution() {
        // Query the channel that needs to be recorded nowId
        List<Integer> startChannelIdList = queryCurrentChannelRecord();

        if (startChannelIdList == null || startChannelIdList.isEmpty()) {
            // There is currently no recording task. If there is an old recording task, remove it.
            if(!recordStreamMap.isEmpty()) {
                Set<Integer> recordStreamSet = new HashSet<>(recordStreamMap.keySet());
                stopStreams(recordStreamSet, recordStreamMap);
                recordStreamMap.clear();
            }
        }else {
            // There is currently a recording task. Get the content that is currently recording but does not exist in the current recording list and stop it.; Get the recording that is not currently being recorded but exists in the list that currently needs to be recorded and open it..
            Set<Integer> recordStreamSet = new HashSet<>(recordStreamMap.keySet());
            startChannelIdList.forEach(recordStreamSet::remove);
            if (!recordStreamSet.isEmpty()) {
                // Stop the content that is being recorded but does not exist in the current recording list.;
                stopStreams(recordStreamSet, recordStreamMap);
            }

            // RemovestartChannelIdListThe parts that are already being recorded, the rest need to be added newly.(Open the recording that is not currently being recorded but exists in the list that currently needs to be recorded.)
            recordStreamMap.keySet().forEach(startChannelIdList::remove);
            if (!startChannelIdList.isEmpty()) {
                // Get all associated channels
                List<CommonGBChannel> channelList = channelMapper.queryByIds(startChannelIdList);
                if (!channelList.isEmpty()) {
                    // Check whether recording has been turned on, if not, turn on recording
                    for (CommonGBChannel channel : channelList) {
                        // Turn on on-demand,
                        channelPlayService.play(channel, null, true, ((code, msg, streamInfo) -> {
                            if (code == InviteErrorCode.SUCCESS.getCode() && streamInfo != null) {
                                log.info("[Video] Opened successfully, channelID: {}", channel.getGbId());
                                recordStreamMap.put(channel.getGbId(), streamInfo);
                            } else {
                                log.info("[Video] Failed to open, try again in ten minutes, channelID: {}", channel.getGbId());
                            }
                        }));
                    }
                } else {
                    log.error("[Recording plan] Data anomaly, these associated channels no longer exist: {}", Joiner.on(",").join(startChannelIdList));
                }
            }
        }
    }

    /**
     * Get the list of channel IDs that should be recorded in the current time period
     */
    private List<Integer> queryCurrentChannelRecord(){
        // Get the serial number of the current time within a week, starting from the 30th minute stored in the database, 0-47, including beginning and end
        LocalDateTime now = LocalDateTime.now();
        int week = now.getDayOfWeek().getValue();
        int index = now.getHour() * 60 + now.getMinute();

        // Query the channel that needs to be recorded nowId
        return recordPlanMapper.queryRecordIng(week, index);
    }

    private void stopStreams(Collection<Integer> channelIds, Map<Integer, StreamInfo> recordStreamMap) {
        for (Integer channelId : channelIds) {
            try {
                StreamInfo streamInfo = recordStreamMap.get(channelId);
                if (streamInfo == null) {
                    continue;
                }
                // Check if anyone is watching. If there is, no processing will be done, waiting for subsequent natural processing. If no one is watching, close the stream.
                MediaInfo mediaInfo = mediaServerService.getMediaInfo(streamInfo.getMediaServer(), streamInfo.getApp(), streamInfo.getStream());
                if (mediaInfo.getReaderCount() == null ||  mediaInfo.getReaderCount() == 0) {
                    mediaServerService.closeStreams(streamInfo.getMediaServer(), streamInfo.getApp(), streamInfo.getStream());
                    log.info("[Recording plan] stop channelID: {}", channelId);
                }
            }catch (Exception e) {
                log.error("[Recording plan] Exception while stopping", e);
            }finally {
                recordStreamMap.remove(channelId);
            }
        }
    }

    @Override
    public Integer recording(String app, String stream) {
        for (Integer channelId : recordStreamMap.keySet()) {
            StreamInfo streamInfo = recordStreamMap.get(channelId);
            if (streamInfo != null && streamInfo.getApp().equals(app) && streamInfo.getStream().equals(stream)) {
                return channelId;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public void add(RecordPlan plan) {
        plan.setCreateTime(DateUtil.getNow());
        plan.setUpdateTime(DateUtil.getNow());
        recordPlanMapper.add(plan);
        if (plan.getId() > 0 && !plan.getPlanItemList().isEmpty()) {
            for (RecordPlanItem recordPlanItem : plan.getPlanItemList()) {
                recordPlanItem.setPlanId(plan.getId());
            }
            recordPlanMapper.batchAddItem(plan.getId(), plan.getPlanItemList());
        }
        // TODO  Update recording queue
    }

    @Override
    public RecordPlan get(Integer planId) {
        RecordPlan recordPlan = recordPlanMapper.get(planId);
        if (recordPlan == null) {
            return null;
        }
        List<RecordPlanItem> recordPlanItemList = recordPlanMapper.getItemList(planId);
        if (!recordPlanItemList.isEmpty()) {
            recordPlan.setPlanItemList(recordPlanItemList);
        }
        return recordPlan;
    }

    @Override
    @Transactional
    public void update(RecordPlan plan) {
        plan.setUpdateTime(DateUtil.getNow());
        recordPlanMapper.update(plan);
        recordPlanMapper.cleanItems(plan.getId());
        if (plan.getPlanItemList() != null && !plan.getPlanItemList().isEmpty()){
            List<RecordPlanItem> planItemList = new ArrayList<>();
            for (RecordPlanItem recordPlanItem : plan.getPlanItemList()) {
                if (recordPlanItem.getStart() == null || recordPlanItem.getStop() == null || recordPlanItem.getWeekDay() == null){
                    continue;
                }
                if (recordPlanItem.getPlanId() == null) {
                    recordPlanItem.setPlanId(plan.getId());
                }
                planItemList.add(recordPlanItem);
            }
            if(!planItemList.isEmpty()) {
                recordPlanMapper.batchAddItem(plan.getId(), planItemList);
            }
        }
        // TODO  Update recording queue

    }

    @Override
    @Transactional
    public void delete(Integer planId) {
        RecordPlan recordPlan = recordPlanMapper.get(planId);
        if (recordPlan == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "The recording plan does not exist");
        }
        // Clear associated channels
        channelMapper.removeRecordPlanByPlanId(recordPlan.getId());
        recordPlanMapper.cleanItems(planId);
        recordPlanMapper.delete(planId);
        // TODO  Update recording queue
    }

    @Override
    public PageInfo<RecordPlan> query(Integer page, Integer count, String query) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<RecordPlan> all = recordPlanMapper.query(query);
        return new PageInfo<>(all);
    }

    @Override
    public void link(List<Integer> channelIds, Integer planId) {
        if (channelIds == null || channelIds.isEmpty()) {
            log.info("[Recording plan] association/When removing an association, the channel number must exist");
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Channel number must exist");
        }
        if (planId == null) {
            channelMapper.removeRecordPlan(channelIds);
        }else {
            channelMapper.addRecordPlan(channelIds, planId);
        }
        // Check whether the current to-be-recorded list has changed. If so, call the recording plan to start recording immediately.
        execution();
    }

    @Override
    public PageInfo<CommonGBChannel> queryChannelList(int page, int count, String query, Integer dataType, Boolean online, Integer planId, Boolean hasLink) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = channelMapper.queryForRecordPlanForWebList(planId, query, dataType, online, hasLink);
        return new PageInfo<>(all);
    }

    @Override
    public void linkAll(Integer planId) {
        channelMapper.addRecordPlanForAll(planId);
    }

    @Override
    public void cleanAll(Integer planId) {
        channelMapper.removeRecordPlanByPlanId(planId);
    }
}
