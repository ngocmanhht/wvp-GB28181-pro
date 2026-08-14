package com.genersoft.iot.vmp.gb28181.conf;

import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.notify.cmd.AlarmNotifyMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Get sip default configuration
 * @author lin
 */
public class DefaultProperties {

    public static Properties getProperties(String name, boolean sipLog, boolean sipCacheServerConnections) {
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", name);
//        properties.setProperty("javax.sip.IP_ADDRESS", ip);
        // Turn off automated sessions
        properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "off");

        /**
         * Full configuration reference gov.nist.javax.sip.SipStackImpl，Need to download source code
         * gov/nist/javax/sip/SipStackImpl.class
         * sipThe message is parsed in gov.nist.javax.sip.stack.UDPMessageChannelofprocessIncomingDataPacketmethod
         */

        // Receive all notify requests, even if not subscribed
        properties.setProperty("gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY", "true");
        properties.setProperty("gov.nist.javax.sip.AUTOMATIC_DIALOG_ERROR_HANDLING", "false");
        properties.setProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true");
        // Pass _terminated_event for _NULL_dialog
        properties.setProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", "true");
        // Whether to automatically calculate the actual length of content length, not calculated by default
        properties.setProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "true");
        // Session cleanup strategy
        properties.setProperty("gov.nist.javax.sip.RELEASE_REFERENCES_STRATEGY", "Normal");
        // Handles underlying TCP based keep alive timeouts handled by this server
        properties.setProperty("gov.nist.javax.sip.RELIABLE_CONNECTION_KEEP_ALIVE_TIMEOUT", "60");
        // Get the actual content length without using the length information in the header
        properties.setProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "true");
        // Threads are reentrant
        properties.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER", "true");
        // Defines how often the application intends to audit the SIP stack for the health of its internal threads (this property specifies the time in milliseconds between consecutive audits））
        properties.setProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS", "30000");

        // Some devices will send a large number of registrations in a short period of time, causing the protocol stack memory to overflow. Turning this on can prevent these devices from registering and avoid service crashes, but it will reduce system performance, as described below.
        // The default value is true。
        // Setting this to false will cause the Stack to Server Transaction Close the server socket after entering the TERMINATED state。
        // This allows the server to prevent client-initiated TCP-based denial-of-service attacks (i.e., initiating hundreds of client transactions）。
        // If true (the default), the stack will keep the socket open to maximize performance at the expense of thread and memory resources - Make yourself vulnerable to DOS attacks。
        properties.setProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS", String.valueOf(sipCacheServerConnections));

        properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", "com.genersoft.iot.vmp.gb28181.conf.CustomMessageProcessorFactory");

        /**
         * sip_server_log.log and sip_debug_log.log ERROR, INFO, WARNING, OFF, DEBUG, TRACE
         */
        Logger log = LoggerFactory.getLogger(AlarmNotifyMessageHandler.class);
        if (sipLog) {
            properties.setProperty("gov.nist.javax.sip.STACK_LOGGER", "com.genersoft.iot.vmp.gb28181.conf.StackLoggerImpl");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOGGER", "com.genersoft.iot.vmp.gb28181.conf.ServerLoggerImpl");
            properties.setProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT", "true");
            log.info("[SIPLog]Already turned on");
        }else {
            log.info("[SIPLog]Closed");
        }
        return properties;
    }
}
