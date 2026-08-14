package com.genersoft.iot.vmp.conf;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServiceInfo implements ApplicationListener<WebServerInitializedEvent> {

    @Getter
    private static int serverPort;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        // Project startup to obtain the startup port number
        ServiceInfo.serverPort = event.getWebServer().getPort();
        log.info("Project startup to obtain the startup port number:  {}", ServiceInfo.serverPort);
    }

    public void setServerPort(int serverPort) {
        ServiceInfo.serverPort = serverPort;
    }
}
