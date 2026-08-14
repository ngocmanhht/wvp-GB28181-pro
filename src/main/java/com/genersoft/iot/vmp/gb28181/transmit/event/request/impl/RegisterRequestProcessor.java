package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.RemoteAddressInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.auth.DigestServerAuthenticationHelper;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.GbCode;
import com.genersoft.iot.vmp.gb28181.bean.GbSipDate;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.IpPortUtil;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * SIPCommand type: REGISTER request
 */
@Slf4j
@Component
public class RegisterRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    public final String method = "REGISTER";

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private SIPProcessorObserver sipProcessorObserver;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private SIPSender sipSender;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;


    @Override
    public void afterPropertiesSet() throws Exception {
        // Add message processing subscription
        sipProcessorObserver.addRequestProcessor(method, this);
    }

    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();

            FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
            AddressImpl address = (AddressImpl) fromHeader.getAddress();
            SipUri uri = (SipUri) address.getURI();
            String deviceId = uri.getUser();

            if (userSetting.isDeviceIdStrict()) {
                GbCode decode = GbCode.decode(deviceId);
                if (decode == null) {
                    Response response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
                    sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                    return;
                }
            }

            ExpiresHeader expiresHeader = request.getExpires();
            if (expiresHeader == null) {
                Response response = getMessageFactory().createResponse(Response.BAD_REQUEST, request);
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                return;
            }

            boolean registerFlag = expiresHeader.getExpires() != 0;

            Device device = deviceService.getDeviceByDeviceId(deviceId);

            RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request,
                    userSetting.getSipUseSourceIpAsRemoteAddress());
            String requestAddress = remoteAddressInfo.getIp() + ":" + remoteAddressInfo.getPort();

            if (registerFlag) {
                registerHandler(device, request, remoteAddressInfo, deviceId, requestAddress);
            } else {
                cancellationHandler(device, request, remoteAddressInfo, deviceId, requestAddress);
            }
        } catch (SipException | NoSuchAlgorithmException | ParseException e) {
            log.error("unhandled exception ", e);
        }
    }

    private Response getRegisterOkResponse(Request request) throws ParseException {
        // Bring the authorization header and the password is correct
        Response response = getMessageFactory().createResponse(Response.OK, request);
        // If the Date header is actively disabled, it is not added
        if (!userSetting.isDisableDateHeader()) {
            // Add date header
            SIPDateHeader dateHeader = new SIPDateHeader();
            // Use your own modified
            GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
            dateHeader.setDate(gbSipDate);
            response.addHeader(dateHeader);
        }

        // Add Contact header
        response.addHeader(request.getHeader(ContactHeader.NAME));
        // Add Expires header
        response.addHeader(request.getExpires());

        return response;

    }

    private void registerHandler(Device device, SIPRequest request, RemoteAddressInfo remoteAddressInfo,
                                  String deviceId, String requestAddress) throws SipException, NoSuchAlgorithmException, ParseException {
        if (device != null && device.getSipTransactionInfo() != null &&
                request.getCallIdHeader().getCallId().equals(device.getSipTransactionInfo().getCallId())) {
            log.info("[Registration renewal] Equipment：{}", device.getDeviceId());
            device.setExpires(request.getExpires().getExpires());
            device.setIp(remoteAddressInfo.getIp());
            device.setPort(remoteAddressInfo.getPort());
            device.setHostAddress(IpPortUtil.concatenateIpAndPort(remoteAddressInfo.getIp(), String.valueOf(remoteAddressInfo.getPort())));
            device.setLocalIp(request.getLocalAddress().getHostAddress());

            ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
            String transport = reqViaHeader.getTransport();
            device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");

            Response okResponse = getRegisterOkResponse(request);
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), okResponse);
            device.setRegisterTimeStamp(System.currentTimeMillis());
            deviceService.online(device);
            redisCatchStorage.updateDeviceRegisterTimeStamp(List.of(device));
            return;
        }

        if (device == null && ObjectUtils.isEmpty(sipConfig.getPassword())) {
            log.info("[Registration request] Equipment：{}, address: {}, The public password has been disabled, please add user information and register", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }
        String password = device != null && !ObjectUtils.isEmpty(device.getPassword()) ? device.getPassword() : sipConfig.getPassword();

        AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if (!ObjectUtils.isEmpty(password) && authHead == null) {
            log.info("[Registration request] Equipment：{}, Reply401: {}", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.UNAUTHORIZED, request);
            new DigestServerAuthenticationHelper().generateChallenge(getHeaderFactory(), response, sipConfig.getDomain());
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }

        if (!ObjectUtils.isEmpty(password) && !new DigestServerAuthenticationHelper().doAuthenticatePlainTextPassword(request, password)) {
            log.info("[Registration request] Equipment：{}, Password/SIPWrong server ID, reply403: {}", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
            response.setReasonPhrase("wrong password");
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }

        Response response = getMessageFactory().createResponse(Response.OK, request);
        if (!userSetting.isDisableDateHeader()) {
            SIPDateHeader dateHeader = new SIPDateHeader();
            GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
            dateHeader.setDate(gbSipDate);
            response.addHeader(dateHeader);
        }
        response.addHeader(request.getHeader(ContactHeader.NAME));
        response.addHeader(request.getExpires());

        if (device == null) {
            device = new Device();
            device.setStreamMode("TCP-PASSIVE");
            device.setCharset("GB2312");
            device.setGeoCoordSys("WGS84");
            device.setMediaServerId("auto");
            device.setDeviceId(deviceId);
            device.setOnLine(false);
        } else {
            if (ObjectUtils.isEmpty(device.getStreamMode())) {
                device.setStreamMode("TCP-PASSIVE");
            }
            if (ObjectUtils.isEmpty(device.getCharset())) {
                device.setCharset("GB2312");
            }
            if (ObjectUtils.isEmpty(device.getGeoCoordSys())) {
                device.setGeoCoordSys("WGS84");
            }
        }
        device.setServerId(userSetting.getServerId());
        device.setIp(remoteAddressInfo.getIp());
        device.setPort(remoteAddressInfo.getPort());
        device.setHostAddress(IpPortUtil.concatenateIpAndPort(remoteAddressInfo.getIp(), String.valueOf(remoteAddressInfo.getPort())));
        device.setLocalIp(request.getLocalAddress().getHostAddress());
        device.setExpires(request.getExpires().getExpires());

        ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
        String transport = reqViaHeader.getTransport();
        device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");

        sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);

        device.setRegisterTimeStamp(System.currentTimeMillis());
        SipTransactionInfo sipTransactionInfo = new SipTransactionInfo((SIPResponse) response);
        device.setSipTransactionInfo(sipTransactionInfo);
        deviceService.online(device);
        redisCatchStorage.updateDeviceRegisterTimeStamp(List.of(device));

        log.info("[Registration successful] deviceId: {}->{}", deviceId, requestAddress);
    }

    private void cancellationHandler(Device device, SIPRequest request, RemoteAddressInfo remoteAddressInfo,
                                      String deviceId, String requestAddress) throws SipException, NoSuchAlgorithmException, ParseException {
        if (device != null && device.getSipTransactionInfo() != null &&
                request.getCallIdHeader().getCallId().equals(device.getSipTransactionInfo().getCallId())) {
            Response response = getRegisterOkResponse(request);
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            deviceService.offline(device);
            device.setRegisterTimeStamp(System.currentTimeMillis());
            redisCatchStorage.updateDeviceRegisterTimeStamp(List.of(device));
            log.info("[Logout successful] deviceId: {}->{}", deviceId, requestAddress);
            return;
        }

        if (device == null && ObjectUtils.isEmpty(sipConfig.getPassword())) {
            log.info("[Logout request] Equipment：{}, address: {}, The public password has been disabled, please add user information and log out", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }
        String password = device != null && !ObjectUtils.isEmpty(device.getPassword()) ? device.getPassword() : sipConfig.getPassword();

        AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if (!ObjectUtils.isEmpty(password) && authHead == null) {
            log.info("[Logout request] Equipment：{}, Reply401: {}", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.UNAUTHORIZED, request);
            new DigestServerAuthenticationHelper().generateChallenge(getHeaderFactory(), response, sipConfig.getDomain());
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }

        if (!ObjectUtils.isEmpty(password) && !new DigestServerAuthenticationHelper().doAuthenticatePlainTextPassword(request, password)) {
            log.info("[Logout request] Equipment：{}, Password/SIPWrong server ID, reply403: {}", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
            response.setReasonPhrase("wrong password");
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }

        Response response = getRegisterOkResponse(request);
        sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);

        if (device != null) {
            deviceService.offline(device);
            device.setRegisterTimeStamp(System.currentTimeMillis());
            redisCatchStorage.updateDeviceRegisterTimeStamp(List.of(device));
        }

        log.info("[Logout successful] deviceId: {}->{}", deviceId, requestAddress);
    }

}
