package com.genersoft.iot.vmp.gb28181.transmit.callback;

import lombok.Data;

/**
 * @description: Request information definition   
 * @author: swwheihei
 * @date:   2020May 8, 2018, afternoon1:09:18     
 */
@Data
public class RequestMessage {
	
	private String id;

	private String key;

	private Object data;
}
