package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;

import java.util.List;

/**
 * Resource capability access-Others
 */
public interface ISourceOtherService {


    Boolean closeStreamOnNoneReader(String mediaServerId, String app, String stream, String schema);

    Boolean addChannelIdForMobilePosition(List<? extends MobilePosition> mobilePositionList);

}
