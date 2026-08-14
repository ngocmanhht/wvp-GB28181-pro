package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * SIPCommand type: CANCEL request
 */
@Slf4j
@Component
public class CancelRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private final String method = "CANCEL";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// Add message processing subscription
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	/**
	 * Handling CANCEL requests
	 *
	 * @param evt event
	 */
	@Override
	public void process(RequestEvent evt) {
		// TODO priority99 Cancel RequestMessage implementation. This message is generally a cascade message. The superior sends a request to cancel the instruction to the subordinate.
		try {
			responseAck((SIPRequest) evt.getRequest(), Response.OK);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			log.error("[Command sending failed] Reply200 OK: {}", e.getMessage());
		}
	}

}
