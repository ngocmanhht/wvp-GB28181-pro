package com.genersoft.iot.vmp.gb28181.event.subscribe.catalog;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Setter
@Getter
public class CatalogEvent  extends ApplicationEvent {

    public CatalogEvent(Object source) {
        super(source);
    }

    /**
     * Go online
     */
    public static final String ON = "ON";

    /**
     * Offline
     */
    public static final String OFF = "OFF";

    /**
     * Video lost
     */
    public static final String VLOST = "VLOST";

    /**
     * Failure
     */
    public static final String DEFECT = "DEFECT";

    /**
     * increase
     */
    public static final String ADD = "ADD";

    /**
     * Delete
     */
    public static final String DEL = "DEL";

    /**
     * update
     */
    public static final String UPDATE = "UPDATE";

    private List<CommonGBChannel> channels;

    private String type;

    private Platform platform;

}
