package com.genersoft.iot.vmp.gb28181.bean;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * @description:Device recording informationbean
 * @author: swwheihei
 * @date:   2020May 8, 2018, afternoon2:05:56
 */
@Setter
@Getter
@Schema(description = "Device recording query result information")
public class RecordInfo {

	@Schema(description = "Device number")
	private String deviceId;

	@Schema(description = "Channel number")
	private String channelId;

	@Schema(description = "command sequence number")
	private String sn;

	@Schema(description = "Device name")
	private String name;

	@Schema(description = "total number of lists")
	private int sumNum;

	private int count;

	private Instant lastTime;

	@Schema(description = "")
	private List<RecordItem> recordList;

}
