package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelControlService;
import com.genersoft.iot.vmp.gb28181.service.ISourcePTZService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GbChannelControlServiceImpl implements IGbChannelControlService {


    @Autowired
    private Map<String, ISourcePTZService> sourcePTZServiceMap;


    @Override
    public void ptz(CommonGBChannel channel, FrontEndControlCodeForPTZ frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[Universal channel] PTZ control, type： {}， No.：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type： {} Does not support PTZ control", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.ptz(channel, frontEndControlCode, callback);
    }

    @Override
    public void preset(CommonGBChannel channel, FrontEndControlCodeForPreset frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[Universal channel] Preset position control, type： {}， No.：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type： {} Does not support preset position control", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.preset(channel, frontEndControlCode, callback);
    }

    @Override
    public void fi(CommonGBChannel channel, FrontEndControlCodeForFI frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[Universal channel] FIdirective, type： {}， No.：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type： {} Does not support FI instructions", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.fi(channel, frontEndControlCode, callback);
    }

    @Override
    public void tour(CommonGBChannel channel, FrontEndControlCodeForTour frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[Universal channel] cruise command, type： {}， No.：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type： {} Does not support cruise commands", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.tour(channel, frontEndControlCode, callback);
    }

    @Override
    public void scan(CommonGBChannel channel, FrontEndControlCodeForScan frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[Universal channel] Scan command, type： {}， No.：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type： {} Scan command not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.scan(channel, frontEndControlCode, callback);
    }

    @Override
    public void auxiliary(CommonGBChannel channel, FrontEndControlCodeForAuxiliary frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[Universal channel] Auxiliary switch control command, type： {}， No.：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type： {} Does not support auxiliary switch control instructions", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.auxiliary(channel, frontEndControlCode, callback);
    }

    @Override
    public void wiper(CommonGBChannel channel, FrontEndControlCodeForWiper frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[Universal channel] wiper control, type： {}， No.：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type： {} Does not support wiper control", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.wiper(channel, frontEndControlCode, callback);
    }

    @Override
    public void queryPreset(CommonGBChannel channel, ErrorCallback<List<Preset>> callback) {
        log.info("[Universal channel] Preset position query, type： {}， No.：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type： {} Preset position query is not supported", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.queryPreset(channel, callback);
    }

    @Override
    public void dragZoom(CommonGBChannel channel, FrontEndControlCodeForDragZoom frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[Universal channel] Pull frame{} Type： {}， No.：{}", frontEndControlCode.getCode() == 1 ? "Zoom in": "zoom out", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            // Channel data abnormality
            log.error("[On-demand universal channel] Type： {} Does not support pull box control", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.dragZoom(channel, frontEndControlCode, callback);
    }

    @Override
    public void homePosition(CommonGBChannel channel, Boolean enabled, Integer resetTime, Integer presetIndex, ErrorCallback<String> callback) {
        log.info("[Universal channel] Guard bit setting, type： {}， No.：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePTZService sourcePTZService = sourcePTZServiceMap.get(ChannelDataType.PTZ_SERVICE + dataType);
        if (sourcePTZService == null) {
            log.error("[Universal channel] Type： {} Does not support guard bit setting", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourcePTZService.homePosition(channel, enabled, resetTime, presetIndex, callback);
    }
}
