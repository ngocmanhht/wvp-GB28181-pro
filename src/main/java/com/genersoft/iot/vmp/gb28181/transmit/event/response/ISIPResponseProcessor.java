package com.genersoft.iot.vmp.gb28181.transmit.event.response;

import org.springframework.scheduling.annotation.Async;

import javax.sip.ResponseEvent;

/**    
 * @description:Process and receive the SIP protocol response message sent by IPCamera
 * @author: swwheihei
 * @date:   2020May 3rd, afternoon4:42:22     
 */
public interface ISIPResponseProcessor {


	void process(ResponseEvent evt);


}
