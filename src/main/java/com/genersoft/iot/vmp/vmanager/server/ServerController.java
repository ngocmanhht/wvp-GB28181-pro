package com.genersoft.iot.vmp.vmanager.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.SystemAllInfo;
import com.genersoft.iot.vmp.common.VersionPo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.VersionInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.jt1078.config.JT1078Config;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerChangeEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMapService;
import com.genersoft.iot.vmp.service.bean.MediaServerLoad;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.vmanager.bean.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

@SuppressWarnings("rawtypes")
@Tag(name = "Service control")
@Slf4j
@RestController
@RequestMapping("/api/server")
public class ServerController {


    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private VersionInfo versionInfo;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private JT1078Config jt1078Config;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService channelService;

    @Autowired
    private IStreamPushService pushService;

    @Autowired
    private IStreamProxyService proxyService;


    @Autowired(required = false)
    private IMapService mapService;

    @Value("${server.port}")
    private int serverPort;


    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;


    @GetMapping(value = "/media_server/list")
    @ResponseBody
    @Operation(summary = "List of streaming services", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<MediaServer> getMediaServerList() {
        return mediaServerService.getAll();
    }

    @GetMapping(value = "/media_server/online/list")
    @ResponseBody
    @Operation(summary = "List of online streaming services", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<MediaServer> getOnlineMediaServerList() {
        return mediaServerService.getAllOnline();
    }

    @GetMapping(value = "/media_server/one/{id}")
    @ResponseBody
    @Operation(summary = "Stop video playback", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "streaming servicesID", required = true)
    public MediaServer getMediaServer(@PathVariable String id) {
        return mediaServerService.getOne(id);
    }

    @Operation(summary = "Test streaming services", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "ip", description = "streaming servicesIP", required = true)
    @Parameter(name = "port", description = "Streaming media service HTTP port", required = true)
    @Parameter(name = "secret", description = "streaming servicessecret", required = true)
    @GetMapping(value = "/media_server/check")
    @ResponseBody
    public MediaServer checkMediaServer(@RequestParam String ip, @RequestParam int port, @RequestParam String secret, @RequestParam String type) {
        return mediaServerService.checkMediaServer(ip, port, secret, type);
    }

    @Operation(summary = "Test streaming video management service", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "ip", description = "streaming servicesIP", required = true)
    @Parameter(name = "port", description = "Streaming media service HTTP port", required = true)
    @GetMapping(value = "/media_server/record/check")
    @ResponseBody
    public void checkMediaRecordServer(@RequestParam String ip, @RequestParam int port) {
        boolean checkResult = mediaServerService.checkMediaRecordServer(ip, port);
        if (!checkResult) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Connection failed");
        }
    }

    @Operation(summary = "Save streaming services", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "mediaServerItem", description = "Streaming media information", required = true)
    @PostMapping(value = "/media_server/save")
    @ResponseBody
    public void saveMediaServer(@RequestBody MediaServer mediaServer) {
        MediaServer mediaServerItemInDatabase = mediaServerService.getOneFromDatabase(mediaServer.getId());

        if (mediaServerItemInDatabase != null) {
            mediaServerService.update(mediaServer);
        } else {
            mediaServerService.add(mediaServer);
            // Send event
            MediaServerChangeEvent event = new MediaServerChangeEvent(this);
            event.setMediaServerItemList(mediaServer);
            applicationEventPublisher.publishEvent(event);
        }
    }

    @Operation(summary = "Remove streaming services", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "streaming mediaID", required = true)
    @DeleteMapping(value = "/media_server/delete")
    @ResponseBody
    public void deleteMediaServer(@RequestParam String id) {
        MediaServer mediaServer = mediaServerService.getOne(id);
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Streaming media does not exist");
        }
        mediaServerService.delete(mediaServer);
    }

    @Operation(summary = "Get flow information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "Application name", required = true)
    @Parameter(name = "stream", description = "flowID", required = true)
    @Parameter(name = "mediaServerId", description = "streaming mediaID", required = true)
    @GetMapping(value = "/media_server/media_info")
    @ResponseBody
    public MediaInfo getMediaInfo(String app, String stream, String mediaServerId) {
        MediaServer mediaServer = mediaServerService.getOneFromCluster(mediaServerId);
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Streaming media does not exist");
        }
        return mediaServerService.getMediaInfo(mediaServer, app, stream);
    }


    @Operation(summary = "Close service", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping(value = "/shutdown")
    @ResponseBody
    public void shutdown() {
        log.info("Closing service。。。");
        System.exit(1);
    }

    @Operation(summary = "Get system configuration information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping(value = "/system/configInfo")
    @ResponseBody
    public SystemConfigInfo getConfigInfo() {
        SystemConfigInfo systemConfigInfo = new SystemConfigInfo();
        systemConfigInfo.setVersion(versionInfo.getVersion());
        systemConfigInfo.setSip(sipConfig);
        systemConfigInfo.setAddOn(userSetting);
        systemConfigInfo.setServerPort(serverPort);
        systemConfigInfo.setJt1078Config(jt1078Config);
        return systemConfigInfo;
    }

    @Operation(summary = "Get version information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping(value = "/version")
    @ResponseBody
    public VersionPo VersionPogetVersion() {
        return versionInfo.getVersion();
    }

    @GetMapping(value = "/config")
    @Operation(summary = "Get configuration information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "type", description = "Configuration type（sip, base）", required = true)
    @ResponseBody
    public JSONObject getVersion(String type) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("server.port", serverPort);
        if (ObjectUtils.isEmpty(type)) {
            jsonObject.put("sip", JSON.toJSON(sipConfig));
            jsonObject.put("base", JSON.toJSON(userSetting));
        } else {
            switch (type) {
                case "sip":
                    jsonObject.put("sip", sipConfig);
                    break;
                case "base":
                    jsonObject.put("base", userSetting);
                    break;
                default:
                    break;
            }
        }
        return jsonObject;
    }

    @GetMapping(value = "/system/info")
    @ResponseBody
    @Operation(summary = "Get system information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public SystemAllInfo getSystemInfo() {
        SystemAllInfo systemAllInfo = redisCatchStorage.getSystemInfo();

        return systemAllInfo;
    }

    @GetMapping(value = "/media_server/load")
    @ResponseBody
    @Operation(summary = "Get load information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<MediaServerLoad> getMediaLoad() {
        List<MediaServerLoad> result = new ArrayList<>();
        List<MediaServer> allOnline = mediaServerService.getAllOnline();
        if (allOnline.isEmpty()) {
            return result;
        } else {
            for (MediaServer mediaServerItem : allOnline) {
                result.add(mediaServerService.getLoad(mediaServerItem));
            }
        }
        return result;
    }

    @GetMapping(value = "/resource/info")
    @ResponseBody
    @Operation(summary = "Get load information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public ResourceInfo getResourceInfo() {
        ResourceInfo result = new ResourceInfo();
        ResourceBaseInfo deviceInfo = deviceService.getOverview();
        result.setDevice(deviceInfo);
        ResourceBaseInfo channelInfo = channelService.getOverview();
        result.setChannel(channelInfo);
        ResourceBaseInfo pushInfo = pushService.getOverview();
        result.setPush(pushInfo);
        ResourceBaseInfo proxyInfo = proxyService.getOverview();
        result.setProxy(proxyInfo);

        return result;
    }

    @GetMapping(value = "/info")
    @ResponseBody
    @Operation(summary = "Get system information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public Map<String, Map<String, String>> getInfo(HttpServletRequest request) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        Map<String, String> hardwareMap = new LinkedHashMap<>();
        result.put("Hardware information", hardwareMap);

        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        // Get CPU information
        CentralProcessor.ProcessorIdentifier processorIdentifier = hardware.getProcessor().getProcessorIdentifier();
        hardwareMap.put("CPU", processorIdentifier.getName());
        // Get memory
        GlobalMemory memory = hardware.getMemory();
        hardwareMap.put("memory", formatByte(memory.getTotal() - memory.getAvailable()) + "/" + formatByte(memory.getTotal()));
        hardwareMap.put("manufacturer", systemInfo.getHardware().getComputerSystem().getManufacturer());
        hardwareMap.put("Product name", systemInfo.getHardware().getComputerSystem().getModel());
        // network card
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        StringBuilder ips = new StringBuilder();
        for (int i = 0; i < networkIFs.size(); i++) {
            NetworkIF networkIF = networkIFs.get(i);
            String ipsStr = StringUtils.join(networkIF.getIPv4addr());
            if (ObjectUtils.isEmpty(ipsStr)) {
                continue;
            }
            ips.append(ipsStr);
            if (i < networkIFs.size() - 1) {
                ips.append(",");
            }
        }
        hardwareMap.put("network card", ips.toString());

        Map<String, String> operatingSystemMap = new LinkedHashMap<>();
        result.put("operating system", operatingSystemMap);
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        operatingSystemMap.put("Name", operatingSystem.getFamily() + " " + operatingSystem.getVersionInfo().getVersion());
        operatingSystemMap.put("Type", operatingSystem.getManufacturer());

        Map<String, String> platformMap = new LinkedHashMap<>();
        result.put("Platform information", platformMap);
        VersionPo version = versionInfo.getVersion();
        platformMap.put("version", version.getVersion());
        platformMap.put("Build date", version.getBUILD_DATE());
        platformMap.put("GITbranch", version.getGIT_BRANCH());
        platformMap.put("GITaddress", version.getGIT_URL());
        platformMap.put("GITDate", version.getGIT_DATE());
        platformMap.put("GITversion", version.getGIT_Revision_SHORT());
        platformMap.put("DOCKERenvironment", new File("/.dockerenv").exists()?"Yes":"No");

        Map<String, String> docmap = new LinkedHashMap<>();
        result.put("Document address", docmap);
        docmap.put("Deployment documentation", "https://doc.wvp-pro.cn");
        docmap.put("Interface documentation", String.format("%s://%s:%s/doc.html", request.getScheme(), request.getServerName(), request.getServerPort()));


        return result;
    }

    /**
     * Unit conversion
     */
    private static String formatByte(long byteNumber) {
        //Conversion unit
        double FORMAT = 1024.0;
        double kbNumber = byteNumber / FORMAT;
        if (kbNumber < FORMAT) {
            return new DecimalFormat("#.##KB").format(kbNumber);
        }
        double mbNumber = kbNumber / FORMAT;
        if (mbNumber < FORMAT) {
            return new DecimalFormat("#.##MB").format(mbNumber);
        }
        double gbNumber = mbNumber / FORMAT;
        if (gbNumber < FORMAT) {
            return new DecimalFormat("#.##GB").format(gbNumber);
        }
        double tbNumber = gbNumber / FORMAT;
        return new DecimalFormat("#.##TB").format(tbNumber);
    }

    @GetMapping(value = "/map/config")
    @ResponseBody
    @Operation(summary = "Get map configuration", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<MapConfig> getMapConfig() {
        if (mapService == null) {
            return Collections.emptyList();
        }
        return mapService.getConfig();
    }

    @GetMapping(value = "/map/model-icon/list")
    @ResponseBody
    @Operation(summary = "Get map configuration icon", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<MapModelIcon> getMapModelIconList() {
        if (mapService == null) {
            return Collections.emptyList();
        }
        return mapService.getModelList();
    }
}
