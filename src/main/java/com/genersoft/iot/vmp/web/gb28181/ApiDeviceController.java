package com.genersoft.iot.vmp.web.gb28181;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Preset;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.web.gb28181.dto.DeviceChannelExtend;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.*;

/**
 * APICompatible: device information
 */
@SuppressWarnings("unchecked")
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/device")
@Hidden
public class ApiDeviceController {

    @Autowired
    private SIPCommander cmder;
    @Autowired
    private IDeviceChannelService channelService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private IDeviceService deviceService;


    /**
     * Get the device list through paging. Now return directly, paging has not been implemented yet.
     * @param start
     * @param limit
     * @param q
     * @param online
     * @return
     */
    @GetMapping(value = "/list")
    public JSONObject list( @RequestParam(required = false)Integer start,
                            @RequestParam(required = false)Integer limit,
                            @RequestParam(required = false)String q,
                            @RequestParam(required = false)Boolean online ){

//        if (logger.isDebugEnabled()) {
//            logger.debug("Query all video device API calls");
//        }

        JSONObject result = new JSONObject();
        List<Device> devices;
        if (start == null || limit ==null) {
            devices = deviceService.getAllByStatus(online);
            result.put("DeviceCount", devices.size());
        }else {
            PageInfo<Device> deviceList = deviceService.getAll(start/limit, limit,null, online);
            result.put("DeviceCount", deviceList.getTotal());
            devices = deviceList.getList();
        }

        JSONArray deviceJSONList = new JSONArray();
        devices.stream().forEach(device -> {
            JSONObject deviceJsonObject = new JSONObject();
            deviceJsonObject.put("ID", device.getDeviceId());
            deviceJsonObject.put("Name", device.getName());
            deviceJsonObject.put("Type", "GB");
            deviceJsonObject.put("ChannelCount", device.getChannelCount());
            deviceJsonObject.put("RecvStreamIP", "");
            deviceJsonObject.put("CatalogInterval", 3600); // Channel directory crawl cycle
            deviceJsonObject.put("SubscribeInterval", device.getSubscribeCycleForCatalog()); // Subscription cycle(seconds), 0 Indicates that the background does not subscribe periodically
            deviceJsonObject.put("Online", device.isOnLine());
            deviceJsonObject.put("Password", "");
            deviceJsonObject.put("MediaTransport", device.getTransport());
            deviceJsonObject.put("RemoteIP", device.getIp());
            deviceJsonObject.put("RemotePort", device.getPort());
            deviceJsonObject.put("LastRegisterAt", "");
            deviceJsonObject.put("LastKeepaliveAt", "");
            deviceJsonObject.put("UpdatedAt", "");
            deviceJsonObject.put("CreatedAt", "");
            deviceJSONList.add(deviceJsonObject);
        });
        result.put("DeviceList",deviceJSONList);
        return result;
    }

    @GetMapping(value = "/channellist")
    public JSONObject channellist( String serial,
                                   @RequestParam(required = false)String channel_type,
                                   @RequestParam(required = false)String code ,
                                   @RequestParam(required = false)String dir_serial ,
                                   @RequestParam(required = false)Integer start,
                                   @RequestParam(required = false)Integer limit,
                                   @RequestParam(required = false)String q,
                                   @RequestParam(required = false)Boolean online ){

        JSONObject result = new JSONObject();
        List<DeviceChannelExtend> deviceChannels;
        List<String> channelIds = null;
        if (!ObjectUtils.isEmpty(code)) {
            String[] split = code.trim().split(",");
            channelIds = Arrays.asList(split);
        }
        List<DeviceChannelExtend> allDeviceChannelList = channelService.queryChannelExtendsByDeviceId(serial,channelIds,online);
        if (start == null || limit ==null) {
            deviceChannels = allDeviceChannelList;
            result.put("ChannelCount", deviceChannels.size());
        }else {
            if (start > allDeviceChannelList.size()) {
                deviceChannels = new ArrayList<>();
            }else {
                if (start + limit < allDeviceChannelList.size()) {
                    deviceChannels = allDeviceChannelList.subList(start, start + limit);
                }else {
                    deviceChannels = allDeviceChannelList.subList(start, allDeviceChannelList.size());
                }
            }
            result.put("ChannelCount", allDeviceChannelList.size());
        }
        JSONArray channleJSONList = new JSONArray();
        deviceChannels.stream().forEach(deviceChannelExtend -> {
            JSONObject deviceJOSNChannel = new JSONObject();
            deviceJOSNChannel.put("ID", deviceChannelExtend.getChannelId());
            deviceJOSNChannel.put("DeviceID", deviceChannelExtend.getDeviceId());
            deviceJOSNChannel.put("DeviceName", deviceChannelExtend.getDeviceName());
            deviceJOSNChannel.put("DeviceOnline", deviceChannelExtend.isDeviceOnline());
            deviceJOSNChannel.put("Channel", 0); // TODO Custom serial number
            deviceJOSNChannel.put("Name", deviceChannelExtend.getName());
            deviceJOSNChannel.put("Custom", false);
            deviceJOSNChannel.put("CustomName", "");
            deviceJOSNChannel.put("SubCount", deviceChannelExtend.getSubCount()); // TODO ? Number of child nodes, SubCount > 0 Indicates that the channel is a subdirectory
            deviceJOSNChannel.put("SnapURL", "");
            deviceJOSNChannel.put("Manufacturer ", deviceChannelExtend.getManufacture());
            deviceJOSNChannel.put("Model", deviceChannelExtend.getModel());
            deviceJOSNChannel.put("Owner", deviceChannelExtend.getOwner());
            deviceJOSNChannel.put("CivilCode", deviceChannelExtend.getCivilCode());
            deviceJOSNChannel.put("Address", deviceChannelExtend.getAddress());
            deviceJOSNChannel.put("Parental", deviceChannelExtend.getParental()); // When it is a channel device, whether there is a channel sub-device, 1-Yes,0-No
            deviceJOSNChannel.put("ParentID", deviceChannelExtend.getParentId()); // Direct superior number
            deviceJOSNChannel.put("Secrecy", deviceChannelExtend.getSecrecy());
            deviceJOSNChannel.put("RegisterWay", 1); // Registration method, default is 1, allowed values: 1, 2, 3
            // 1-IETF RFC3261,
            // 2-Password-based two-way authentication,
            // 3-Digital certificate-based two-way authentication
            deviceJOSNChannel.put("Status", deviceChannelExtend.getStatus());
            deviceJOSNChannel.put("Longitude", deviceChannelExtend.getLongitude());
            deviceJOSNChannel.put("Latitude", deviceChannelExtend.getLatitude());
            deviceJOSNChannel.put("PTZType ", deviceChannelExtend.getPTZType()); // PTZ type, 0 - unknown, 1 - ball machine, 2 - hemisphere,
            //   3 - Fixed bolt, 4 - remote control gun
            deviceJOSNChannel.put("CustomPTZType", "");
            deviceJOSNChannel.put("StreamID", deviceChannelExtend.getStreamId()); // StreamID Live stream ID, a value indicates that the live broadcast is ongoing
            deviceJOSNChannel.put("NumOutputs ", -1); // Number of people online during live broadcast
            channleJSONList.add(deviceJOSNChannel);
        });
        result.put("ChannelList", channleJSONList);
        return result;
    }

    /**
     * Device information - Get the lower channel preset position
     * @param serial Device number
     * @param code Channel number, pass /api/v1/device/channellist obtained ChannelList.ID, You can choose to pass this parameter or channel.
     * @param channel Channel number, default value: 1
     * @param fill Whether to fill the vacant preset positions. When the subordinate returns preset positions but there are not enough 255, the vacant preset positions will be automatically filled to 255. Default value: true, allowed value: true, false
     * @param timeout timeout(seconds) Default value: 15
     * @return
     */
    @GetMapping(value = "/fetchpreset")
    private DeferredResult<WVPResult<Object>>  list(String serial,
                      @RequestParam(required = false)Integer channel,
                      @RequestParam(required = false)String code,
                      @RequestParam(required = false)Boolean fill,
                      @RequestParam(required = false)Integer timeout){

        if (log.isDebugEnabled()) {
            log.debug("<Analog interface> Get lower-level channel preset position API call，deviceId：{} ，channel：{} ，code：{} ，fill：{} ，timeout：{} ",
                    serial, channel, code, fill, timeout);
        }

        Device device = deviceService.getDeviceByDeviceId(serial);
        Assert.notNull(device, "Device does not exist");
        DeferredResult<WVPResult<Object>> deferredResult = new DeferredResult<> (timeout * 1000L);
        deviceService.queryPreset(device, code, (resultCode, msg, data) -> {
            if (resultCode == ErrorCode.SUCCESS.getCode()) {
                List<Preset> presetQuerySipReqList = (List<Preset>)data;
                HashMap<String, Object> resultMap = new HashMap<>();
                resultMap.put("DeviceID", code);
                resultMap.put("Result", "OK");
                resultMap.put("SumNum", presetQuerySipReqList.size());
                ArrayList<Map<String, Object>> presetItemList = new ArrayList<>(presetQuerySipReqList.size());
                for (Preset presetQuerySipReq : presetQuerySipReqList) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("PresetID", presetQuerySipReq.getPresetId());
                    item.put("PresetName", presetQuerySipReq.getPresetName());
                    item.put("PresetEnable", true);
                    presetItemList.add(item);
                }
                resultMap.put("PresetItemList",presetItemList );
                deferredResult.setResult(new WVPResult<>(resultCode, msg, resultMap));
            }else {
                deferredResult.setResult(new WVPResult<>(resultCode, msg, null));
            }
        });

        deferredResult.onTimeout(()->{
            log.warn("[Get device preset position] timeout, {}", device.getDeviceId());
            deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "wait for presetquery timeout["+timeout+"s]"));
        });
        return deferredResult;
    }
}
