package com.genersoft.iot.vmp.streamPush.enent;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.streamPush.bean.StreamPushExcelDto;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.springframework.util.ObjectUtils;

import java.util.*;

public class StreamPushUploadFileHandler extends AnalysisEventListener<StreamPushExcelDto> {

    /**
     * Error data callback, used to send error data to the page
     */
    private final ErrorDataHandler errorDataHandler;

    /**
     * The push business class is used to store data
     */
    private final IStreamPushService pushService;

    /**
     * Default streaming nodeID
     */
    private final String defaultMediaServerId;

    /**
     * Used to store moreAPP+StreamThe filtered data can be directly stored in the stream_push table and gb_stream table
     */
    private final Map<String, StreamPush> streamPushItemForSave = new HashMap<>();

    /**
     * for storageAPP+Stream->The data structure of the national standard ID, one-to-one data correspondence, global judgmentAPP+Stream->Does the national standard ID exist or not?
     */
    private final BiMap<String,String> gBMap = HashBiMap.create();

    /**
     * for storageAPP+Stream-> data in database
     */
    private final BiMap<String,String> pushMapInDb = HashBiMap.create();

    /**
     * Record wrongAPP+Stream
     */
    private final List<String> errorStreamList = new ArrayList<>();


    /**
     * Recording wrong national standardID
     */
    private final List<String> errorInfoList = new ArrayList<>();

    /**
     * Read quantity counter
     */
    private int loadedSize = 0;

    public StreamPushUploadFileHandler(IStreamPushService pushService, String defaultMediaServerId, ErrorDataHandler errorDataHandler) {
        this.pushService = pushService;
        this.defaultMediaServerId = defaultMediaServerId;
        this.errorDataHandler = errorDataHandler;
        // Get the existing data in the database, ignore the existing data
        List<String> allAppAndStreams = pushService.getAllAppAndStream();
        if (!allAppAndStreams.isEmpty()) {
            for (String allAppAndStream : allAppAndStreams) {
                pushMapInDb.put(allAppAndStream, allAppAndStream);
            }
        }
    }

    public interface ErrorDataHandler{
        void handle(List<String> streams, List<String> gbId);
    }

    @Override
    public void invoke(StreamPushExcelDto streamPushExcelDto, AnalysisContext analysisContext) {
        if (ObjectUtils.isEmpty(streamPushExcelDto.getApp())
                || ObjectUtils.isEmpty(streamPushExcelDto.getStream())
                || ObjectUtils.isEmpty(streamPushExcelDto.getGbDeviceId())) {
            return;
        }
        Integer rowIndex = analysisContext.readRowHolder().getRowIndex();

        if (gBMap.get(streamPushExcelDto.getApp() + streamPushExcelDto.getStream()) == null) {
            try {
                gBMap.put(streamPushExcelDto.getApp() + streamPushExcelDto.getStream(), streamPushExcelDto.getGbDeviceId());
            }catch (IllegalArgumentException e) {
                errorInfoList.add("OK：" + rowIndex + ", " + streamPushExcelDto.getGbDeviceId() + " National standard ID reuse");
                return;
            }
        }else {
            if (!gBMap.get(streamPushExcelDto.getApp() + streamPushExcelDto.getStream()).equals(streamPushExcelDto.getGbDeviceId())) {
                errorInfoList.add("OK：" + rowIndex + ", " + streamPushExcelDto.getGbDeviceId() + " The same application name and stream ID use different national standardsID");
                return;
            }
        }

        StreamPush streamPush = new StreamPush();
        streamPush.setApp(streamPushExcelDto.getApp());
        streamPush.setStream(streamPushExcelDto.getStream());
        streamPush.setGbDeviceId(streamPushExcelDto.getGbDeviceId());
        streamPush.setGbStatus(streamPushExcelDto.isStatus()?"ON":"OFF");
        streamPush.setCreateTime(DateUtil.getNow());
        streamPush.setMediaServerId(defaultMediaServerId);
        streamPush.setGbName(streamPushExcelDto.getName());
        streamPush.setGbLongitude(streamPushExcelDto.getLongitude());
        streamPush.setGbLatitude(streamPushExcelDto.getLatitude());
        streamPush.setUpdateTime(DateUtil.getNow());
        streamPushItemForSave.put(streamPush.getApp() + streamPush.getStream(), streamPush);

        loadedSize ++;
        if (loadedSize > 1000) {
            saveData();
            streamPushItemForSave.clear();
            loadedSize = 0;
        }

    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // The data must also be saved here to ensure that the last remaining data is also stored in the database.
        saveData();
        streamPushItemForSave.clear();
        gBMap.clear();
        errorDataHandler.handle(errorStreamList, errorInfoList);
    }

    private void saveData(){
        if (!streamPushItemForSave.isEmpty()) {
            // Query the database to see if there are duplicatesapp
            pushService.batchAdd(new ArrayList<>(streamPushItemForSave.values()));
        }
    }
}
