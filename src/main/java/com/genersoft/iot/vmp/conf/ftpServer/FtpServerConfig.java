package com.genersoft.iot.vmp.conf.ftpServer;

import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.*;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(value = "ftp.enable", havingValue = "true")
@Slf4j
public class FtpServerConfig {

    @Autowired
    private UserManager userManager;

    @Autowired
    private FtpFileSystemFactory fileSystemFactory;

    @Autowired
    private Ftplet ftplet;

    @Autowired
    private FtpSetting ftpSetting;

    /**
     * ftp server init
     */
    @Bean
    public FtpServer ftpServer() {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory listenerFactory = new ListenerFactory();
        // 1、Set service port
        listenerFactory.setPort(ftpSetting.getPort());
        // 2、Set the interface range for passive mode data upload. The cloud server needs to open the port in the corresponding range to the client.
        DataConnectionConfigurationFactory dataConnectionConfFactory = new DataConnectionConfigurationFactory();
        dataConnectionConfFactory.setPassivePorts(ftpSetting.getPassivePorts());
        listenerFactory.setDataConnectionConfiguration(dataConnectionConfFactory.createDataConnectionConfiguration());
        // 4、Replace the default listener
        Listener listener = listenerFactory.createListener();
        serverFactory.addListener("default", listener);
        // 5、Configure custom user events
        Map<String, org.apache.ftpserver.ftplet.Ftplet> ftpLets = new HashMap<>();
        ftpLets.put("ftpService", ftplet);
        serverFactory.setFtplets(ftpLets);
        // 6、Read user configuration information
        // 6.2、Setting information
        serverFactory.setUserManager(userManager);
        serverFactory.setFileSystem(fileSystemFactory);
        // 7、InstantiateFTP Server
        FtpServer server = serverFactory.createServer();
        try {
            server.start();
            if (!server.isStopped()) {
                log.info("[FTPservice] started, port： {}", ftpSetting.getPort());
            }
        } catch (FtpException e) {
            log.info("[FTPservice] Startup failed ", e);
        }
        return server;
    }
}
