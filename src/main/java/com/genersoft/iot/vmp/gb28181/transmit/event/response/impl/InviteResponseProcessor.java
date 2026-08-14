package com.genersoft.iot.vmp.gb28181.transmit.event.response.impl;

import com.genersoft.iot.vmp.gb28181.bean.Gb28181Sdp;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.gb28181.transmit.event.response.SIPResponseProcessorAbstract;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.IpPortUtil;
import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.address.SipURI;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;


/**
 * @description: Handling INVITE response
 * @author: panlinlin
 * @date: 2021November 5 16：40
 */
@Slf4j
@Component
public class InviteResponseProcessor extends SIPResponseProcessorAbstract {

	private final String method = "INVITE";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Autowired
	private SIPSender sipSender;

	@Autowired
	private SIPRequestHeaderProvider headerProvider;

	@Override
	public void afterPropertiesSet() throws Exception {
		// Add message processing subscription
		sipProcessorObserver.addResponseProcessor(method, this);
	}



	/**
	 * Handle invite response
	 * 
	 * @param evt response message
	 * @throws ParseException
	 */
	@Override
	public void process(ResponseEvent evt ){
		log.debug("message received：" + evt.getResponse());
		try {
			SIPResponse response = (SIPResponse)evt.getResponse();
			int statusCode = response.getStatusCode();
			// tryingWill not reply
			if (statusCode == Response.TRYING) {
			}
			// successful response
			// Issueack
			if (statusCode == Response.OK) {
				ResponseEventExt event = (ResponseEventExt)evt;

				String contentString = new String(response.getRawContent());
				Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
				SessionDescription sdp = gb28181Sdp.getBaseSdb();
				SipURI requestUri = SipFactory.getInstance().createAddressFactory().createSipURI(sdp.getOrigin().getUsername(), IpPortUtil.concatenateIpAndPort(event.getRemoteIpAddress(), String.valueOf(event.getRemotePort())));
				Request reqAck = headerProvider.createAckRequest(response.getLocalAddress().getHostAddress(), requestUri, response);

				log.info("[Replyack] {}-> {}:{} ", sdp.getOrigin().getUsername(), event.getRemoteIpAddress(), event.getRemotePort());
				sipSender.transmitRequest( response.getLocalAddress().getHostAddress(), reqAck);
			}
		} catch (InvalidArgumentException | ParseException | SipException | SdpParseException e) {
			log.info("[Reply on demandACK]，Abnormal：", e );
		}
	}

}
