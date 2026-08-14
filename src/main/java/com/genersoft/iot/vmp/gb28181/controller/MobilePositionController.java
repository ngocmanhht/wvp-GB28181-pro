package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.service.IMobilePositionService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.util.StringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

/**
 *  Location information management
 */
@Tag(name  = "Location information management")
@Slf4j
@RestController
@RequestMapping("/api/position")
public class MobilePositionController {

    @Autowired
    private IMobilePositionService mobilePositionService;

	@Autowired
	private ISIPCommander cmder;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IDeviceService deviceService;


    @Operation(summary = "Query historical track", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channel databaseID")
    @Parameter(name = "start", description = "start time")
    @Parameter(name = "end", description = "end time")
    @GetMapping("/history/{deviceId}")
    public List<MobilePosition> positions( Integer channelId,
                                           @RequestParam(required = false) String start,
                                           @RequestParam(required = false) String end) {

        if (StringUtil.isEmpty(start)) {
            start = null;
        }
        if (StringUtil.isEmpty(end)) {
            end = null;
        }
        return mobilePositionService.queryMobilePositions(channelId, start, end);
    }

    @Operation(summary = "Query the latest position of the channel", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "channel databaseID", required = true)
    @GetMapping("/latest")
    public MobilePosition latestPosition(Integer channelId) {
        return mobilePositionService.queryLatestPosition(channelId);
    }

    /**
     *  Get mobile location information
     * @param deviceId EquipmentID
     * @return
     */
    @Operation(summary = "Get mobile location information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
    @GetMapping("/realtime/{deviceId}")
    public DeferredResult<MobilePosition> realTimePosition(@PathVariable String deviceId) {
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        String uuid = UUID.randomUUID().toString();
        String key = DeferredResultHolder.CALLBACK_CMD_MOBILE_POSITION + deviceId;
        try {
            cmder.mobilePositionQuery(device, event -> {
                RequestMessage msg = new RequestMessage();
                msg.setId(uuid);
                msg.setKey(key);
                msg.setData(String.format("Failed to obtain mobile location information, error code： %s, %s", event.statusCode, event.msg));
                resultHolder.invokeResult(msg);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[Command sending failed] Get mobile location information: {}", e.getMessage());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Command sending failed: " + e.getMessage());
        }
        DeferredResult<MobilePosition> result = new DeferredResult<MobilePosition>(5*1000L);
		result.onTimeout(()->{
			log.warn(String.format("Timeout for obtaining mobile location information"));
			// releasertpserver
			RequestMessage msg = new RequestMessage();
            msg.setId(uuid);
            msg.setKey(key);
			msg.setData("Timeout");
			resultHolder.invokeResult(msg);
		});
        resultHolder.put(key, uuid, result);
        return result;
    }

    /**
     * Subscribe to location information
     * @param deviceId EquipmentID
     * @param expires Subscription timeout
     * @param interval Reporting interval
     * @return true = Command sent successfully
     */
    @Operation(summary = "Subscribe to location information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "deviceId", description = "Equipment national standard number", required = true)
    @Parameter(name = "expires", description = "Subscription timeout", required = true)
    @Parameter(name = "interval", description = "Reporting interval", required = true)
    @GetMapping("/subscribe/{deviceId}")
    public void positionSubscribe(@PathVariable String deviceId,
                                                    @RequestParam String expires,
                                                    @RequestParam String interval) {

        if (StringUtil.isEmpty(interval)) {
            interval = "5";
        }
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        device.setSubscribeCycleForMobilePosition(Integer.parseInt(expires));
        device.setMobilePositionSubmissionInterval(Integer.parseInt(interval));
        deviceService.updateCustomDevice(device);
    }
}
