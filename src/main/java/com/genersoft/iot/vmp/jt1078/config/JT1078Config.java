package com.genersoft.iot.vmp.jt1078.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "jt1078", ignoreInvalidFields = true)
@Order(3)
public class JT1078Config {

    private Integer port;

    private String password;

    private Boolean record = false;

    /**
     * IDLEStatus timeout time, unit: seconds, default 0 means not enabled. After enabling, the connection will be disconnected when it enters the IDLE state and exceeds this time.
       The conditions for the connection to enter the IDLE state are: no data packets are received within the readerIdleTime time, and no data packets are sent within the writerIdleTime time
     */
    private Integer readerIdleTime = 0;
}
