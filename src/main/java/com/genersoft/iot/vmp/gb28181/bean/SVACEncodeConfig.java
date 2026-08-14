package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "SVACencoding configuration")
public class SVACEncodeConfig implements DeviceConfigAware {

    @Schema(description = "Region of interest parameters")
    private ROIParam roiParam;

    @Schema(description = "SVCparameters")
    private SVCParam svcParam;

    @Schema(description = "Monitor special information parameters")
    private SurveillanceParam surveillanceParam;

    @Schema(description = "Audio parameters")
    private AudioParam audioParam;

    @Override
    public String configType() {
        return "SVACEncodeConfig";
    }

    @Override
    public void fromXml(Element element) {
        Element roiEl = element.element("ROIParam");
        if (roiEl != null) {
            ROIParam r = new ROIParam();
            r.fromXml(roiEl);
            setRoiParam(r);
        }
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
        Element audioEl = element.element("AudioParam");
        if (audioEl != null) {
            AudioParam a = new AudioParam();
            a.fromXml(audioEl);
            setAudioParam(a);
        }
    }

    @Data
    public static class ROIParam {
        @Schema(description = "Region of interest switch, 0: off, 1: on")
        private Integer ROIFlag;

        @Schema(description = "Number of regions of interest, value range0~16")
        private Integer ROINumber;

        @Schema(description = "List of areas of interest")
        private List<ROIItem> Item;

        @Schema(description = "Background area encoding quality level, 0: average, 1: better, 2: good, 3: very good")
        private Integer BackGroundQP;

        @Schema(description = "Background skip switch, 0: off, 1: on")
        private Integer BackGroundSkipFlag;

        public void fromXml(Element element) {
            setROIFlag(XmlUtil.getInteger(element, "ROIFlag"));
            setROINumber(XmlUtil.getInteger(element, "ROINumber"));
            List<Element> itemElements = element.elements("Item");
            if (!itemElements.isEmpty()) {
                List<ROIItem> list = new ArrayList<>();
                for (Element e : itemElements) {
                    ROIItem item = new ROIItem();
                    item.fromXml(e);
                    list.add(item);
                }
                setItem(list);
            }
            setBackGroundQP(XmlUtil.getInteger(element, "BackGroundQP"));
            setBackGroundSkipFlag(XmlUtil.getInteger(element, "BackGroundSkipFlag"));
        }
    }

    @Data
    public static class ROIItem {
        @Schema(description = "Area of interest number, value range1~16")
        private Integer ROISeq;

        @Schema(description = "Coordinates of the upper left corner of the area of interest, value range0~19683")
        private Integer TopLeft;

        @Schema(description = "Coordinates of the lower right corner of the area of interest, value range0~19683")
        private Integer BottomRight;

        @Schema(description = "ROIRegional encoding quality level, 0: Fair, 1: Better, 2: Good, 3: Very good")
        private Integer ROIQP;

        public void fromXml(Element element) {
            setROISeq(XmlUtil.getInteger(element, "ROISeq"));
            setTopLeft(XmlUtil.getInteger(element, "TopLeft"));
            setBottomRight(XmlUtil.getInteger(element, "BottomRight"));
            setROIQP(XmlUtil.getInteger(element, "ROIQP"));
        }

    }

    @Data
    public static class SVCParam {
        @Schema(description = "Airspace coding method, 0: basic layer, 1:1 level enhancement, 2:2 level enhancement, 3:3 level enhancement")
        private Integer SVCSpaceDomainMode;

        @Schema(description = "Time domain coding method, 0: basic layer, 1: 1-level enhancement, 2: 2-level enhancement, 3: 3-level enhancement")
        private Integer SVCTimeDomainMode;

        @Schema(description = "Airspace coding capability, 0: not supported, 1: level 1 enhancement, 2: level 2 enhancement, 3: level 3 enhancement")
        private Integer SVCSpaceSupportMode;

        @Schema(description = "Time domain coding capability, 0: not supported, 1: level 1 enhancement, 2: level 2 enhancement, 3: level 3 enhancement")
        private Integer SVCTimeSupportMode;

        public void fromXml(Element element) {
            setSVCSpaceDomainMode(XmlUtil.getInteger(element, "SVCSpaceDomainMode"));
            setSVCTimeDomainMode(XmlUtil.getInteger(element, "SVCTimeDomainMode"));
            setSVCSpaceSupportMode(XmlUtil.getInteger(element, "SVCSpaceSupportMode"));
            setSVCTimeSupportMode(XmlUtil.getInteger(element, "SVCTimeSupportMode"));
        }
    }

    @Data
    public static class SurveillanceParam {
        @Schema(description = "Absolute time information switch, 0: off, 1: on")
        private Integer TimeFlag;

        @Schema(description = "Monitoring event information switch, 0: off, 1: on")
        private Integer EventFlag;

        @Schema(description = "Alarm information switch, 0: off, 1: on")
        private Integer AlertFlag;

        public void fromXml(Element element) {
            setTimeFlag(XmlUtil.getInteger(element, "TimeFlag"));
            setEventFlag(XmlUtil.getInteger(element, "EventFlag"));
            setAlertFlag(XmlUtil.getInteger(element, "AlertFlag"));
        }
    }

    @Data
    public static class AudioParam {
        @Schema(description = "Voice recognition feature parameter switch, 0: off, 1: on")
        private Integer AudioRecognitionFlag;

        public void fromXml(Element element) {
            setAudioRecognitionFlag(XmlUtil.getInteger(element, "AudioRecognitionFlag"));
        }
    }
}
