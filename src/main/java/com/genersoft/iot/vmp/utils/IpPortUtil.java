package com.genersoft.iot.vmp.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpPortUtil {

    /**
     * Splice IP and port
     * @param ip IPaddress string
     * @param port port number string
     * @return concatenated string
     * @throws IllegalArgumentException If the IP address is invalid or the port is invalid
     */
    public static String concatenateIpAndPort(String ip, String port) {
        if (port == null || port.isEmpty()) {
            throw new IllegalArgumentException("Port number cannot be empty");
        }

        // Verify that the port is a valid number
        try {
            int portNum = Integer.parseInt(port);
            if (portNum < 0 || portNum > 65535) {
                throw new IllegalArgumentException("The port number must be in0-65535within range");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port number must be a valid number", e);
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(ip);

            if (inetAddress instanceof Inet6Address) {
                // IPv6The address needs to be enclosed in square brackets
                return "[" + ip + "]:" + port;
            } else {
                // IPv4Address direct splicing
                return ip + ":" + port;
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP address: " + ip, e);
        }
    }
}
