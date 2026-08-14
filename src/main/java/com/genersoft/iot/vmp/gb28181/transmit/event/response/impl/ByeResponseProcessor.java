package com.genersoft.iot.vmp.gb28181.transmit.event.response.impl;

import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.response.SIPResponseProcessorAbstract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;

/**    
 * @description: BYErequest responder
 * @author: swwheihei
 * @date:   2020May 3rd, afternoon5:32:05     
 */
@Component
public class ByeResponseProcessor extends SIPResponseProcessorAbstract {

	private final String method = "BYE";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// Add message processing subscription
		sipProcessorObserver.addResponseProcessor(method, this);
	}
	/**
	 * Handling BYE responses
	 * 
	 * @param evt
	 */
	@Override
	public void process(ResponseEvent evt) {
		// TODO Auto-generated method stub
	}


}
