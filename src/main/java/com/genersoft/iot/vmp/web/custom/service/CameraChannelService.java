package com.genersoft.iot.vmp.web.custom.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.FrontEndControlCodeForPTZ;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.gb28181.event.channel.ChannelEvent;
import com.genersoft.iot.vmp.gb28181.event.subscribe.mobilePosition.MobilePositionEvent;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelControlService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.utils.Coordtransform;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.web.custom.bean.*;
import com.genersoft.iot.vmp.web.custom.conf.SyTokenManager;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.xiaoymin.knife4j.core.util.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;

@Slf4j
@Service
@ConditionalOnProperty(value = "sy.enable", havingValue = "true")
public class CameraChannelService implements CommandLineRunner {

    private final String REDIS_GPS_MESSAGE = "VM_MSG_MOBILE_GPS";
    private final String REDIS_CHANNEL_MESSAGE = "VM_MSG_MOBILE_CHANNEL";
    private final String REDIS_MEMBER_STATUS_MESSAGE = "VM_MSG_MEMBER_STATUS_CHANNEL";
    private final String MOBILE_CHANNEL_PREFIX = "nationalStandardMobileTerminal_";
    private final String DELAY_TASK_KEY = "DELAY_TASK_KEY_";

    @Autowired
    private CommonGBChannelMapper channelMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplateForString;

    @Autowired
    private IGbChannelPlayService channelPlayService;

    @Autowired
    private IGbChannelControlService channelControlService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DynamicTask dynamicTask;

    @Override
    public void run(String... args) {
        // Get global at startuptoken
        String taskKey = UUID.randomUUID().toString();
        if (!refreshToken()) {
            log.info("[SY-readToken]Failed, try again in 30 seconds");
            dynamicTask.startDelay(taskKey, ()->{
                this.run(args);
            }, 30000);
        }else {
            log.info("[SY-readToken] success");
        }
    }

    private boolean refreshToken() {
        String adminToken = redisTemplateForString.opsForValue().get("SYSTEM_ACCESS_TOKEN");
        if (adminToken == null) {
            log.warn("[SYreadTOKEN] SYSTEM_ACCESS_TOKEN Read failed");
            return false;
        }
        SyTokenManager.INSTANCE.adminToken = adminToken;

        String sm4Key = redisTemplateForString.opsForValue().get("SYSTEM_SM4_KEY");
        if (sm4Key == null) {
            log.warn("[SYreadTOKEN] SYSTEM_SM4_KEY Read failed");
            return false;
        }
        SyTokenManager.INSTANCE.sm4Key = sm4Key;

        JSONObject appJson = (JSONObject)redisTemplate.opsForValue().get("SYSTEM_APPKEY");
        if (appJson == null) {
            log.warn("[SYreadTOKEN] SYSTEM_APPKEY Read failed");
            return false;
        }
        SyTokenManager.INSTANCE.appMap.put(appJson.getString("appKey"), appJson.getString("appSecret"));

        JSONObject timeJson = (JSONObject)redisTemplate.opsForValue().get("sys_INTERFACE_VALID_TIME");
        if (timeJson == null) {
            log.warn("[SYreadTOKEN] sys_INTERFACE_VALID_TIME Read failed");
            return false;
        }
        SyTokenManager.INSTANCE.expires = timeJson.getLong("systemValue");

        return true;
    }

    // Monitor channel changes and send redis messages if it is a mobile device
    @EventListener
    public void onApplicationEvent(ChannelEvent event) {
        List<CommonGBChannel> channels = event.getChannels();
        if (channels.isEmpty()) {
            return;
        }
        List<CommonGBChannel> resultListForAdd = new ArrayList<>();
        List<CameraChannel> resultListForDelete = new ArrayList<>();
        List<CommonGBChannel> resultListForUpdate = new ArrayList<>();
        List<CommonGBChannel> resultListForOnline = new ArrayList<>();
        List<CommonGBChannel> resultListForOffline = new ArrayList<>();

        Map<String, CommonGBChannel> delayChannelMap = new HashMap<>();

        List<SYMember> memberList = new ArrayList<>();


        switch (event.getMessageType()) {
            case UPDATE:
                List<CommonGBChannel> oldChannelList = event.getOldChannels();
                List<CommonGBChannel> channelList = event.getChannels();
                // update operation
                if (oldChannelList == null || oldChannelList.isEmpty()) {
                    // If there is no old equipment, there is no need to judge. Currently, only when groups or administrative divisions are converted to channel information, there is no old channel information. These two types do not need to send notifications and can be ignored directly.
                    break;
                }
                // It is necessary to compare old data to see if it is a newly added mobile device or a canceled mobile device
                // Convert channelList to gbDeviceId as key Map
                Map<String, CommonGBChannel> oldChannelMap = new HashMap<>();
                for (CommonGBChannel channel : oldChannelList) {
                    if (channel != null && channel.getGbDeviceId() != null) {
                        oldChannelMap.put(channel.getGbDeviceId(), channel);
                    }
                }
                for (CommonGBChannel channel : channelList) {
                    if (channel.getGbPtzType() != null && channel.getGbPtzType() == 99) {
                        CommonGBChannel oldChannel = oldChannelMap.get(channel.getGbDeviceId());
                        if (channel.getGbStatus() == null) {
                            channel.setGbStatus(oldChannel.getGbStatus());
                        }
                        if (oldChannel != null) {
                            if (oldChannel.getGbPtzType() != null && oldChannel.getGbPtzType() == 99) {
                                resultListForUpdate.add(channel);
                                // Send message if status changes
                                if (!Objects.equals(oldChannel.getGbStatus(), channel.getGbStatus())) {
                                    SYMember member = getMember(channel.getGbDeviceId());
                                    if (member != null) {
                                        if ("ON".equals(channel.getGbStatus())) {
                                            member.setTerminalMemberStatus("ONLINE");
                                        }else {
                                            member.setTerminalMemberStatus("OFFLINE");
                                        }
                                        memberList.add(member);
                                    }
                                }

                            }else {
                                resultListForAdd.add(channel);
                                if ("ON".equals(channel.getGbStatus())) {
                                    delayChannelMap.put(channel.getGbDeviceId(), channel);
                                }
                            }
                        }else {
                            resultListForAdd.add(channel);
                            if ("ON".equals(channel.getGbStatus())) {
                                delayChannelMap.put(channel.getGbDeviceId(), channel);
                            }
                        }
                    }else {
                        CommonGBChannel oldChannel = oldChannelMap.get(channel.getGbDeviceId());
                        if (oldChannel != null && oldChannel.getGbPtzType() != null && oldChannel.getGbPtzType() == 99) {
                            CameraChannel cameraChannel = new CameraChannel();
                            cameraChannel.setGbDeviceId(channel.getGbDeviceId());
                            resultListForDelete.add(cameraChannel);
                            SYMember member = getMember(cameraChannel.getGbDeviceId());
                            if (member != null) {
                                member.setTerminalMemberStatus("OFFLINE");
                                memberList.add(member);
                            }
                        }
                    }
                }

                break;
            case DEL:
                for (CommonGBChannel channel : channels) {
                    if (channel.getGbPtzType()  != null && channel.getGbPtzType() == 99) {
                        CameraChannel cameraChannel = new CameraChannel();
                        cameraChannel.setGbDeviceId(channel.getGbDeviceId());
                        resultListForDelete.add(cameraChannel);
                        SYMember member = getMember(cameraChannel.getGbDeviceId());
                        if (member != null) {
                            member.setTerminalMemberStatus("OFFLINE");
                            memberList.add(member);
                        }
                    }
                }

                break;
            case ON:
            case OFF:
            case DEFECT:
            case VLOST:
                for (CommonGBChannel channel : channels) {
                    if (channel.getGbPtzType()  != null && channel.getGbPtzType() == 99) {
                        CameraChannel cameraChannel = channelMapper.queryCameraChannelById(channel.getGbId());
                        SYMember member = getMember(cameraChannel.getGbDeviceId());

                        if (event.getMessageType() == ChannelEvent.ChannelEventMessageType.ON) {
                            cameraChannel.setGbStatus("ON");
                            resultListForOnline.add(cameraChannel);
                            if (member != null) {
                                member.setTerminalMemberStatus("ONLINE");
                                memberList.add(member);
                            }
                        }else {
                            cameraChannel.setGbStatus("OFF");
                            resultListForOffline.add(cameraChannel);
                            if (member != null) {
                                member.setTerminalMemberStatus("OFFLINE");
                                memberList.add(member);
                            }
                        }
                    }
                }
                break;
            case ADD:
                for (CommonGBChannel channel : channels) {
                    if (channel.getGbPtzType()  != null && channel.getGbPtzType() == 99) {
                        resultListForAdd.add(channel);
                        if ("ON".equals(channel.getGbStatus())) {
                            delayChannelMap.put(channel.getGbDeviceId(), channel);
                        }
                    }
                }
                break;
        }
        if (!resultListForDelete.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", ChannelEvent.ChannelEventMessageType.DEL);
            jsonObject.put("list", resultListForDelete);
            log.info("[SY-redisSend notification-DEL] Send channel information changes {}: {}", REDIS_CHANNEL_MESSAGE, jsonObject.toString());
            redisTemplateForString.convertAndSend(REDIS_CHANNEL_MESSAGE, jsonObject.toString());
        }
        if (!resultListForAdd.isEmpty()) {
            sendChannelMessage(resultListForAdd, ChannelEvent.ChannelEventMessageType.ADD);
        }
        if (!resultListForUpdate.isEmpty()) {
            sendChannelMessage(resultListForUpdate, ChannelEvent.ChannelEventMessageType.UPDATE);
        }
        if (!resultListForOnline.isEmpty()) {
            sendChannelMessage(resultListForOnline, ChannelEvent.ChannelEventMessageType.ON);
        }
        if (!resultListForOffline.isEmpty()) {
            sendChannelMessage(resultListForOffline, ChannelEvent.ChannelEventMessageType.OFF);
        }
        if (!memberList.isEmpty()) {
            sendMemberStatusMessage(memberList);
        }
        if (!delayChannelMap.isEmpty()) {
            // Delay checking and sending for online terminals
            for (CommonGBChannel commonGBChannel : delayChannelMap.values()) {
                String key = DELAY_TASK_KEY + commonGBChannel.getGbDeviceId();
                dynamicTask.startDelay(key, () -> {
                    dynamicTask.stop(key);
                    SYMember member = getMember(commonGBChannel.getGbDeviceId());
                    if (member == null) {
                        return;
                    }
                    member.setTerminalMemberStatus("ONLINE");
                    sendMemberStatusMessage(List.of(member));
                }, 3000);
            }
        }
    }


    private void sendMemberStatusMessage(List<SYMember> memberList) {
        // Cancel delayed delivery
        for (SYMember syMember : memberList) {
            String key = DELAY_TASK_KEY + syMember.getChannelDeviceId();
            if (dynamicTask.contains(key)) {
                log.info("[SY-redisSend notification] Cancel delayed new tasks: {}", key);
                dynamicTask.stop(key);
            }
        }

        String jsonString = JSONObject.toJSONString(memberList);
        log.info("[SY-redisSend notification] Send status change {}: {}", REDIS_MEMBER_STATUS_MESSAGE, jsonString);
        redisTemplateForString.convertAndSend(REDIS_MEMBER_STATUS_MESSAGE, jsonString);
    }

    private void sendChannelMessage(List<CommonGBChannel> channelList, ChannelEvent.ChannelEventMessageType type) {
        if (channelList.isEmpty()) {
            log.warn("[SY-redisSend notification-{}] Sending failed, data is empty, channel information changes {}", type, REDIS_CHANNEL_MESSAGE);
            return;
        }
        List<CameraChannel> cameraChannelList = channelMapper.queryCameraChannelByIds(channelList);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", type);
        jsonObject.put("list", cameraChannelList);
        log.info("[SY-redisSend notification-{}] Send channel information changes {}: {}", type, REDIS_CHANNEL_MESSAGE, jsonObject.toString());
        redisTemplateForString.convertAndSend(REDIS_CHANNEL_MESSAGE, jsonObject.toString());
    }

    // Listen to GPS messages and send redis messages if it is a mobile device
    @Async
    @EventListener
    public void onApplicationEvent(MobilePositionEvent event) {
        List<? extends MobilePosition> mobilePositionList = event.getMobilePositionList();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (MobilePosition mobilePosition : mobilePositionList) {
                executor.submit(() -> {
                    // Supplementary information from redis
                    SYMember member = getMember(mobilePosition.getChannelDeviceId());
                    if (member == null) {
                        log.info("[SY-redisSend notification-Mobile device location information] Cache not retrieved {}", mobilePosition.toString());
                        return;
                    }

                    // Send redis message
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("gpsDate", DateUtil.timestampMsTo_yyyy_MM_dd_HH_mm_ss(mobilePosition.getTimestamp()));
                    jsonObject.put("unicodeNo", member.getUnicodeNo());
                    jsonObject.put("memberNo", member.getNo());
                    jsonObject.put("unitNo", member.getUnitNo());
                    jsonObject.put("longitude", mobilePosition.getLongitude());
                    jsonObject.put("latitude", mobilePosition.getLatitude());
                    jsonObject.put("altitude", mobilePosition.getAltitude());
                    jsonObject.put("direction", mobilePosition.getDirection());
                    jsonObject.put("speed", mobilePosition.getSpeed());
                    jsonObject.put("blockId", member.getBlockId());
                    jsonObject.put("gbDeviceId", mobilePosition.getChannelDeviceId());
                    log.info("[SY-redisSend notification-Mobile device location information] send {}: {}", REDIS_GPS_MESSAGE, jsonObject.toString());
                    redisTemplateForString.convertAndSend(REDIS_GPS_MESSAGE, jsonObject.toString());
                });
            }
        }



    }

    public SYMember getMember(String deviceId) {
        // Supplementary information from redis
        String key = MOBILE_CHANNEL_PREFIX + deviceId;
        JSONObject jsonObject = (JSONObject)redisTemplate.opsForValue().get(key);
        if (jsonObject == null) {
            return null;
        }
        SYMember syMember = JSONObject.parseObject(jsonObject.toString(), SYMember.class);
        syMember.setChannelDeviceId(deviceId);
        return syMember;
    }


    public PageInfo<CameraChannel> queryList(Integer page, Integer count, String groupAlias, Boolean status, String geoCoordSys) {
        // Build organizational structure information
        Group group = groupMapper.queryGroupByAlias(groupAlias);
        if (group == null) {
            log.warn("[SY-Query the camera list, only query the cameras under the current virtual organization] Organizational structure does not exist: {}", groupAlias);
            return new PageInfo<>(Collections.emptyList());
        }

        String groupDeviceId = group.getDeviceId();

        // Build pagination
        PageHelper.startPage(page, count);

        List<CameraChannel> all = channelMapper.queryListForSy(groupDeviceId, status);
        PageInfo<CameraChannel> groupPageInfo = new PageInfo<>(all);
        List<CameraChannel> list = addIconPathAndPositionForCameraChannelList(groupPageInfo.getList(), geoCoordSys);
        groupPageInfo.setList(list);
        return groupPageInfo;
    }

    public PageInfo<CameraChannel> queryListWithChild(Integer page, Integer count, String query, String sortName, Boolean order, String groupAlias, Boolean status, String geoCoordSys) {

        List<CameraGroup> groupList = null;
        // Build organizational structure information
        if (groupAlias != null) {
            CameraGroup group = groupMapper.queryGroupByAlias(groupAlias);
            if (group == null) {
                log.warn("[SY-Query the camera list, query the current virtual organization and all sub-nodes] Organizational structure does not exist: {}", groupAlias);
                return new PageInfo<>(Collections.emptyList());
            }
            // Get all child nodes
            groupList = queryAllGroupChildren(group.getId(), group.getBusinessGroup());
            groupList.add(group);
        }

        // Build pagination
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        if (order == null) {
            order = true;
        }
        List<CameraChannel> all = channelMapper.queryListWithChildForSy(query, sortName, order, groupList, status);
        PageInfo<CameraChannel> groupPageInfo = new PageInfo<>(all);
        List<CameraChannel> list = addIconPathAndPositionForCameraChannelList(groupPageInfo.getList(), geoCoordSys);
        groupPageInfo.setList(list);
        return groupPageInfo;
    }

    // Get all child nodes
    private List<CameraGroup> queryAllGroupChildren(int groupId, String businessGroup) {
        Map<Integer, CameraGroup> groupMap = groupMapper.queryByBusinessGroupForMap(businessGroup);
        for (CameraGroup cameraGroup : groupMap.values()) {
            cameraGroup.setParent(groupMap.get(cameraGroup.getParentId()));
        }
        CameraGroup cameraGroup = groupMap.get(groupId);
        if (cameraGroup == null) {
            return Collections.emptyList();
        }else {
            return cameraGroup.getChild();
        }
    }

    public List<CameraCount> queryCountWithChild(String groupAlias) {
        // Build organizational structure information
        CameraGroup group = groupMapper.queryGroupByAlias(groupAlias);
        if (group == null) {
            log.warn("[SY-Count the number of cameras by organizational structure] Organizational structure does not exist: {}", groupAlias);
            return Collections.emptyList();
        }
        // Get all child nodes
        List<CameraGroup> groupList = queryAllGroupChildren(group.getId(), group.getBusinessGroup());
        groupList.add(group);

        // TODO The sorting here can be optimized, try to let sql directly return the corresponding structure without the need for secondary sorting.
        List<CameraCount> cameraCounts = groupMapper.queryCountWithChild(groupList);
        if (cameraCounts.isEmpty()) {
            return Collections.emptyList();
        }else {
           Map<String, String> cameraGroupMap = new HashMap<>();
            for (CameraGroup cameraGroup : groupList) {
                cameraGroupMap.put(cameraGroup.getDeviceId(), cameraGroup.getAlias());
            }
            List<CameraCount> result = new ArrayList<>();
            for (CameraCount cameraCount : cameraCounts) {
                String alias = cameraGroupMap.get(cameraCount.getDeviceId());
                if (alias == null) {
                    continue;
                }
                cameraCount.setGroupAlias(alias);
                result.add(cameraCount);
            }
            return result;
        }
    }

    /**
     * Add image information and transform coordinate systems to channels
     */
    private List<CameraChannel> addIconPathAndPositionForCameraChannelList(List<CameraChannel> channels, String geoCoordSys) {
        // Read redis icon information
        /*
          {
              "brand": "WVP",
              "createdTime": 1715845840000,
              "displayInSelect": true,
              "id": 12,
              "imagesPath": "images/lt132",
              "machineName": "Picture transmission intercom individual soldier",
              "machineType": "LT132"
           },
         */
        JSONArray jsonArray = (JSONArray) redisTemplate.opsForValue().get("machineInfo");
        Map<String, String> pathMap = new HashMap<>();
        if (jsonArray != null && !jsonArray.isEmpty()) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String machineType = jsonObject.getString("machineType");
                String imagesPath = jsonObject.getString("imagesPath");
                if (machineType != null && imagesPath != null) {
                    pathMap.put(machineType, imagesPath);
                }
            }
        }else {
            log.warn("[Failed to read channel icon information]");
        }
        for (CameraChannel channel : channels) {
            if (channel.getGbModel() != null && pathMap.get(channel.getGbModel()) != null) {
                channel.setIcon(pathMap.get(channel.getGbModel()));
            }
            // Coordinate system conversion
            if (geoCoordSys != null && channel.getGbLongitude() != null && channel.getGbLatitude() != null
                    && channel.getGbLongitude() > 0 && channel.getGbLatitude() > 0) {
                if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                    Double[] position = Coordtransform.WGS84ToGCJ02(channel.getGbLongitude(), channel.getGbLatitude());
                    channel.setGbLongitude(position[0]);
                    channel.setGbLatitude(position[1]);
                }else if (geoCoordSys.equalsIgnoreCase("BD09")) {
                    Double[] gcj02Position = Coordtransform.WGS84ToGCJ02(channel.getGbLongitude(), channel.getGbLatitude());
                    Double[] position = Coordtransform.GCJ02ToBD09(gcj02Position[0], gcj02Position[1]);
                    channel.setGbLongitude(position[0]);
                    channel.setGbLatitude(position[1]);
                }
            }
        }
        return channels;
    }

    public CameraChannel queryOne(String deviceId, String deviceCode, String geoCoordSys) {
        List<CameraChannel> cameraChannels = channelMapper.queryGbChannelByChannelDeviceIdAndGbDeviceId(deviceId, deviceCode);
        Assert.isTrue(cameraChannels.isEmpty(), "Channel does not exist");
        List<CameraChannel> channels = addIconPathAndPositionForCameraChannelList(cameraChannels, geoCoordSys);
        CameraChannel channel = channels.get(0);
        if (deviceCode != null) {
            channel.setDeviceCode(deviceCode);
        }

        return channel;
    }

    /**
     * Playback channel
     * @param deviceId Channel number
     * @param deviceCode The number of the national standard equipment corresponding to the channel
     * @param callback Replay of on-demand results
     */
    public void play(String deviceId, String deviceCode, ErrorCallback<CameraStreamInfo> callback) {
        List<CameraChannel> cameraChannels = channelMapper.queryGbChannelByChannelDeviceIdAndGbDeviceId(deviceId, deviceCode);
        Assert.isTrue(cameraChannels.isEmpty(), "Channel does not exist");
        CameraChannel channel = cameraChannels.get(0);
        channelPlayService.play(channel, null, userSetting.getRecordSip(), (code, msg, data) -> {
            callback.run(code, msg, new CameraStreamInfo(channel, data));
        });
    }

    /**
     * Stop playing channel
     * @param deviceId Channel number
     * @param deviceCode The number of the national standard equipment corresponding to the channel
     */
    public void stopPlay(String deviceId, String deviceCode) {
        List<CameraChannel> cameraChannels = channelMapper.queryGbChannelByChannelDeviceIdAndGbDeviceId(deviceId, deviceCode);
        Assert.isTrue(cameraChannels.isEmpty(), "Channel does not exist");
        CameraChannel channel = cameraChannels.get(0);
        channelPlayService.stopPlay(channel);
    }

    public void ptz(String deviceId, String deviceCode, String command, Integer speed, ErrorCallback<String> callback) {
        List<CameraChannel> cameraChannels = channelMapper.queryGbChannelByChannelDeviceIdAndGbDeviceId(deviceId, deviceCode);
        Assert.isTrue(cameraChannels.isEmpty(), "Channel does not exist");
        CameraChannel channel = cameraChannels.get(0);

        if (speed == null) {
            speed = 50;
        }else if (speed < 0 || speed > 100) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "panSpeed for 0-100number");
        }

        FrontEndControlCodeForPTZ controlCode = new FrontEndControlCodeForPTZ();
        controlCode.setPanSpeed(speed);
        controlCode.setTiltSpeed(speed);
        controlCode.setZoomSpeed(speed);
        switch (command){
            case "left":
                controlCode.setPan(0);
                break;
            case "right":
                controlCode.setPan(1);
                break;
            case "up":
                controlCode.setTilt(0);
                break;
            case "down":
                controlCode.setTilt(1);
                break;
            case "upleft":
                controlCode.setPan(0);
                controlCode.setTilt(0);
                break;
            case "upright":
                controlCode.setTilt(0);
                controlCode.setPan(1);
                break;
            case "downleft":
                controlCode.setPan(0);
                controlCode.setTilt(1);
                break;
            case "downright":
                controlCode.setTilt(1);
                controlCode.setPan(1);
                break;
            case "zoomin":
                controlCode.setZoom(1);
                break;
            case "zoomout":
                controlCode.setZoom(0);
                break;
            default:
                break;
        }

        channelControlService.ptz(channel, controlCode, callback);
    }

    public void updateCamera(String deviceId, String deviceCode, String name, Double longitude, Double latitude, String geoCoordSys) {
        List<CameraChannel> cameraChannels = channelMapper.queryGbChannelByChannelDeviceIdAndGbDeviceId(deviceId, deviceCode);
        Assert.isTrue(cameraChannels.isEmpty(), "Channel does not exist");
        CameraChannel commonGBChannel = cameraChannels.get(0);
        commonGBChannel.setGbName(name);
        if (geoCoordSys != null && longitude != null && latitude != null
                && longitude > 0 && latitude > 0) {
            if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                Double[] position = Coordtransform.GCJ02ToWGS84(longitude, latitude);
                commonGBChannel.setGbLongitude(position[0]);
                commonGBChannel.setGbLatitude(position[1]);
            }else if (geoCoordSys.equalsIgnoreCase("BD09")) {
                Double[] gcj02Position = Coordtransform.BD09ToGCJ02(longitude, latitude);
                Double[] position = Coordtransform.GCJ02ToWGS84(gcj02Position[0], gcj02Position[1]);
                commonGBChannel.setGbLongitude(position[0]);
                commonGBChannel.setGbLatitude(position[1]);
            }else {
                commonGBChannel.setGbLongitude(longitude);
                commonGBChannel.setGbLatitude(latitude);
            }
        }else {
            commonGBChannel.setGbLongitude(longitude);
            commonGBChannel.setGbLatitude(latitude);
        }
        channelMapper.update(commonGBChannel);
    }

    public List<CameraChannel> queryListByDeviceIds(List<String> deviceIds, String geoCoordSys) {
        List<CameraChannel> cameraChannels = channelMapper.queryListByDeviceIds(deviceIds);
        return addIconPathAndPositionForCameraChannelList(cameraChannels, geoCoordSys);
    }

    public List<CameraChannel> queryListByAddressAndDirectionType(String address, Integer directionType, String geoCoordSys) {
        List<CameraChannel> cameraChannels = channelMapper.queryListByAddressAndDirectionType(address, directionType);
        return addIconPathAndPositionForCameraChannelList(cameraChannels, geoCoordSys);
    }


    public List<CameraChannel> queryListInBox(Double minLongitude, Double maxLongitude, Double minLatitude, Double maxLatitude, Integer level, String groupAlias, String geoCoordSys) {
        // Build organizational structure information
        CameraGroup group = groupMapper.queryGroupByAlias(groupAlias);
        if (group == null) {
            log.warn("[SY-Frame selection] Organizational structure does not exist: {}", groupAlias);
            return Collections.emptyList();
        }
        // Get all child nodes
        List<CameraGroup> groupList = queryAllGroupChildren(group.getId(), group.getBusinessGroup());
        groupList.add(group);
        // Parametric coordinate series conversion
        if (geoCoordSys != null) {
            if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                Double[] minPosition = Coordtransform.GCJ02ToWGS84(minLongitude, minLatitude);
                minLongitude = minPosition[0];
                minLatitude = minPosition[1];

                Double[] maxPosition = Coordtransform.GCJ02ToWGS84(maxLongitude, maxLatitude);
                maxLongitude = maxPosition[0];
                maxLatitude = maxPosition[1];
            }else if (geoCoordSys.equalsIgnoreCase("BD09")) {
                Double[] gcj02MinPosition = Coordtransform.BD09ToGCJ02(minLongitude, minLatitude);
                Double[] minPosition = Coordtransform.GCJ02ToWGS84(gcj02MinPosition[0], gcj02MinPosition[1]);
                minLongitude = minPosition[0];
                minLatitude = minPosition[1];

                Double[] gcj02MaxPosition = Coordtransform.BD09ToGCJ02(maxLongitude, maxLatitude);
                Double[] maxPosition = Coordtransform.GCJ02ToWGS84(gcj02MaxPosition[0], gcj02MaxPosition[1]);
                maxLongitude = maxPosition[0];
                maxLatitude = maxPosition[1];
            }
        }

        List<CameraChannel> all = channelMapper.queryListInBox(minLongitude, maxLongitude, minLatitude, maxLatitude, level, groupList);
        return addIconPathAndPositionForCameraChannelList(all, geoCoordSys);
    }

    public List<CameraChannel> queryListInCircle(Double centerLongitude, Double centerLatitude, Double radius, Integer level, String groupAlias, String geoCoordSys) {
        // Build organizational structure information
        CameraGroup group = groupMapper.queryGroupByAlias(groupAlias);
        if (group == null) {
            log.warn("[SY-circle selection] Organizational structure does not exist: {}", groupAlias);
            return Collections.emptyList();
        }
        // Get all child nodes
        List<CameraGroup> groupList = queryAllGroupChildren(group.getId(), group.getBusinessGroup());
        groupList.add(group);

        // Parametric coordinate series conversion
        if (geoCoordSys != null) {
            if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                Double[] position = Coordtransform.GCJ02ToWGS84(centerLongitude, centerLatitude);
                centerLongitude = position[0];
                centerLatitude = position[1];

            }else if (geoCoordSys.equalsIgnoreCase("BD09")) {
                Double[] gcj02Position = Coordtransform.BD09ToGCJ02(centerLongitude, centerLatitude);
                Double[] position = Coordtransform.GCJ02ToWGS84(gcj02Position[0], gcj02Position[1]);
                centerLongitude = position[0];
                centerLatitude = position[1];
            }
        }

        List<CameraChannel> all = channelMapper.queryListInCircle(centerLongitude, centerLatitude, radius, level, groupList);
        return addIconPathAndPositionForCameraChannelList(all, geoCoordSys);
    }

    public List<CameraChannel> queryListInPolygon(List<Point> pointList, String groupAlias, Integer level, String geoCoordSys) {
        // Build organizational structure information
        CameraGroup group = groupMapper.queryGroupByAlias(groupAlias);
        if (group == null) {
            log.warn("[SY-polygon] Organizational structure does not exist: {}", groupAlias);
            return Collections.emptyList();
        }
        // Get all child nodes
        List<CameraGroup> groupList = queryAllGroupChildren(group.getId(), group.getBusinessGroup());
        groupList.add(group);

        // Parametric coordinate series conversion
        if (geoCoordSys != null) {
            for (Point point : pointList) {
                if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                    Double[] position = Coordtransform.GCJ02ToWGS84(point.getLng(), point.getLat());
                    point.setLng(position[0]);
                    point.setLat(position[1]);
                }else if (geoCoordSys.equalsIgnoreCase("BD09")) {
                    Double[] gcj02Position = Coordtransform.BD09ToGCJ02(point.getLng(), point.getLat());
                    Double[] position = Coordtransform.GCJ02ToWGS84(gcj02Position[0], gcj02Position[1]);
                    point.setLng(position[0]);
                    point.setLat(position[1]);
                }
            }
        }

        List<CameraChannel> all = channelMapper.queryListInPolygon(pointList, level, groupList);
        return addIconPathAndPositionForCameraChannelList(all, geoCoordSys);
    }

    public PageInfo<CameraChannel> queryListForMobile(Integer page, Integer count, String topGroupAlias) {

        CameraGroup cameraGroup = groupMapper.queryGroupByAlias(topGroupAlias);

        String business = null;
        if (cameraGroup != null) {
            business = cameraGroup.getDeviceId();
        }
        // Build pagination
        PageHelper.startPage(page, count);
        List<CameraChannel> all = channelMapper.queryListForSyMobile(business);

        PageInfo<CameraChannel> groupPageInfo = new PageInfo<>(all);
        List<CameraChannel> list = addIconPathAndPositionForCameraChannelList(groupPageInfo.getList(), null);
        groupPageInfo.setList(list);
        return groupPageInfo;
    }


    public List<CameraChannel> queryMeetingChannelList(String topGroupAlias) {
        CameraGroup cameraGroup = groupMapper.queryGroupByAlias(topGroupAlias);
        Assert.notNull(cameraGroup, "Domain does not exist");
        String business = cameraGroup.getDeviceId();
        Assert.notNull(business, "Domain does not exist");

        return channelMapper.queryMeetingChannelList(business);
    }
}
