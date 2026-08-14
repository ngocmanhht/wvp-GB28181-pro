package com.genersoft.iot.vmp.gb28181.utils;

import com.genersoft.iot.vmp.gb28181.bean.Gb28181Sdp;
import com.genersoft.iot.vmp.common.RemoteAddressInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Subject;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.ObjectUtils;

import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.header.FromHeader;
import javax.sip.header.SubjectHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author panlinlin
 * @version 1.0.0
 * @description JAIN SIPTool class
 * @createTime 2021September 27, 15:12:00
 */
@Slf4j
public class SipUtils {

    public static String getUserIdFromFromHeader(Request request) {
        FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
        return getUserIdFromFromHeader(fromHeader);
    }
    /**
     * Read from subjectchannelId
     * */
    public static String[] getChannelIdFromRequest(Request request) {
        SubjectHeader subject = (Subject)request.getHeader("subject");
        if (subject == null) {
            // if missingsubject
            return null;
        }
        String[] result = new String[2];
        String subjectStr = subject.getSubject();
        if (subjectStr.indexOf(",") > 0) {
            String[] subjectSplit = subjectStr.split(",");
            result[0] = subjectSplit[0].split(":")[0];
            result[1] = subjectSplit[1].split(":")[0];
        }else {
            result[0] = subjectStr.split(":")[0];
        }
        return result;
    }

    public static String getUserIdFromFromHeader(FromHeader fromHeader) {
        AddressImpl address = (AddressImpl)fromHeader.getAddress();
        SipUri uri = (SipUri) address.getURI();
        return uri.getUser();
    }

    public static  String getNewViaTag() {
        return "z9hG4bK" + RandomStringUtils.randomNumeric(10);
    }

    public static UserAgentHeader createUserAgentHeader(GitUtil gitUtil) throws PeerUnavailableException, ParseException {
        List<String> agentParam = new ArrayList<>();
        agentParam.add("WVP-Pro ");
        if (gitUtil != null ) {
            if (!ObjectUtils.isEmpty(gitUtil.getBuildVersion())) {
                agentParam.add("v");
                agentParam.add(gitUtil.getBuildVersion() + ".");
            }
            if (!ObjectUtils.isEmpty(gitUtil.getCommitTime())) {
                agentParam.add(gitUtil.getCommitTime());
            }
        }
        return SipFactory.getInstance().createHeaderFactory().createUserAgentHeader(agentParam);
    }

    public static String getNewFromTag(){
        return UUID.randomUUID().toString().replace("-", "");

//        return getNewTag();
    }

    public static String getNewTag(){
        return String.valueOf(System.currentTimeMillis());
    }


    /**
     * PTZ instruction code calculation
     *
     * @param leftRight  Camera moves left and right 0: Stop 1: Move left 2: Move right
     * @param upDown     Lens moves up and down 0: Stop 1: Move up 2: Move down
     * @param inOut      Lens zoom in and out 0: Stop 1: Zoom out 2: Zoom in
     * @param moveSpeed  Lens movement speed Default 0XFF (0-255)
     * @param zoomSpeed  Lens zoom speed Default 0X1 (0-255)
     */
    public static String cmdString(int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed) {
        int cmdCode = 0;
        if (leftRight == 2) {
            cmdCode|=0x01;		// Move right
        } else if(leftRight == 1) {
            cmdCode|=0x02;		// Shift left
        }
        if (upDown == 2) {
            cmdCode|=0x04;		// move down
        } else if(upDown == 1) {
            cmdCode|=0x08;		// move up
        }
        if (inOut == 2) {
            cmdCode |= 0x10;	// Zoom in
        } else if(inOut == 1) {
            cmdCode |= 0x20;	// zoom out
        }
        StringBuilder builder = new StringBuilder("A50F01");
        String strTmp;
        strTmp = String.format("%02X", cmdCode);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", moveSpeed);
        builder.append(strTmp, 0, 2);
        builder.append(strTmp, 0, 2);

        //Optimize the zoom rate at low zoom speeds
        if ((zoomSpeed > 0) && (zoomSpeed <16))
        {
            zoomSpeed = 16;
        }
        strTmp = String.format("%X", zoomSpeed);
        builder.append(strTmp, 0, 1).append("0");
        //Calculate check code
        int checkCode = (0XA5 + 0X0F + 0X01 + cmdCode + moveSpeed + moveSpeed + (zoomSpeed /*<< 4*/ & 0XF0)) % 0X100;
        strTmp = String.format("%02X", checkCode);
        builder.append(strTmp, 0, 2);
        return builder.toString();
    }

    public static String getNewCallId() {
        return (int) Math.floor(Math.random() * 1000000000) + "";
    }

    public static int getTypeCodeFromGbCode(String deviceId) {
        if (ObjectUtils.isEmpty(deviceId)) {
            return 0;
        }
        return Integer.parseInt(deviceId.substring(10, 13));
    }

    /**
     * Determine whether it is a front-end peripheral device
     * @param deviceId
     * @return
     */
    public static boolean isFrontEnd(String deviceId) {
        int typeCodeFromGbCode = getTypeCodeFromGbCode(deviceId);
        return typeCodeFromGbCode > 130 && typeCodeFromGbCode < 199;
    }
    /**
     * Get the device ip address and port number from the request
     * @param request Request
     * @param sipUseSourceIpAsRemoteAddress  false Get the address from via, true to get the remote address directly
     * @return Address information
     */
    public static RemoteAddressInfo getRemoteAddressFromRequest(SIPRequest request, boolean sipUseSourceIpAsRemoteAddress) {

        String remoteAddress;
        int remotePort;
        if (sipUseSourceIpAsRemoteAddress) {
            remoteAddress = request.getPeerPacketSourceAddress().getHostAddress();
            remotePort = request.getPeerPacketSourcePort();

        }else {
            // Determine whether the RPort has changed. Changes indicate changes in routing NAT information and modify device information.
            // Obtain information such as mailing address
            remoteAddress = request.getTopmostViaHeader().getReceived();
            remotePort = request.getTopmostViaHeader().getRPort();
            // Resolve local address substitution
            if (ObjectUtils.isEmpty(remoteAddress) || remotePort == -1) {
                if (request.getPeerPacketSourceAddress() != null) {
                    remoteAddress = request.getPeerPacketSourceAddress().getHostAddress();
                }else {
                    remoteAddress = request.getRemoteAddress().getHostAddress();
                }
                if( request.getPeerPacketSourcePort() > 0) {
                    remotePort = request.getPeerPacketSourcePort();
                }else {
                    remotePort = request.getRemotePort();
                }
            }
        }

        return new RemoteAddressInfo(remoteAddress, remotePort);
    }

    public static Gb28181Sdp parseSDP(String sdpStr) throws SdpParseException {

        // jainSipNot supportedy= f=Field, removed for parsing。
        int ssrcIndex = sdpStr.indexOf("y=");
        int mediaDescriptionIndex = sdpStr.indexOf("f=");
        // Check if there is a y field
        SessionDescription sdp;
        String ssrc = null;
        String mediaDescription = null;
        if (mediaDescriptionIndex == 0 && ssrcIndex == 0) {
            sdp = SdpFactory.getInstance().createSessionDescription(sdpStr);
        }else {
            String lines[] = sdpStr.split("\\r?\\n");
            StringBuilder sdpBuffer = new StringBuilder();
            for (String line : lines) {
                if (line.trim().startsWith("y=")) {
                    ssrc = line.substring(2);
                }else if (line.trim().startsWith("f=")) {
                    mediaDescription = line.substring(2);
                }else {
                    sdpBuffer.append(line.trim()).append("\r\n");
                }
            }
            sdp = SdpFactory.getInstance().createSessionDescription(sdpBuffer.toString());
        }
        return Gb28181Sdp.getInstance(sdp, ssrc, mediaDescription);
    }

    public static String getSsrcFromSdp(String sdpStr) {

        // jainSipNot supportedy= f=Field, removed for parsing。
        int ssrcIndex = sdpStr.indexOf("y=");
        if (ssrcIndex == 0) {
            return null;
        }
        String lines[] = sdpStr.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().startsWith("y=")) {
                return line.substring(2);
            }
        }
        return null;
    }

    public static String parseTime(String timeStr) {
        if (ObjectUtils.isEmpty(timeStr)){
            return null;
        }
        LocalDateTime localDateTime;
        try {
            localDateTime = LocalDateTime.parse(timeStr);
        }catch (DateTimeParseException e) {
            try {
                localDateTime = LocalDateTime.parse(timeStr, DateUtil.formatterISO8601);
            }catch (DateTimeParseException e2) {
                log.error("[Format time] Unable to format time： {}", timeStr);
                return null;
            }
        }
        return localDateTime.format(DateUtil.formatter);
    }

    public static Long parseTimeForTimestamp(String timeStr) {
        if (ObjectUtils.isEmpty(timeStr)){
            return null;
        }
        LocalDateTime localDateTime;
        try {
            localDateTime = LocalDateTime.parse(timeStr);
        }catch (DateTimeParseException e) {
            try {
                localDateTime = LocalDateTime.parse(timeStr, DateUtil.formatterISO8601);
            }catch (DateTimeParseException e2) {
                log.error("[Format time] Unable to format time： {}", timeStr);
                return null;
            }
        }
        // Returns millisecond value
        return localDateTime.atZone(ZoneId.of(DateUtil.zoneStr)).toInstant().toEpochMilli();

    }
}
