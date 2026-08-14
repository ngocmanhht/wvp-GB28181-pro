package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dom4j.Element;

@Data
@Schema(description = "SVACDecoding configuration")
public class SVACDecodeConfig implements DeviceConfigAware {

    @Schema(description = "SVCparameters")
    private SVCParam svcParam;

    @Schema(description = "Monitor special information parameters")
    private SurveillanceParam surveillanceParam;

    @Override
    public String configType() {
        return "SVACDecodeConfig";
    }

    @Override
    public void fromXml(Element element) {
        Element svcEl = element.element("SVCParam");
        if (svcEl != null) {
            SVCParam s = new SVCParam();
            s.fromXml(svcEl);
            setSvcParam(s);
        }
        Element survEl = element.element("SurveillanceParam");
        if (survEl != null) {
            SurveillanceParam s = new SurveillanceParam();
            s.fromXml(survEl);
            setSurveillanceParam(s);
        }
    }

    @Data
    public static class SVCParam {
        @Schema(description = "Airspace coding capability, 0: not supported, 1: level 1 enhancement, 2: level 2 enhancement, 3: level 3 enhancement")
        private Integer SVCSpaceSupportMode;

        @Schema(description = "Time domain coding capability, 0: not supported, 1: level 1 enhancement, 2: level 2 enhancement, 3: level 3 enhancement")
        private Integer SVCTimeSupportMode;

        public void fromXml(Element element) {
            setSVCSpaceSupportMode(XmlUtil.getInteger(element, "SVCSpaceSupportMode"));
            setSVCTimeSupportMode(XmlUtil.getInteger(element, "SVCTimeSupportMode"));
        }
    }

    @Data
    public static class SurveillanceParam {
        @Schema(description = "Absolute time information display switch, 0: off, 1: on")
        private Integer TimeShowFlag;

        @Schema(description = "Monitoring event information display switch, 0: off, 1: on")
        private Integer EventShowFlag;

        @Schema(description = "Alarm information display switch, 0: off, 1: on")
        private Integer AlerShowtFlag;

        public void fromXml(Element element) {
            setTimeShowFlag(XmlUtil.getInteger(element, "TimeShowFlag"));
            setEventShowFlag(XmlUtil.getInteger(element, "EventShowFlag"));
            setAlerShowtFlag(XmlUtil.getInteger(element, "AlerShowtFlag"));
        }
    }
}
