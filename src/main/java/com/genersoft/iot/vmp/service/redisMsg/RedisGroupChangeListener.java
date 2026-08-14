package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.RedisGroupMessage;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Auther: JiangFeng
 * @Date: 2022/8/16 11:32
 * @Description: Receive push device list update notifications sent by redis
 * monitor：  SUBSCRIBE VM_MSG_GROUP_LIST_CHANGE
 * publish PUBLISH VM_MSG_GROUP_LIST_CHANGE  '[{"groupName":"Test domain modified","topGroupGAlias":3,"messageType":"update","groupAlias":3}]'
 */
@Slf4j
@Component
public class RedisGroupChangeListener implements MessageListener {

    @Resource
    private IGroupService groupService;

    @Resource
    private IStreamPushService streamPushService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SipConfig sipConfig;

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void onMessage(Message message, byte[] bytes) {
        log.info("[REDIS-Group information changes] key： {}， ： {}", VideoManagerConstants.VM_MSG_GROUP_LIST_CHANGE, new String(message.getBody()));
        taskQueue.offer(message);
    }

    @Scheduled(fixedDelay = 100)
    public void executeTaskQueue() {
        if (taskQueue.isEmpty()) {
            return;
        }
        List<Message> messageDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            Message msg = taskQueue.poll();
            if (msg != null) {
                messageDataList.add(msg);
            }
        }
        if (messageDataList.isEmpty()) {
            return;
        }
        for (Message msg : messageDataList) {
            try {
                List<RedisGroupMessage> groupMessages = JSON.parseArray(new String(msg.getBody()), RedisGroupMessage.class);
                for (int i = 0; i < groupMessages.size(); i++) {
                    RedisGroupMessage groupMessage = groupMessages.get(i);
                    log.info("[REDISnews-Group information update] {}", groupMessage.toString());
                    Group group = groupService.queryGroupByAlias(groupMessage.getGroupAlias());
                    switch (groupMessage.getMessageType()){
                        case "add":
                            // Alias is used here as the basis for judgment. The alias here is often the only one grouped in the third-party system.ID
                            if (groupMessage.getGroupAlias() == null || ObjectUtils.isEmpty(groupMessage.getGroupName())
                                    || ObjectUtils.isEmpty(groupMessage.getTopGroupGAlias())) {
                                log.info("[REDISnews-New grouping information] Message key fields are missing， {}", groupMessage.toString());
                                continue;
                            }
                            if (group != null) {
                                log.info("[REDISnews-New grouping information] failed {}，Alias already exists", groupMessage.getGroupAlias());
                                continue;
                            }
                            group = new Group();
                            boolean isTop = groupMessage.getTopGroupGAlias().equals(groupMessage.getGroupAlias());
                            String deviceId = buildGroupDeviceId(isTop);
                            group.setDeviceId(deviceId);
                            group.setAlias(groupMessage.getGroupAlias());
                            group.setName(groupMessage.getGroupName());
                            if (!isTop) {
                                if (ObjectUtils.isEmpty(groupMessage.getTopGroupGAlias()) ) {
                                    log.info("[REDISnews-New grouping information] The message is missing the business group alias or parent node alias， {}", groupMessage.toString());
                                    continue;
                                }

                                Group topGroup = groupService.queryGroupByAlias(groupMessage.getTopGroupGAlias());
                                if (topGroup == null) {
                                    log.info("[REDISnews-New grouping information] Business group information is not stored in the database， {}", groupMessage.toString());
                                    continue;
                                }
                                group.setBusinessGroup(topGroup.getDeviceId());
                                group.setParentId(topGroup.getId());
                            }
                            if (groupMessage.getParentGAlias() != null) {
                                Group parentGroup = groupService.queryGroupByAlias(groupMessage.getParentGAlias());
                                if (parentGroup == null) {
                                    log.info("[REDISnews-New grouping information] The virtual organization parent node information is not stored in the database， {}", groupMessage.toString());
                                    continue;
                                }
                                group.setParentId(parentGroup.getId());
                                group.setParentDeviceId(parentGroup.getDeviceId());
                            }
                            group.setCreateTime(DateUtil.getNow());
                            group.setUpdateTime(DateUtil.getNow());
                            groupService.add(group);

                            break;
                        case "update":
                            // Alias is used here as the basis for judgment. The alias here is often the only one grouped in the third-party system.ID
                            if (groupMessage.getGroupAlias() == null) {
                                log.info("[REDISnews-Group information update] Message key fields are missing， {}", groupMessage.toString());
                                continue;
                            }
                            if (group == null ) {
                                log.info("[REDISnews-Group information update] failed {}，Alias does not exist", groupMessage.getGroupAlias());
                                continue;
                            }
                            group.setName(groupMessage.getGroupName());
                            group.setUpdateTime(DateUtil.getNow());
                            if (groupMessage.getParentGAlias() != null) {
                                Group parentGroup = groupService.queryGroupByAlias(groupMessage.getParentGAlias());
                                if (parentGroup == null) {
                                    log.info("[REDISnews-Group information update] The virtual organization parent node information is not stored in the database， {}", groupMessage.toString());
                                    continue;
                                }
                                group.setParentId(parentGroup.getId());
                                group.setParentDeviceId(parentGroup.getDeviceId());
                            }else {
                                Group businessGroup = groupService.queryGroupByDeviceId(group.getBusinessGroup());
                                if (businessGroup == null ) {
                                    log.info("[REDISnews-Group information update] failed {}，Business group does not exist", groupMessage.getGroupAlias());
                                    continue;
                                }
                                group.setParentId(businessGroup.getId());
                                group.setParentDeviceId(null);
                            }
                            groupService.update(group);
                            break;
                        case "delete":
                            // Alias is used here as the basis for judgment. The alias here is often the only one grouped in the third-party system.ID
                            if (groupMessage.getGroupAlias() == null) {
                                log.info("[REDISnews-Group information deletion] Message key fields are missing， {}", groupMessage.toString());
                                continue;
                            }
                            if (group == null) {
                                log.info("[REDISnews-Group information deletion] failed {}，Alias does not exist", groupMessage.getGroupAlias());
                                continue;
                            }
                            groupService.delete(group.getId());
                            break;
                        default:
                            log.info("[REDISnews-Group information changes] Unrecognized message type {}，Currently supported message types are add、update、delete", groupMessage.getMessageType());
                    }
                }

            } catch (Exception e) {
                log.warn("[REDISnews-Business group synchronization reply] Unhandled exception found, \r\n{}", new String(msg.getBody()));
                log.error("[REDISnews-Business group synchronization reply] Unusual content： ", e);
            }
        }

    }

    /**
     * Generate grouping national standard number
     */
    private String buildGroupDeviceId(boolean isTop) {
        try {
            String deviceTemplate = userSetting.getGroupSyncDeviceTemplate();
            if (ObjectUtils.isEmpty(deviceTemplate) || !deviceTemplate.contains("%s")) {
                String domain = sipConfig.getDomain();
                if (domain.length() != 10) {
                    domain = sipConfig.getId().substring(0, 10);
                }
                deviceTemplate = domain + "%s0%s";
            }
            String codeType = "216";
            if (isTop) {
                codeType = "215";
            }
            return String.format(deviceTemplate, codeType, RandomStringUtils.secureStrong().next(6, false, true));
        }catch (Exception e) {
            log.error("[REDISnews-Business group synchronization reply] Failed to build new group number", e);
            return null;
        }
    }
}
