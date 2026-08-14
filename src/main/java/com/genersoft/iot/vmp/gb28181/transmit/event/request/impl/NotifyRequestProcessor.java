package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.gb28181.bean.CmdType;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * SIPCommand type: NOTIFY request. The device will respond only after the superior sends a subscription request.
 */
@Slf4j
@Component
public class NotifyRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private final String method = "NOTIFY";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private NotifyRequestForCatalogProcessor notifyRequestForCatalogProcessor;

	@Autowired
	private NotifyRequestForMobilePositionProcessor notifyRequestForMobilePositionProcessor;

	@Autowired
	private NotifyRequestForAlarm notifyRequestForAlarm;

	@Override
	public void afterPropertiesSet() throws Exception {
		// Add message processing subscription
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	@Override
	public void process(RequestEvent evt) {
		try {
			responseAckAsync((SIPRequest) evt.getRequest(), Response.OK);
			Element rootElement = getRootElement(evt);
			if (rootElement == null) {
				log.error("The message body was not obtained when processing the NOTIFY message.,{}", evt.getRequest());
				return;
			}
			String cmd = XmlUtil.getText(rootElement, "CmdType");

			if (CmdType.CATALOG.equals(cmd)) {
				notifyRequestForCatalogProcessor.process(evt);
			} else if (CmdType.ALARM.equals(cmd)) {
				notifyRequestForAlarm.process(evt);
			} else if (CmdType.MOBILE_POSITION.equals(cmd)) {
				notifyRequestForMobilePositionProcessor.process(evt);
			} else {
				log.info("[Notify] Location type message received：{}, \r\n {}",  cmd, evt.getRequest());
			}
		} catch (SipException | InvalidArgumentException | ParseException e) {
			log.error("unhandled exception ", e);
		} catch (DocumentException e) {
			throw new RuntimeException(e);
		}
	}
}
