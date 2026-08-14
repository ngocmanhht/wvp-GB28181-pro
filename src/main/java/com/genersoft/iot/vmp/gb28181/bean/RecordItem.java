package com.genersoft.iot.vmp.gb28181.bean;


import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;

/**
 * @description:Equipment videobean
 * @author: swwheihei
 * @date:   2020May 8, 2018, afternoon2:06:54
 */
@Setter
@Getter
@Schema(description = "Device recording details")
public class RecordItem  implements Comparable<RecordItem>{

	@Schema(description = "Device number")
	private String deviceId;

	@Schema(description = "Name")
	private String name;

	@Schema(description = "file pathname (Optional)")
	private String filePath;

	@Schema(description = "Video file size, unit:Byte(Optional)")
	private String fileSize;

	@Schema(description = "Video address(Optional)")
	private String address;

	@Schema(description = "Recording start time(Optional)")
	private String startTime;

	@Schema(description = "Recording end time(Optional)")
	private String endTime;

	@Schema(description = "Confidential attribute(Required)The default is0;0:Not confidential, 1: confidential")
	private int secrecy;

	@Schema(description = "Video generation type(Optional)timeor alarm or manual")
	private String type;

	@Schema(description = "video triggerID(Optional)")
	private String recorderId;

    @Override
	public int compareTo(@NotNull RecordItem recordItem) {
		TemporalAccessor startTimeNow = DateUtil.formatter.parse(startTime);
		TemporalAccessor startTimeParam = DateUtil.formatter.parse(recordItem.getStartTime());
		Instant startTimeParamInstant = Instant.from(startTimeParam);
		Instant startTimeNowInstant = Instant.from(startTimeNow);
		if (startTimeNowInstant.equals(startTimeParamInstant)) {
			return 0;
		}else if (Instant.from(startTimeParam).isAfter(Instant.from(startTimeNow)) ) {
			return -1;
		}else {
			return 1;
		}

	}
}
