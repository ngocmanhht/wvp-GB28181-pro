package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * SIPCommand type: SUBSCRIBE request
 * @author lin
 */
@Slf4j
@Component
public class SubscribeRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private final String method = "SUBSCRIBE";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private SubscribeHolder subscribeHolder;

	@Autowired
	private SIPSender sipSender;


	@Autowired
	private IPlatformService platformService;

	@Override
	public void afterPropertiesSet() throws Exception {
		// Add message processing subscription
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	/**
	 * Handling SUBSCRIBE requests
	 *
	 * @param evt event
	 */
	@Override
	public void process(RequestEvent evt) {
		SIPRequest request = (SIPRequest) evt.getRequest();
		try {
			Element rootElement = getRootElement(evt);
			if (rootElement == null) {
				log.error("Processing SUBSCRIBE request, message body not obtained{}", evt.getRequest());
				responseAck(request, Response.BAD_REQUEST);
				return;
			}
			ExpiresHeader expires = request.getExpires();
			if (expires == null) {
				log.error("Processing SUBSCRIBE request not obtainedExpiresHeader{}", evt.getRequest());
				responseAck(request, Response.BAD_REQUEST, "missing expires");
				return;
			}
			String platformId = SipUtils.getUserIdFromFromHeader(request);
			String cmd = XmlUtil.getText(rootElement, "CmdType");
			log.info("[Subscription request received] Type： {}, from： {}", cmd, platformId);
			if (CmdType.MOBILE_POSITION.equals(cmd)) {
				processNotifyMobilePosition(request, rootElement);
//			} else if (CmdType.ALARM.equals(cmd)) {
//				logger.info("Alarm subscription received");
//				processNotifyAlarm(serverTransaction, rootElement);
			} else if (CmdType.CATALOG.equals(cmd)) {
				processNotifyCatalogList(request, rootElement);
			} else {
                log.info("message received：{}", cmd);

				Response response = getMessageFactory().createResponse(200, request);
				if (response != null) {
					ExpiresHeader expireHeader = getHeaderFactory().createExpiresHeader(30);
					response.setExpires(expireHeader);
				}
                log.info("response : {}", response);
				sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
			}
		} catch (ParseException | SipException | InvalidArgumentException | DocumentException e) {
			log.error("unhandled exception ", e);
		}

	}

	/**
	 * Handling mobile location subscription messages
	 */
	private void processNotifyMobilePosition(SIPRequest request, Element rootElement) throws SipException {
		if (request == null) {
			return;
		}
		String platformId = SipUtils.getUserIdFromFromHeader(request);
		String deviceId = XmlUtil.getText(rootElement, "DeviceID");
		Platform platform = platformService.queryPlatformByServerGBId(platformId);
		if (platform == null) {
			return;
		}

		String sn = XmlUtil.getText(rootElement, "SN");
		log.info("[Reply to superior's mobile location subscription request]: {}", platformId);
		StringBuilder resultXml = new StringBuilder(200);
		resultXml.append("<?xml version=\"1.0\" ?>\r\n")
				.append("<Response>\r\n")
				.append("<CmdType>MobilePosition</CmdType>\r\n")
				.append("<SN>").append(sn).append("</SN>\r\n")
				.append("<DeviceID>").append(deviceId).append("</DeviceID>\r\n")
				.append("<Result>OK</Result>\r\n")
				.append("</Response>\r\n");
		try {
			int expires = request.getExpires().getExpires();
			SIPResponse response = responseXmlAck(request, resultXml.toString(), platform, expires);

			SubscribeInfo subscribeInfo = SubscribeInfo.getInstance(response, platformId, expires,
					(EventHeader)request.getHeader(EventHeader.NAME));
			if (subscribeInfo.getExpires() > 0) {
				// GPSReporting interval
				String interval = XmlUtil.getText(rootElement, "Interval");
				if (interval == null) {
					subscribeInfo.setGpsInterval(5);
				}else {
					subscribeInfo.setGpsInterval(Integer.parseInt(interval));
				}
				subscribeInfo.setSn(sn);
			}
			if (subscribeInfo.getExpires() == 0) {
				subscribeHolder.removeMobilePositionSubscribe(platformId);
			}else {
				subscribeInfo.setTransactionInfo(new SipTransactionInfo(response));
				subscribeHolder.putMobilePositionSubscribe(platformId, subscribeInfo, ()->{
					platformService.sendNotifyMobilePosition(platformId);
				});
			}

		} catch (SipException | InvalidArgumentException | ParseException e) {
			log.error("unhandled exception ", e);
		}
	}

	private void processNotifyAlarm(RequestEvent evt, Element rootElement) {

	}

	private void processNotifyCatalogList(SIPRequest request, Element rootElement) throws SipException {
		if (request == null) {
			log.info("[Handle directory subscriptions] Found request to be NUll. Ignored");
			return;
		}
		String platformId = SipUtils.getUserIdFromFromHeader(request);
		String deviceId = XmlUtil.getText(rootElement, "DeviceID");
		Platform platform = platformService.queryPlatformByServerGBId(platformId);
		if (platform == null){
			log.info("[Handle directory subscriptions] Platform not found {}。Ignored", platformId);
			return;
		}

		String sn = XmlUtil.getText(rootElement, "SN");
		log.info("[Respond to directory subscription requests from superiors]: {}/{}", platformId, deviceId);
		StringBuilder resultXml = new StringBuilder(200);
		resultXml.append("<?xml version=\"1.0\" ?>\r\n")
				.append("<Response>\r\n")
				.append("<CmdType>Catalog</CmdType>\r\n")
				.append("<SN>").append(sn).append("</SN>\r\n")
				.append("<DeviceID>").append(deviceId).append("</DeviceID>\r\n")
				.append("<Result>OK</Result>\r\n")
				.append("</Response>\r\n");

		try {
			int expires = request.getExpires().getExpires();
			Platform parentPlatform = platformService.queryPlatformByServerGBId(platformId);
			SIPResponse response = responseXmlAck(request, resultXml.toString(), parentPlatform, expires);

			SubscribeInfo subscribeInfo = SubscribeInfo.getInstance(response, platformId, expires,
					(EventHeader)request.getHeader(EventHeader.NAME));

			if (subscribeInfo.getExpires() == 0) {
				subscribeHolder.removeCatalogSubscribe(platformId);
			}else {
				subscribeInfo.setTransactionInfo(new SipTransactionInfo(response));
				subscribeHolder.putCatalogSubscribe(platformId, subscribeInfo);
			}
		} catch (SipException | InvalidArgumentException | ParseException e) {
			log.error("unhandled exception ", e);
		}
		if (subscribeHolder.getCatalogSubscribe(platformId) == null
				&& platform.getAutoPushChannel() != null && platform.getAutoPushChannel()) {
			platformService.addSimulatedSubscribeInfo(platform);
		}
	}
}
