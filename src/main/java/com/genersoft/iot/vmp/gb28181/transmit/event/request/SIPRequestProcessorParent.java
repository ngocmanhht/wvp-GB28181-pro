package com.genersoft.iot.vmp.gb28181.transmit.event.request;

import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.IpPortUtil;
import com.google.common.primitives.Bytes;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.ObjectUtils;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @description:Process and receive SIP protocol request messages from IPCamera
 * @author: songww
 * @date:   2020May 3rd, afternoon4:42:22
 */
@Slf4j
public abstract class SIPRequestProcessorParent {

	@Autowired
	private SIPSender sipSender;

	public HeaderFactory getHeaderFactory() {
		try {
			return SipFactory.getInstance().createHeaderFactory();
		} catch (PeerUnavailableException e) {
			log.error("unhandled exception ", e);
		}
		return null;
	}

	public MessageFactory getMessageFactory() {
		try {
			return SipFactory.getInstance().createMessageFactory();
		} catch (PeerUnavailableException e) {
			log.error("unhandled exception ", e);
		}
		return null;
	}

	class ResponseAckExtraParam{
		String content;
		ContentTypeHeader contentTypeHeader;
		SipURI sipURI;
		int expires = -1;
	}

	/***
	 * Reply status code
	 * 100 trying
	 * 200 OK
	 * 400
	 * 404
	 */
	public SIPResponse responseAck(SIPRequest sipRequest, int statusCode) throws SipException, InvalidArgumentException, ParseException {
		return responseAck(sipRequest, statusCode, null);
	}

	@Async
	public void responseAckAsync(SIPRequest sipRequest, int statusCode) throws SipException, InvalidArgumentException, ParseException {
		responseAck(sipRequest, statusCode, null);
	}

	public SIPResponse responseAck(SIPRequest sipRequest, int statusCode, String msg) throws SipException, InvalidArgumentException, ParseException {
		return responseAck(sipRequest, statusCode, msg, null);
	}


	public SIPResponse responseAck(SIPRequest sipRequest, int statusCode, String msg, ResponseAckExtraParam responseAckExtraParam) throws SipException, InvalidArgumentException, ParseException {
		// Global defense: Verify the legality of SIP status codes and prevent illegal status codes from being passed inJAIN-SIPcauseIllegalArgumentException
		if (statusCode < 100 || statusCode > 699) {
			log.error("[SIPresponse] Illegal status code: {}，Replaced with500 Server Internal Error。original message: {}", statusCode, msg);
			statusCode = Response.SERVER_INTERNAL_ERROR; // 500
		}

		if (sipRequest.getToHeader().getTag() == null) {
			sipRequest.getToHeader().setTag(SipUtils.getNewTag());
		}
		SIPResponse response = (SIPResponse)getMessageFactory().createResponse(statusCode, sipRequest);
		response.setStatusCode(statusCode);
		if (msg != null) {
			response.setReasonPhrase(msg);
		}

		if (responseAckExtraParam != null) {
			if (responseAckExtraParam.sipURI != null && sipRequest.getMethod().equals(Request.INVITE)) {
				log.debug("responseSdpAck SipURI: {}:{}", responseAckExtraParam.sipURI.getHost(), responseAckExtraParam.sipURI.getPort());
				Address concatAddress = SipFactory.getInstance().createAddressFactory().createAddress(
						SipFactory.getInstance().createAddressFactory().createSipURI(responseAckExtraParam.sipURI.getUser(), IpPortUtil.concatenateIpAndPort(responseAckExtraParam.sipURI.getHost(), String.valueOf(responseAckExtraParam.sipURI.getPort()))
						));
				response.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));
			}
			if (responseAckExtraParam.contentTypeHeader != null) {
				response.setContent(responseAckExtraParam.content, responseAckExtraParam.contentTypeHeader);
			}

			if (sipRequest.getMethod().equals(Request.SUBSCRIBE)) {
				if (responseAckExtraParam.expires == -1) {
					log.error("[Incomplete parameters] 2xxSUBSCRIBE reply, must be setExpires header");
				}else {
					ExpiresHeader expiresHeader = SipFactory.getInstance().createHeaderFactory().createExpiresHeader(responseAckExtraParam.expires);
					response.addHeader(expiresHeader);
				}
			}
		}else {
			if (sipRequest.getMethod().equals(Request.SUBSCRIBE)) {
				log.error("[Incomplete parameters] 2xxSUBSCRIBE reply, must be setExpires header");
			}
		}

		// sendresponse
		sipSender.transmitRequest(sipRequest.getLocalAddress().getHostAddress(), response);

		return response;
	}



	/**
	 * Reply with sdp200
	 */
	public SIPResponse responseSdpAck(SIPRequest request, String sdp, Platform platform) throws SipException, InvalidArgumentException, ParseException {

		ContentTypeHeader contentTypeHeader = SipFactory.getInstance().createHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");

		// Compatible with the use of encoding@domain name as RequestURI in the national standard
		SipURI sipURI = (SipURI)request.getRequestURI();
		if (sipURI.getPort() == -1) {
			sipURI = SipFactory.getInstance().createAddressFactory().createSipURI(platform.getServerGBId(),  IpPortUtil.concatenateIpAndPort(platform.getServerIp(), String.valueOf(platform.getServerPort())));
		}
		ResponseAckExtraParam responseAckExtraParam = new ResponseAckExtraParam();
		responseAckExtraParam.contentTypeHeader = contentTypeHeader;
		responseAckExtraParam.content = sdp;
		responseAckExtraParam.sipURI = sipURI;

		SIPResponse sipResponse = responseAck(request, Response.OK, null, responseAckExtraParam);


		return sipResponse;
	}

	/**
	 * Reply with xml200
	 */
	public SIPResponse responseXmlAck(SIPRequest request, String xml, Platform platform, Integer expires) throws SipException, InvalidArgumentException, ParseException {
		ContentTypeHeader contentTypeHeader = SipFactory.getInstance().createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");

		SipURI sipURI = (SipURI)request.getRequestURI();
		if (sipURI.getPort() == -1) {
			sipURI = SipFactory.getInstance().createAddressFactory().createSipURI(platform.getServerGBId(), IpPortUtil.concatenateIpAndPort(platform.getServerIp(), String.valueOf(platform.getServerPort())));
		}
		ResponseAckExtraParam responseAckExtraParam = new ResponseAckExtraParam();
		responseAckExtraParam.contentTypeHeader = contentTypeHeader;
		responseAckExtraParam.content = xml;
		responseAckExtraParam.sipURI = sipURI;
		responseAckExtraParam.expires = expires;
		return responseAck(request, Response.OK, null, responseAckExtraParam);
	}

	public Element getRootElement(RequestEvent evt) throws DocumentException {
		return getRootElement(evt, "gb2312");
	}
	public Element getRootElement(RequestEvent evt, String charset) throws DocumentException {

		byte[] rawContent = evt.getRequest().getRawContent();
		if (evt.getRequest().getContentLength().getContentLength() == 0
				|| rawContent == null
				|| rawContent.length == 0
				|| ObjectUtils.isEmpty(new String(rawContent))) {
			return null;
		}

		if (charset == null) {
			charset = "gb2312";
		}
		SAXReader reader = new SAXReader();
		reader.setEncoding(charset);
		// Process unescaped characters appearing in Haikang。
		String[] destStrArray = new String[]{"&lt;","&gt;","&amp;","&apos;","&quot;"};
		// It may be extended to be compatible with other characters
		char despChar = '&';
		byte destBye = (byte) despChar;
		List<Byte> result = new ArrayList<>();
		for (int i = 0; i < rawContent.length; i++) {
			if (rawContent[i] == destBye) {
				boolean resul = false;
				for (String destStr : destStrArray) {
					if (i + destStr.length() <= rawContent.length) {
						byte[] bytes = Arrays.copyOfRange(rawContent, i, i + destStr.length());
						resul = resul || (Arrays.equals(bytes,destStr.getBytes()));
					}
				}
				if (resul) {
					result.add(rawContent[i]);
				}
			}else {
				result.add(rawContent[i]);
			}
		}
		byte[] bytesResult = Bytes.toArray(result);

		Document xml;
		try {
			xml = reader.read(new ByteArrayInputStream(bytesResult));
		}catch (DocumentException e) {
			log.warn("[xmlparsing exception]： The original text is as follows： \r\n{}", new String(bytesResult));
			log.warn("[xmlparsing exception]： The original text is as follows: Try compatibility processing");
			String[] xmlLineArray = new String(bytesResult).split("\\r?\\n");

			// The address field compatible with Hikvision has<The problem of failure to parse xml due to broken xml structure
			StringBuilder stringBuilder = new StringBuilder();
			for (String s : xmlLineArray) {
				if (s.startsWith("<Address")) {
					continue;
				}
				stringBuilder.append(s);
			}
			xml = reader.read(new ByteArrayInputStream(stringBuilder.toString().getBytes()));
		}
		return xml.getRootElement();
	}


}
