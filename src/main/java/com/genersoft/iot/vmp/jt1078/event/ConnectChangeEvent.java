package com.genersoft.iot.vmp.jt1078.event;

import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;
import java.time.Clock;

/**
 * Link disconnection or connection events
 */

@Setter
@Getter
public class ConnectChangeEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConnectChangeEvent(Object source) {
        super(source);
    }


    private boolean connected;

    private String phoneNumber;

}
