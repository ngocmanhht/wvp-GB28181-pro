package com.genersoft.iot.vmp.conf;


import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.storager.dao.CloudRecordServiceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Recording files are deleted regularly
 */
@Slf4j
@Component
public class CloudRecordTimer {

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private CloudRecordServiceMapper cloudRecordServiceMapper;

    /**
     * Regularly query video files to be deleted
     */
//    @Scheduled(fixedRate = 10000) //Executed every five seconds for easy testing
    @Scheduled(cron = "0 0 0 * * ?")   //Executed at 0 o'clock every day
    public void execute(){
        log.info("[Regular cleaning of video files] Start cleaning up expired video files");
        // Get the streaming node configured with assist
        List<MediaServer> mediaServerItemList =  mediaServerService.getAllOnline();
        if (mediaServerItemList.isEmpty()) {
            return;
        }
        long result = 0;
        for (MediaServer mediaServerItem : mediaServerItemList) {

            Calendar lastCalendar = Calendar.getInstance();
            if (mediaServerItem.getRecordDay() > 0) {
                lastCalendar.setTime(new Date());
                // Get the last save date[period, because each node has a date, that is, each node is supported to set a different save date.，
                lastCalendar.add(Calendar.DAY_OF_MONTH, -mediaServerItem.getRecordDay());
                Long lastDate = lastCalendar.getTimeInMillis();

                // Obtain the list of video files before the end of the date. The file list satisfies the requirements of not being collected or saved. These two fields are currently consistent，
                // For the code related to my own business system, you can just use the collect type when you use it.
                List<CloudRecordItem> cloudRecordItemList = cloudRecordServiceMapper.queryRecordListForDelete(lastDate, mediaServerItem.getId());
                if (cloudRecordItemList.isEmpty()) {
                    continue;
                }
                // TODO You can delete the empty expiration date folder later.
                for (CloudRecordItem cloudRecordItem : cloudRecordItemList) {
                    String date = new File(cloudRecordItem.getFilePath()).getParentFile().getName();
                    try {
                        boolean deleteResult = mediaServerService.deleteRecordDirectory(mediaServerItem, cloudRecordItem.getApp(),
                                cloudRecordItem.getStream(), date, cloudRecordItem.getFileName());
                        if (deleteResult) {
                            log.warn("[Regular cleaning of video files] Disk file deleted successfully： {}", cloudRecordItem.getFilePath());
                        }
                    }catch (ControllerException ignored) {}

                }
                result += cloudRecordServiceMapper.deleteList(cloudRecordItemList);
            }
        }
        log.info("[Regular cleaning of video files] Total cleanup{}expired video files", result);
    }
}
