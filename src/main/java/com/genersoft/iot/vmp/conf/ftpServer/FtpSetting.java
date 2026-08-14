package com.genersoft.iot.vmp.conf.ftpServer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Configuration file user-settings Mapping configuration information
 */
@Component
@ConfigurationProperties(prefix = "ftp", ignoreInvalidFields = true)
@Order(0)
@Data
public class FtpSetting {

    private Boolean enable = Boolean.FALSE;

    private int port = 21;
    private String passivePorts = "10000-10500";
}
