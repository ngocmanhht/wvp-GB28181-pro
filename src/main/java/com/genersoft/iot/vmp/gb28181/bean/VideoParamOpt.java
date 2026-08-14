package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dom4j.Element;

@Data
@Schema(description = "Video parameter range")
public class VideoParamOpt implements DeviceConfigAware {

    @Schema(description = "EquipmentID")
    private String deviceId;

    @Schema(description = "Download speed range, each optional parameter is '/' separate")
    private String downloadSpeed;

    @Schema(description = "The resolution supported by the camera, multiple resolution values start with '/' separate")
    private String resolution;

    @Override
    public String configType() {
        return "VideoParamOpt";
    }

    @Override
    public void fromXml(Element element) {
        setDownloadSpeed(XmlUtil.getText(element, "DownloadSpeed"));
        setResolution(XmlUtil.getText(element, "Resolution"));
    }
}
