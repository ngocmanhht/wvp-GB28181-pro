package com.genersoft.iot.vmp.gb28181.transmit.event.request;

import javax.sip.RequestEvent;

/**
 * @description: Process SIP events, includingrequest， response， timeout， ioException, transactionTerminated,dialogTerminated
 * @author: panlinlin
 * @date:   2021November 5 15：47
 */
public interface ISIPRequestProcessor {

	void process(RequestEvent event);

}
