package com.genersoft.iot.vmp.media.zlm.event;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMServerConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * zlm server_startevent
 */
@Setter
@Getter
public class HookZlmServerStartEvent extends ApplicationEvent {

    public HookZlmServerStartEvent(Object source) {
        super(source);
    }

    private MediaServer mediaServer;
    private ZLMServerConfig config;

}
