package com.genersoft.iot.vmp.gb28181.transmit.event.response.impl;

import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.event.response.SIPResponseProcessorAbstract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;

/**    
 * @description: CANCELresponse handler
 * @author: panlinlin
 * @date:   2021November 5 16:35
 */
@Component
public class CancelResponseProcessor extends SIPResponseProcessorAbstract {

	private final String method = "CANCEL";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// Add message processing subscription
		sipProcessorObserver.addResponseProcessor(method, this);
	}
	/**   
	 * Handling CANCEL response
	 *  
	 * @param evt
	 */
	@Override
	public void process(ResponseEvent evt) {
		// TODO Auto-generated method stub
		
	}

}
