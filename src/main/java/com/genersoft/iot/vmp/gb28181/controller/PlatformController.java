package com.genersoft.iot.vmp.gb28181.controller;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.PlatformChannel;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeHolder;
import com.genersoft.iot.vmp.gb28181.controller.bean.UpdateChannelParam;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Cascade platform management
 */
@Tag(name  = "Cascade platform management")
@Slf4j
@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    private SipConfig sipConfig;

	@Autowired
	private IPlatformService platformService;


    @Operation(summary = "Get the configuration of the national standard service", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping("/server_config")
    public JSONObject serverConfig() {
        JSONObject result = new JSONObject();
        result.put("deviceIp", sipConfig.getShowIp());
        result.put("devicePort", sipConfig.getPort());
        result.put("username", sipConfig.getId());
        result.put("password", sipConfig.getPassword());
        return result;
    }

    @Operation(summary = "Get cascade server information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "Platform national standard number", required = true)
    @GetMapping("/info/{id}")
    public Platform getPlatform(@PathVariable String id) {
        Platform parentPlatform = platformService.queryPlatformByServerGBId(id);
        if (parentPlatform != null) {
            return  parentPlatform;
        } else {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "This platform was not found");
        }
    }

    @GetMapping("/query")
    @Operation(summary = "Paginated query cascade platform", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page")
    @Parameter(name = "count", description = "Number of queries per page")
    @Parameter(name = "query", description = "Query content")
    public PageInfo<Platform> platforms(int page, int count,
                                        @RequestParam(required = false) String query) {

        PageInfo<Platform> parentPlatformPageInfo = platformService.queryPlatformList(page, count, query);
        if (parentPlatformPageInfo != null && !parentPlatformPageInfo.getList().isEmpty()) {
            for (Platform platform : parentPlatformPageInfo.getList()) {
                platform.setMobilePositionSubscribe(subscribeHolder.getMobilePositionSubscribe(platform.getServerGBId()) != null);
                platform.setCatalogSubscribe(subscribeHolder.getCatalogSubscribe(platform.getServerGBId()) != null);
            }
        }
        return parentPlatformPageInfo;
    }

    @Operation(summary = "Add parent platform information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/add")
    @ResponseBody
    public void add(@RequestBody Platform platform) {

        Assert.notNull(platform.getName(), "Platform name cannot be empty");
        Assert.notNull(platform.getServerGBId(), "The national standard number of the superior platform cannot be empty");
        Assert.notNull(platform.getServerIp(), "The upper-level platform IP cannot be empty");
        Assert.isTrue(platform.getServerPort() > 0 && platform.getServerPort() < 65535, "Upper-level platform port abnormality");
        Assert.notNull(platform.getDeviceGBId(), "The national standard number of this platform cannot be empty");

        if (ObjectUtils.isEmpty(platform.getServerGBDomain())) {
            platform.setServerGBDomain(platform.getServerGBId().substring(0, 6));
        }

        if (platform.getExpires() <= 0) {
            platform.setExpires(3600);
        }

        if (platform.getKeepTimeout() <= 0) {
            platform.setKeepTimeout(60);
        }

        if (ObjectUtils.isEmpty(platform.getTransport())) {
            platform.setTransport("UDP");
        }

        if (ObjectUtils.isEmpty(platform.getCharacterSet())) {
            platform.setCharacterSet("GB2312");
        }

        Platform parentPlatformOld = platformService.queryPlatformByServerGBId(platform.getServerGBId());
        if (parentPlatformOld != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "platform " + platform.getServerGBId() + " Already exists");
        }
        platform.setCreateTime(DateUtil.getNow());
        platform.setUpdateTime(DateUtil.getNow());
        boolean updateResult = platformService.add(platform);

        if (!updateResult) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "Update parent platform information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/update")
    @ResponseBody
    public void updatePlatform(@RequestBody Platform parentPlatform) {

        if (ObjectUtils.isEmpty(parentPlatform.getName())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBId())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBDomain())
                || ObjectUtils.isEmpty(parentPlatform.getServerIp())
                || ObjectUtils.isEmpty(parentPlatform.getServerPort())
                || ObjectUtils.isEmpty(parentPlatform.getDeviceGBId())
                || ObjectUtils.isEmpty(parentPlatform.getExpires())
                || ObjectUtils.isEmpty(parentPlatform.getKeepTimeout())
                || ObjectUtils.isEmpty(parentPlatform.getTransport())
                || ObjectUtils.isEmpty(parentPlatform.getCharacterSet())
        ) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        platformService.update(parentPlatform);
    }

    @Operation(summary = "Delete parent platform", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "Superior platformID")
    @DeleteMapping("/delete")
    @ResponseBody
    public WVPResult<?> deletePlatform(Integer id) {

        if (log.isDebugEnabled()) {
            log.debug("Delete upper-level platform API call");
        }
        boolean result = platformService.delete(id);
        if (result) {
            return WVPResult.success();
        }else {
            return  WVPResult.fail(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "Check whether the upper-level platform exists", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "serverGBId", description = "National standard number of the superior platform")
    @GetMapping("/exit/{serverGBId}")
    @ResponseBody
    public Boolean exitPlatform(@PathVariable String serverGBId) {
        Platform platform = platformService.queryPlatformByServerGBId(serverGBId);
        return platform != null;
    }

    @Operation(summary = "Paging query for all channels of the cascade platform", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of items per page", required = true)
    @Parameter(name = "platformId", description = "Data from the upper level platformID")
    @Parameter(name = "channelType", description = "Channel type, 0: national standard equipment, 1: push device, 2: pull agent")
    @Parameter(name = "query", description = "Query content")
    @Parameter(name = "online", description = "Is online")
    @Parameter(name = "hasShare", description = "Has it been shared?")
    @GetMapping("/channel/list")
    @ResponseBody
    public PageInfo<PlatformChannel> queryChannelList(int page, int count,
                                                      @RequestParam(required = false) Integer platformId,
                                                      @RequestParam(required = false) String query,
                                                      @RequestParam(required = false) Integer channelType,
                                                      @RequestParam(required = false) Boolean online,
                                                      @RequestParam(required = false) Boolean hasShare) {

        Assert.notNull(platformId, "The data ID of the upper-level platform cannot beNULL");
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }

        return platformChannelService.queryChannelList(page, count, query, channelType,  online, platformId, hasShare);
    }

    @Operation(summary = "Add national standard channels to the upper level platform", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/channel/add")
    @ResponseBody
    public void addChannel(@RequestBody UpdateChannelParam param) {

        if (log.isDebugEnabled()) {
            log.debug("Add national standard channel API calls to the upper-level platform");
        }
        int result = 0;
        if (param.getChannelIds() == null || param.getChannelIds().isEmpty()) {
            if (param.isAll()) {
                log.info("[National standard cascade]Add all channels to the upper level platform， {}", param.getPlatformId());
                result = platformChannelService.addAllChannel(param.getPlatformId());
            }
        }else {
            result = platformChannelService.addChannels(param.getPlatformId(), param.getChannelIds());
        }
        if (result <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "Remove the national standard channel from the upper-level platform", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @DeleteMapping("/channel/remove")
    @ResponseBody
    public void delChannelForGB(@RequestBody UpdateChannelParam param) {

        if (log.isDebugEnabled()) {
            log.debug("Delete the national standard channel API call to the superior platform");
        }
        int result = 0;
        if (param.getChannelIds() == null || param.getChannelIds().isEmpty()) {
            if (param.isAll()) {
                log.info("[National standard cascade]Remove all channels, upper level platform， {}", param.getPlatformId());
                result = platformChannelService.removeAllChannel(param.getPlatformId());
            }
        }else {
            result = platformChannelService.removeChannels(param.getPlatformId(), param.getChannelIds());
        }
        if (result <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @Operation(summary = "push channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "platformID", required = true)
    @GetMapping("/channel/push")
    @ResponseBody
    public void pushChannel(Integer id) {
        Assert.notNull(id, "Platform ID cannot be empty");
        platformChannelService.pushChannel(id);
    }

    @Operation(summary = "add channel-via device", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/channel/device/add")
    @ResponseBody
    public void addChannelByDevice(@RequestBody UpdateChannelParam param) {
        Assert.notNull(param.getPlatformId(), "Platform ID cannot be empty");
        Assert.notNull(param.getDeviceIds(), "Device ID cannot be empty");
        Assert.notEmpty(param.getDeviceIds(), "Device ID cannot be empty");
        platformChannelService.addChannelByDevice(param.getPlatformId(), param.getDeviceIds());
    }

    @Operation(summary = "Remove channel-via device", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/channel/device/remove")
    @ResponseBody
    public void removeChannelByDevice(@RequestBody UpdateChannelParam param) {
        Assert.notNull(param.getPlatformId(), "Platform ID cannot be empty");
        Assert.notNull(param.getDeviceIds(), "Device ID cannot be empty");
        Assert.notEmpty(param.getDeviceIds(), "Device ID cannot be empty");
        platformChannelService.removeChannelByDevice(param.getPlatformId(), param.getDeviceIds());
    }

    @Operation(summary = "Custom shared channel information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @PostMapping("/channel/custom/update")
    @ResponseBody
    public void updateCustomChannel(@RequestBody PlatformChannel channel) {
        Assert.isTrue(channel.getId() > 0, "Shared channel ID must exist");
        platformChannelService.updateCustomChannel(channel);
    }
}
