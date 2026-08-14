package com.genersoft.iot.vmp.streamPush.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.streamPush.bean.BatchRemoveParam;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.streamPush.bean.StreamPushExcelDto;
import com.genersoft.iot.vmp.streamPush.enent.StreamPushUploadFileHandler;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushPlayService;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name  = "Push information management")
@RestController
@Slf4j
@RequestMapping(value = "/api/push")
public class StreamPushController {

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private IStreamPushPlayService streamPushPlayService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private UserSetting userSetting;

    @GetMapping(value = "/list")
    @ResponseBody
    @Operation(summary = "Push list query", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page")
    @Parameter(name = "count", description = "Number of queries per page")
    @Parameter(name = "query", description = "Query content")
    @Parameter(name = "pushing", description = "Whether streaming is being pushed")
    @Parameter(name = "mediaServerId", description = "streaming mediaID")
    public PageInfo<StreamPush> list(@RequestParam(required = false)Integer page,
                                     @RequestParam(required = false)Integer count,
                                     @RequestParam(required = false)String query,
                                     @RequestParam(required = false)Boolean pushing,
                                     @RequestParam(required = false)String mediaServerId ){

        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        if (ObjectUtils.isEmpty(mediaServerId)) {
            mediaServerId = null;
        }
        PageInfo<StreamPush> pushList = streamPushService.getPushList(page, count, query, pushing, mediaServerId);
        return pushList;
    }


    @PostMapping(value = "/remove")
    @ResponseBody
    @Operation(summary = "Delete", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "Application name", required = true)
    public void delete(int id){
        if (streamPushService.delete(id) <= 0){
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @PostMapping(value = "upload")
    @ResponseBody
    public DeferredResult<ResponseEntity<WVPResult<Object>>> uploadChannelFile(@RequestParam(value = "file") MultipartFile file){

        // Process files for up to one hour
        DeferredResult<ResponseEntity<WVPResult<Object>>> result = new DeferredResult<>(60*60*1000L);
        // Video query uses channelId as deviceId query
        String key = DeferredResultHolder.UPLOAD_FILE_CHANNEL;
        String uuid = UUID.randomUUID().toString();
        log.info("Channel import file type: {}",file.getContentType() );
        if (file.isEmpty()) {
            log.warn("Channel import file is empty");
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(-1);
            wvpResult.setMsg("File is empty");
            result.setResult(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wvpResult));
            return result;
        }
        if (file.getContentType() == null) {
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(-1);
            wvpResult.setMsg("File type not recognized");
            result.setResult(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wvpResult));
            return result;
        }
        // Only process one file at a time
        if (resultHolder.exist(key, null)) {
            log.warn("There is already an import task being executed");
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(-1);
            wvpResult.setMsg("There is already an import task being executed");
            result.setResult(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(wvpResult));
            return result;
        }

        resultHolder.put(key, uuid, result);
        result.onTimeout(()->{
            log.warn("Channel import timed out, the file may be too large");
            RequestMessage msg = new RequestMessage();
            msg.setKey(key);
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(-1);
            wvpResult.setMsg("Import timed out, the file may be too large");
            msg.setData(wvpResult);
            resultHolder.invokeAllResult(msg);
        });
        //Get file stream
        InputStream inputStream = null;
        try {
            String name = file.getName();
            inputStream = file.getInputStream();
        } catch (IOException e) {
            log.error("unhandled exception ", e);
        }
        try {
            //Pass in parameters
            ExcelReader excelReader = EasyExcel.read(inputStream, StreamPushExcelDto.class,
                    new StreamPushUploadFileHandler(streamPushService, mediaServerService.getDefaultMediaServer().getId(), (errorStreams, errorGBs)->{
                        log.info("Channel import successful, duplicates existApp+Streamfor{}, there is a national standard ID of{}a", errorStreams.size(), errorGBs.size());
                        RequestMessage msg = new RequestMessage();
                        msg.setKey(key);
                        WVPResult<Map<String, List<String>>> wvpResult = new WVPResult<>();
                        if (errorStreams.isEmpty() && errorGBs.isEmpty()) {
                            wvpResult.setCode(0);
                            wvpResult.setMsg("success");
                        }else {
                            wvpResult.setCode(1);
                            wvpResult.setMsg("Import successful. But there is duplicate data");
                            Map<String, List<String>> errorData = new HashMap<>();
                            errorData.put("gbId", errorGBs);
                            errorData.put("stream", errorStreams);
                            wvpResult.setData(errorData);
                        }
                        msg.setData(wvpResult);
                        resultHolder.invokeAllResult(msg);
                    })).build();
            ReadSheet readSheet = EasyExcel.readSheet(0).build();
            excelReader.read(readSheet);
            excelReader.finish();
        }catch (ExcelDataConvertException e) {
            log.error("Channel import failed: line： {}， Column： {}, content： {}", e.getRowIndex(), e.getColumnIndex(), e.getCellData().getStringValue());
            RequestMessage msg = new RequestMessage();
            msg.setKey(key);
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("Data anomaly: " + e.getRowIndex() +"OK" + e.getColumnIndex() + "column, content：" + e.getCellData().getStringValue() );
            msg.setData(wvpResult);
            resultHolder.invokeAllResult(msg);
        }catch (Exception e) {
            log.warn("Channel import failed：", e);
            RequestMessage msg = new RequestMessage();
            msg.setKey(key);
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("Channel import failed: " + e.getMessage() );
            msg.setData(wvpResult);
            resultHolder.invokeAllResult(msg);
        }


        return result;
    }

    /**
     * Add push information
     * @param stream push information
     * @return
     */
    @PostMapping(value = "/add")
    @ResponseBody
    @Operation(summary = "Add push information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public StreamPush add(@RequestBody StreamPush stream){
        if (ObjectUtils.isEmpty(stream.getGbId())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "National standard ID cannot be empty");
        }
        if (ObjectUtils.isEmpty(stream.getApp()) && ObjectUtils.isEmpty(stream.getStream())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "appor stream cannot be empty");
        }
        stream.setGbStatus("OFF");
        stream.setPushing(false);
        if (!streamPushService.add(stream)) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
        stream.setDataType(ChannelDataType.STREAM_PUSH);
        stream.setDataDeviceId(stream.getId());
        return stream;
    }

    @PostMapping(value = "/update")
    @ResponseBody
    @Operation(summary = "Update push information", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public void update(@RequestBody StreamPush stream){
        if (ObjectUtils.isEmpty(stream.getId())) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "IDCannot be empty");
        }
        if (!streamPushService.update(stream)) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @DeleteMapping(value = "/batchRemove")
    @ResponseBody
    @Operation(summary = "Delete multiple streams", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public void batchStop(@RequestBody BatchRemoveParam ids){
        if(ids.getIds().isEmpty()) {
            return;
        }
        streamPushService.batchRemove(ids.getIds());
    }

    @GetMapping(value = "/start")
    @ResponseBody
    @Operation(summary = "Start playing", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public DeferredResult<WVPResult<StreamContent>> start(HttpServletRequest request, Integer id){
        Assert.notNull(id, "The push ID cannot beNULL");
        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        result.onTimeout(()->{
            WVPResult<StreamContent> fail = WVPResult.fail(ErrorCode.ERROR100.getCode(), "Timeout waiting for push stream");
            result.setResult(fail);
        });
        streamPushPlayService.start(id, (code, msg, streamInfo) -> {
            if (code == 0 && streamInfo != null) {
                if (userSetting.getUseSourceIpAsStreamIp()) {
                    streamInfo=streamInfo.clone();//deep copy
                    String host;
                    try {
                        URL url=new URL(request.getRequestURL().toString());
                        host=url.getHost();
                    } catch (MalformedURLException e) {
                        host=request.getLocalAddr();
                    }
                    streamInfo.changeStreamIp(host);
                }
                WVPResult<StreamContent> success = WVPResult.success(new StreamContent(streamInfo));
                result.setResult(success);
            }
        }, null, null);
        return result;
    }

    @GetMapping(value = "/forceClose")
    @ResponseBody
    @Operation(summary = "Forcibly stop streaming", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public void stop(String app, String stream){

        streamPushPlayService.stop(app, stream);
    }
}
