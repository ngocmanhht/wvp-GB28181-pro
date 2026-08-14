package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.RedisGroupMessage;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Receive push device list update notifications sent by redis
 * monitor： SUBSCRIBE VM_MSG_GROUP_LIST_RESPONSE
 * publish PUBLISH VM_MSG_GROUP_LIST_RESPONSE '[{"groupName":"R&DAAS","topGroupGAlias":"6","groupAlias":"6"},{"groupName":"testAAS","topGroupGAlias":"5","groupAlias":"5"},{"groupName":"R&D2","topGroupGAlias":"4","groupAlias":"4"},{"groupName":"Ah, the real deal","topGroupGAlias":"4","groupAlias":"100000009"},{"groupName":"test domain","topGroupGAlias":"3","groupAlias":"3"},{"groupName":"structure1","topGroupGAlias":"3","groupAlias":"100000000"},{"groupName":"structure1-1","topGroupGAlias":"3","parentGAlias":"100000000","groupAlias":"100000001"},{"groupName":"structure2-2","topGroupGAlias":"3","groupAlias":"100000002"},{"groupName":"structure1-2","topGroupGAlias":"3","parentGAlias":"100000001","groupAlias":"100000003"},{"groupName":"structure1-3","topGroupGAlias":"3","parentGAlias":"100000003","groupAlias":"100000004"},{"groupName":"R&D1","topGroupGAlias":"3","groupAlias":"100000005"},{"groupName":"R&D3","topGroupGAlias":"3","parentGAlias":"100000005","groupAlias":"100000006"},{"groupName":"test42","topGroupGAlias":"3","parentGAlias":"100000000","groupAlias":"100000010"},{"groupName":"test2","topGroupGAlias":"3","parentGAlias":"100000000","groupAlias":"100000011"},{"groupName":"test3","topGroupGAlias":"3","parentGAlias":"100000000","groupAlias":"100000007"},{"groupName":"test4","topGroupGAlias":"3","parentGAlias":"100000007","groupAlias":"100000008"}]'
 */
@Slf4j
@Component
public class RedisGroupMsgListener implements MessageListener {

    @Autowired
    private IGroupService groupService;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SipConfig sipConfig;

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void onMessage(Message message, byte[] bytes) {
        String serverId = redisCatchStorage.chooseOneServer(null);
        if (!userSetting.getServerId().equals(serverId)) {
            return;
        }
        log.info("[REDIS: Business group synchronization reply] key： {}， ： {}", VideoManagerConstants.VM_MSG_GROUP_LIST_RESPONSE, new String(message.getBody()));
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
            log.warn("[REDISnews-Business group synchronization reply] When processing the queue, the queue is found to be empty.");
            return;
        }
        // Get all business groups by alias
        Map<String, Group> aliasGroupMap = groupService.queryGroupByAliasMap();
        Map<String, Group> aliasGroupToSave = new LinkedHashMap<>();
        for (Message msg : messageDataList) {
            try {
                log.info("[REDISnews-Business group synchronization reply] Process data:  {}", new String(msg.getBody()));
                List<RedisGroupMessage> groupMessages = JSON.parseArray(new String(msg.getBody()), RedisGroupMessage.class);
                log.info("[REDISnews-Business group synchronization reply] Quantity to be processed:  {}", groupMessages.size());
                for (RedisGroupMessage groupMessage : groupMessages) {

                    // Alias is used here as the basis for judgment. The alias here is often the only one grouped in the third-party system.ID
                    if (groupMessage.getGroupAlias() == null || ObjectUtils.isEmpty(groupMessage.getGroupName())
                            || ObjectUtils.isEmpty(groupMessage.getTopGroupGAlias())) {
                        log.info("[REDISnews-Business group synchronization reply] Message key fields are missing， {}", groupMessage.toString());
                        continue;
                    }
                    boolean isTop = groupMessage.getTopGroupGAlias().equals(groupMessage.getGroupAlias());
                    Group group = aliasGroupMap.get(groupMessage.getGroupAlias());
                    if (group == null) {
                        group = new Group();
                        String deviceId = buildGroupDeviceId(isTop);
                        group.setDeviceId(deviceId);
                        group.setAlias(groupMessage.getGroupAlias());
                        group.setName(groupMessage.getGroupName());
                        group.setCreateTime(DateUtil.getNow());
                    }

                    if (!isTop) {
                        if (ObjectUtils.isEmpty(groupMessage.getTopGroupGAlias())) {
                            log.info("[REDISnews-Business group synchronization reply] Message missing business group alias， {}", groupMessage.toString());
                            continue;
                        }

                        Group topGroup = aliasGroupMap.get(groupMessage.getTopGroupGAlias());
                        if (topGroup == null) {
                            topGroup = aliasGroupToSave.get(groupMessage.getTopGroupGAlias());
                        }
                        if (topGroup == null) {
                            log.info("[REDISnews-Business group synchronization reply] The business group information was not sent or was not sent first.， {}", groupMessage.toString());
                            continue;
                        }
                        group.setBusinessGroup(topGroup.getDeviceId());
                        if (groupMessage.getParentGAlias() != null) {
                            Group parentGroup = aliasGroupMap.get(groupMessage.getParentGAlias());
                            if (parentGroup == null) {
                                parentGroup = aliasGroupToSave.get(groupMessage.getParentGAlias());
                            }
                            if (parentGroup == null) {
                                log.info("[REDISnews-Business group synchronization reply] The virtual organization parent node was not sent or was not sent first.， {}", groupMessage.toString());
                                continue;
                            }
                            group.setParentId(null);
                            group.setParentDeviceId(parentGroup.getDeviceId());
                        } else {
                            group.setParentId(null);
                            group.setParentDeviceId(topGroup.getDeviceId());
                        }
                    } else {
                        group.setParentId(null);
                        group.setBusinessGroup(group.getDeviceId());
                        group.setParentDeviceId(null);
                    }
                    group.setUpdateTime(DateUtil.getNow());
                    aliasGroupToSave.put(group.getAlias(), group);
                }
                log.info("[Business group synchronization reply-Store grouped data] {}", JSONObject.toJSONString(aliasGroupToSave.values()));
                // Store grouped data
                groupService.saveByAlias(aliasGroupToSave.values());

            } catch (ControllerException e) {
                log.warn("[REDISnews-Business group synchronization reply] failed, \r\n{}", e.getMsg());
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
            return String.format(deviceTemplate, codeType, RandomStringUtils.insecure().next(6, false, true));
        } catch (Exception e) {
            log.error("[REDISnews-Business group synchronization reply] Failed to build new group number", e);
            return null;
        }
    }
}
