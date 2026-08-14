package com.genersoft.iot.vmp.gb28181.transmit;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.event.sip.SipEvent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.SipException;
import javax.sip.header.*;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * Send SIP message
 *
 * @author lin
 */
@Slf4j
@Component
public class SIPSender {

    @Autowired
    private SipLayer sipLayer;

    @Autowired
    private GitUtil gitUtil;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private SipConfig sipConfig;

    public void transmitRequest(String ip, Message message) throws SipException, ParseException {
        transmitRequest(ip, message, null, null, null);
    }

    public void transmitRequest(String ip, Message message, SipSubscribe.Event errorEvent) throws SipException, ParseException {
        transmitRequest(ip, message, errorEvent, null, null);
    }

    public void transmitRequest(String ip, Message message, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException {
        transmitRequest(ip, message, errorEvent, okEvent, null);
    }

    public void transmitRequest(String ip, Message message, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent, Long timeout) throws SipException {
        ViaHeader viaHeader = (ViaHeader) message.getHeader(ViaHeader.NAME);
        String transport = "UDP";
        if (viaHeader == null) {
            log.warn("[Missing message header]： ViaHeader， Use the default UDP method to process data");
        } else {
            transport = viaHeader.getTransport();
        }
        if (message.getHeader(UserAgentHeader.NAME) == null) {
            try {
                message.addHeader(SipUtils.createUserAgentHeader(gitUtil));
            } catch (ParseException e) {
                log.error("Adding UserAgentHeader failed", e);
            }
        }
        CallIdHeader callIdHeader = (CallIdHeader) message.getHeader(CallIdHeader.NAME);
        CSeqHeader cSeqHeader = (CSeqHeader) message.getHeader(CSeqHeader.NAME);
        String key = callIdHeader.getCallId() + cSeqHeader.getSeqNumber();
        if (okEvent != null || errorEvent != null) {

            FromHeader fromHeader = (FromHeader) message.getHeader(FromHeader.NAME);
            SipEvent sipEvent = SipEvent.getInstance(key, eventResult -> {
                sipSubscribe.removeSubscribe(key);
                if(okEvent != null) {
                    okEvent.response(eventResult);
                }
            }, (eventResult -> {
                sipSubscribe.removeSubscribe(key);
                if (errorEvent != null) {
                    errorEvent.response(eventResult);
                }
            }), timeout == null ? sipConfig.getTimeout() : timeout);
            SipTransactionInfo sipTransactionInfo = new SipTransactionInfo();
            sipTransactionInfo.setFromTag(fromHeader.getTag());
            sipTransactionInfo.setCallId(callIdHeader.getCallId());

            if (message instanceof SIPResponse) {
                SIPResponse response = (SIPResponse) message;
                sipTransactionInfo.setToTag(response.getToHeader().getTag());
                sipTransactionInfo.setViaBranch(response.getTopmostViaHeader().getBranch());
            }else if (message instanceof SIPRequest) {
                SIPRequest request = (SIPRequest) message;
                sipTransactionInfo.setViaBranch(request.getTopmostViaHeader().getBranch());
                SipUri sipUri = (SipUri)request.getRequestLine().getUri();
                sipTransactionInfo.setUser(sipUri.getUser());
            }

            ExpiresHeader expiresHeader = (ExpiresHeader) message.getHeader(ExpiresHeader.NAME);
            if (expiresHeader != null) {
                sipTransactionInfo.setExpires(expiresHeader.getExpires());
            }
            sipEvent.setSipTransactionInfo(sipTransactionInfo);
            sipSubscribe.addSubscribe(key, sipEvent);
        }
        try {
            if ("TCP".equals(transport)) {
                SipProviderImpl tcpSipProvider = sipLayer.getTcpSipProvider(ip);
                if (tcpSipProvider == null) {
                    log.error("[Failed to send message] not foundtcp://{}monitoring information", ip);
                    return;
                }
                if (message instanceof Request) {
                    tcpSipProvider.sendRequest((Request) message);
                } else if (message instanceof Response) {
                    tcpSipProvider.sendResponse((Response) message);
                }

            } else if ("UDP".equals(transport)) {
                SipProviderImpl sipProvider = sipLayer.getUdpSipProvider(ip);
                if (sipProvider == null) {
                    log.error("[Failed to send message] not foundudp://{}monitoring information", ip);
                    return;
                }
                if (message instanceof Request) {
                    sipProvider.sendRequest((Request) message);
                } else if (message instanceof Response) {
                    sipProvider.sendResponse((Response) message);
                }
            }
        }catch (SipException e) {
            sipSubscribe.removeSubscribe(key);
            throw e;
        }
    }

    public CallIdHeader getNewCallIdHeader(String ip, String transport) {
        if (ObjectUtils.isEmpty(transport)) {
            return sipLayer.getUdpSipProvider() != null ? sipLayer.getUdpSipProvider().getNewCallId() : sipLayer.getTcpSipProvider().getNewCallId();
        }
        SipProviderImpl sipProvider;
        if (ObjectUtils.isEmpty(ip)) {
            sipProvider = transport.equalsIgnoreCase("TCP") ? sipLayer.getTcpSipProvider()
                    : sipLayer.getUdpSipProvider();
        } else {
            sipProvider = transport.equalsIgnoreCase("TCP") ? sipLayer.getTcpSipProvider(ip)
                    : sipLayer.getUdpSipProvider(ip);
        }

        if (sipProvider == null) {
            sipProvider = transport.equalsIgnoreCase("TCP") ? sipLayer.getTcpSipProvider()
                    : sipLayer.getUdpSipProvider();
        }

        if (sipProvider != null) {
            return sipProvider.getNewCallId();
        } else {
            log.warn("[Failed to create new CallIdHeader]， ip={}, transport={}", ip, transport);
            return null;
        }
    }


}
