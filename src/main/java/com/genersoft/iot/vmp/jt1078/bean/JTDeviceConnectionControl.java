package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * JT terminal control
 */
@Data
@Schema(description = "terminal control")
public class JTDeviceConnectionControl {

    /**
     * false means switching to the specified monitoring platform server, true means switching back to the original default monitoring platform server
     */
    private Boolean switchOn;
    /**
     * Supervision platform authentication code
     */
    private String authentication;

    /**
     * dial point name
     */
    private String name;

    /**
     * Dial-up username
     */
    private String username;

    /**
     * Dial-up password
     */
    private String password;

    /**
     * address
     */
    private String address;

    /**
     * TCPport
     */
    private Integer tcpPort;

    /**
     * UDPport
     */
    private Integer udpPort;

    /**
     * Time limit for connecting to the specified server
     */
    private Long timeLimit;

    @Override
    public String toString() {
        return "JTDeviceConnectionControl{" +
                "switchOn=" + switchOn +
                ", authentication='" + authentication + '\'' +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", address='" + address + '\'' +
                ", tcpPort=" + tcpPort +
                ", udpPort=" + udpPort +
                ", timeLimit=" + timeLimit +
                '}';
    }
}
