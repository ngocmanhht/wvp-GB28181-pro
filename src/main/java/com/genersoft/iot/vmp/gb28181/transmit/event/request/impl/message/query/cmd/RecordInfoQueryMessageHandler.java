package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.record.RecordInfoEventListener;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.QueryMessageHandler;
import com.genersoft.iot.vmp.utils.DateUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RecordInfoQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final String cmdType = "RecordInfo";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IGbChannelPlayService playService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private SIPCommanderForPlatform cmderFroPlatform;

    @Autowired
    private SIPCommander commander;

    @Autowired
    private RecordInfoEventListener recordInfoEventListener;

    @Override
    public void afterPropertiesSet() throws Exception {
        queryMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[Command sending failed] Reply200 OK: {}", e.getMessage());
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform platform, Element rootElement) {

        SIPRequest request = (SIPRequest) evt.getRequest();
        Element snElement = rootElement.element("SN");
        int sn = Integer.parseInt(snElement.getText());
        Element deviceIDElement = rootElement.element("DeviceID");
        String channelId = deviceIDElement.getText();
        Element startTimeElement = rootElement.element("StartTime");
        String startTime = null;
        if (startTimeElement != null) {
            startTime = startTimeElement.getText();
        }
        Element endTimeElement = rootElement.element("EndTime");
        String endTime = null;
        if (endTimeElement != null) {
            endTime = endTimeElement.getText();
        }
        Element secrecyElement = rootElement.element("Secrecy");
        int secrecy = 0;
        if (secrecyElement != null) {
            secrecy = Integer.parseInt(secrecyElement.getText().trim());
        }
        String type = "all";
        Element typeElement = rootElement.element("Type");
        if (typeElement != null) {
            type =  typeElement.getText();
        }

        // Request video data from national standard equipment
        CommonGBChannel channel = channelService.queryOneWithPlatform(platform.getId(), channelId);
        if (channel == null) {
            log.info("[Platform query video records] Channel not found {}/{}", platform.getName(), channelId );
            try {
                responseAck(request, Response.BAD_REQUEST);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] [Platform query video records] Channel not found: {}", e.getMessage());
            }
            return;
        }
        if (channel.getDataType() == ChannelDataType.GB28181) {
            Device device = deviceService.getDevice(channel.getDataDeviceId());
            if (device == null) {
                log.warn("[Platform query video records] The device corresponding to the channel was not found {}/{}", platform.getName(), channelId );
                try {
                    responseAck(request, Response.BAD_REQUEST);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] [Platform query video records] The device corresponding to the channel was not found: {}", e.getMessage());
                }
                return;
            }
            // Get the original information of the channel
            DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
            // Receive video data
            recordInfoEventListener.addEndEventHandler(device.getDeviceId(), deviceChannel.getDeviceId(), (recordInfo)->{
                try {
                    log.info("[National standard cascade] Recording query received data, channel： {}，Ready to forward===", channelId);
                    cmderFroPlatform.recordInfo(channel, platform, request.getFromTag(), recordInfo);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] National standard cascade reply to video data: {}", e.getMessage());
                }
            });
            try {
                commander.recordInfoQuery(device, deviceChannel.getDeviceId(), DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(startTime),
                        DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(endTime), sn, secrecy, type, (eventResult -> {
                            // Reply200 OK
                            try {
                                responseAck(request, Response.OK);
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                log.error("[Command sending failed] Video inquiry reply: {}", e.getMessage());
                            }
                        }),(eventResult -> {
                            // Query failed - Verify the legality of statusCode to prevent illegal status codes from causingIllegalArgumentException
                            try {
                                int statusCode = eventResult.statusCode;
                                if (statusCode < 100 || statusCode > 699) {
                                    log.warn("[Video query failed] Illegal SIP status code received: {}，channel: {}/{}，news: {}，Replaced with500",
                                            statusCode, platform.getName(), channelId, eventResult.msg);
                                    statusCode = Response.SERVER_INTERNAL_ERROR; // 500
                                }
                                responseAck(request, statusCode, eventResult.msg);
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                log.error("[Command sending failed] Video inquiry reply: {}", e.getMessage());
                            } catch (Exception e) {
                                // Catch all exceptions to prevent them from escaping to the Spring scheduling layer and causing infinite retries.
                                log.error("[Video query] Unexpected exception, channel: {}/{}: {}",
                                        platform.getName(), channelId, e.getMessage(), e);
                            }
                        }));
            } catch (InvalidArgumentException | ParseException | SipException e) {
                log.error("[Command sending failed] Video query: {}", e.getMessage());
            }
        }else {
            // Reply200 OK
            try {
                responseAck(request, Response.OK);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[Command sending failed] Video inquiry reply: {}", e.getMessage());
            }

            playService.queryRecord(channel, DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(startTime),
                    DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(endTime),
                    (code, msg, commonRecordInfoList) -> {
                RecordInfo recordInfo = new RecordInfo();
                recordInfo.setSumNum(commonRecordInfoList.size());
                recordInfo.setChannelId(channelId);
                recordInfo.setSn(sn + "");
                List<RecordItem> recordList = new ArrayList<>(commonRecordInfoList.size());
                for (int i = 0; i < commonRecordInfoList.size(); i++) {
                    CommonRecordInfo commonRecordInfo = commonRecordInfoList.get(i);
                    RecordItem recordItem = new RecordItem();
                    recordItem.setDeviceId(channelId);
                    recordItem.setName(commonRecordInfo.getStartTime());
                    recordItem.setFilePath("/" + commonRecordInfo.getStartTime());
                    recordItem.setAddress("/" + commonRecordInfo.getStartTime());
                    recordItem.setStartTime(commonRecordInfo.getStartTime());
                    recordItem.setEndTime(commonRecordInfo.getEndTime());
                    recordItem.setSecrecy(0);
                    recordItem.setRecorderId("");
                    recordItem.setType("");
                    recordItem.setFileSize(commonRecordInfo.getFileSize());
                    recordList.add(recordItem);
                }
                recordInfo.setRecordList(recordList);

                try {
                    log.info("[National standard cascade] Recording query received data, channel： {}，Ready to forward===", channelId);
                    cmderFroPlatform.recordInfo(channel, platform, request.getFromTag(), recordInfo);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[Command sending failed] National standard cascade reply to video data: {}", e.getMessage());
                }
            });
        }
//
//
//
//
//
//
//
//        if (channel.getDataType() != ChannelDataType.GB28181) {
//            log.info("[Platform query video records] Only supports querying the video data of national standard 28181 {}/{}", platform.getName(), channelId );
//            try {
//                responseAck(request, Response.NOT_IMPLEMENTED); // Reply not implemented
//            } catch (SipException | InvalidArgumentException | ParseException e) {
//                log.error("[Command sending failed] Platform query video records: {}", e.getMessage());
//            }
//            return;
//        }

    }
}
