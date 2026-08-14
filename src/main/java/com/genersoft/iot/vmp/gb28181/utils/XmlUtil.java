package com.genersoft.iot.vmp.gb28181.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import javax.sip.RequestEvent;
import javax.sip.message.Request;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Toolkit based on dom4j
 */
@Slf4j
public class XmlUtil {

    /**
     * Parse XML into Document object
     */
    public static Element parseXml(String xml) {
        Document document = null;
        //
        StringReader sr = new StringReader(xml);
        SAXReader saxReader = new SAXReader();
        try {
            document = saxReader.read(sr);
        } catch (DocumentException e) {
            log.error("Parsing failed", e);
        }
        return null == document ? null : document.getRootElement();
    }

    /**
     * Get the value of text of element object
     *
     * @param em  node object
     * @param tag Nodaltag
     * @return node
     */
    public static String getText(Element em, String tag) {
        if (null == em) {
            return null;
        }
        Element e = em.element(tag);
        //
        return null == e ? null : e.getText().trim();
    }

    /**
     * Get the value of text of element object
     *
     * @param em  node object
     * @param tag Nodaltag
     * @return node
     */
    public static Double getDouble(Element em, String tag) {
        if (null == em) {
            return null;
        }
        Element e = em.element(tag);
        if (null == e) {
            return null;
        }
        String text = e.getText().trim();
        if (ObjectUtils.isEmpty(text) || !NumberUtils.isParsable(text)) {
            return null;
        }
        return Double.parseDouble(text);
    }

    /**
     * Get the value of text of element object
     *
     * @param em  node object
     * @param tag Nodaltag
     * @return node
     */
    public static Integer getInteger(Element em, String tag) {
        if (null == em) {
            return null;
        }
        Element e = em.element(tag);
        if (null == e) {
            return null;
        }
        String text = e.getText().trim();
        if (ObjectUtils.isEmpty(text) || !NumberUtils.isParsable(text)) {
            return null;
        }
        return Integer.parseInt(text);
    }

    /**
     * Recursively parse xml nodes, suitable for multi-node data
     *
     * @param node     node
     * @param nodeName nodeName
     * @return List<Map<String, Object>>
     */
    public static List<Map<String, Object>> listNodes(Element node, String nodeName) {
        if (null == node) {
            return null;
        }
        // initialization return
        List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
        // First get all attribute nodes of the current node
        List<Attribute> list = node.attributes();

        Map<String, Object> map = null;
        // Traverse attribute nodes
        for (Attribute attribute : list) {
            if (nodeName.equals(node.getName())) {
                if (null == map) {
                    map = new HashMap<String, Object>();
                    listMap.add(map);
                }
                // Put the obtained node attributes into the map
                map.put(attribute.getName(), attribute.getValue());
            }

        }
        // Traverse all nodes under the current node, nodeName is the name of the node to be parsed
        // Use recursion
        Iterator<Element> iterator = node.elementIterator();
        while (iterator.hasNext()) {
            Element e = iterator.next();
            listMap.addAll(listNodes(e, nodeName));
        }
        return listMap;
    }

    /**
     * xmlturnjson
     *
     * @param element
     * @param json
     */
    public static void node2Json(Element element, JSONObject json) {
        // If it is an attribute
        for (Object o : element.attributes()) {
            Attribute attr = (Attribute) o;
            if (!ObjectUtils.isEmpty(attr.getValue())) {
                json.put("@" + attr.getName(), attr.getValue());
            }
        }
        List<Element> chdEl = element.elements();
        if (chdEl.isEmpty() && !ObjectUtils.isEmpty(element.getText())) {// If there are no child elements, there is only one value
            json.put(element.getName(), element.getText());
        }

        for (Element e : chdEl) {   // has child elements
            if (!e.elements().isEmpty()) {  // Child elements also have child elements
                JSONObject chdjson = new JSONObject();
                node2Json(e, chdjson);
                Object o = json.get(e.getName());
                if (o != null) {
                    JSONArray jsona = null;
                    if (o instanceof JSONObject) {  // If this element already exists, it will be converted tojsonArray
                        JSONObject jsono = (JSONObject) o;
                        json.remove(e.getName());
                        jsona = new JSONArray();
                        jsona.add(jsono);
                        jsona.add(chdjson);
                    }
                    if (o instanceof JSONArray) {
                        jsona = (JSONArray) o;
                        jsona.add(chdjson);
                    }
                    json.put(e.getName(), jsona);
                } else {
                    if (!chdjson.isEmpty()) {
                        json.put(e.getName(), chdjson);
                    }
                }
            } else { // Child element has no child elements
                for (Object o : element.attributes()) {
                    Attribute attr = (Attribute) o;
                    if (!ObjectUtils.isEmpty(attr.getValue())) {
                        json.put("@" + attr.getName(), attr.getValue());
                    }
                }
                if (!e.getText().isEmpty()) {
                    json.put(e.getName(), e.getText());
                }
            }
        }
    }
    public static  Element getRootElement(RequestEvent evt) throws DocumentException {

        return getRootElement(evt, "gb2312");
    }

    public static Element getRootElement(RequestEvent evt, String charset) throws DocumentException {
        Request request = evt.getRequest();
        return getRootElement(request.getRawContent(), charset);
    }

    public static Element getRootElement(byte[] content, String charset) throws DocumentException {
        if (charset == null) {
            charset = "gb2312";
        }
        SAXReader reader = new SAXReader();
        reader.setEncoding(charset);
        Document xml = reader.read(new ByteArrayInputStream(content));
        return xml.getRootElement();
    }

    private enum ChannelType{
        CivilCode, BusinessGroup,VirtualOrganization,Other
    }

//    public static DeviceChannel channelContentHandler(Element itemDevice, Device device, String event){
//        DeviceChannel deviceChannel = new DeviceChannel();
//        deviceChannel.setDeviceId(device.getDeviceId());
//        Element channdelIdElement = itemDevice.element("DeviceID");
//        if (channdelIdElement == null) {
//            logger.warn("When parsing the Catalog message, it was found that the DeviceID");
//            return null;
//        }
//        String channelId = channdelIdElement.getTextTrim();
//        if (ObjectUtils.isEmpty(channelId)) {
//            logger.warn("When parsing the Catalog message, it was found that the DeviceID");
//            return null;
//        }
//        deviceChannel.setDeviceId(channelId);
//        if (event != null && !event.equals(CatalogEvent.ADD) && !event.equals(CatalogEvent.UPDATE)) {
//            // All content needs to be identified except for ADD and update.，
//            return deviceChannel;
//        }
//        Element nameElement = itemDevice.element("Name");
//        // When the channel name is empty, set the channel name to the channel code to avoid the upper-level receiving channel failure due to the empty channel name during cascading.
//        if (nameElement != null && StringUtils.isNotBlank(nameElement.getText())) {
//            deviceChannel.setName(nameElement.getText());
//        } else {
//            deviceChannel.setName(channelId);
//        }
//        if(channelId.length() <= 8) {
//            deviceChannel.setHasAudio(false);
//            CivilCodePo parentCode = CivilCodeUtil.INSTANCE.getParentCode(channelId);
//            if (parentCode != null) {
//                deviceChannel.setParentId(parentCode.getCode());
//                deviceChannel.setCivilCode(parentCode.getCode());
//            }else {
//                logger.warn("[xmlparse] Unable to determine administrative division{}the superior administrative division of", channelId);
//            }
//            deviceChannel.setStatus("ON");
//            return deviceChannel;
//        }else {
//            if(channelId.length() != 20) {
//                logger.warn("[xmlparse] Failed, the number does not meet the definition of national standard 28181： {}", channelId);
//                return null;
//            }
//
//            int code = Integer.parseInt(channelId.substring(10, 13));
//            if (code == 136 || code == 137 || code == 138) {
//                deviceChannel.setHasAudio(true);
//            }else {
//                deviceChannel.setHasAudio(false);
//            }
//            // Equipment manufacturer
//            String manufacturer = getText(itemDevice, "Manufacturer");
//            // Device model
//            String model = getText(itemDevice, "Model");
//            // Equipment ownership
//            String owner = getText(itemDevice, "Owner");
//            // Administrative region
//            String civilCode = getText(itemDevice, "CivilCode");
//            // The business group ID to which the virtual organization belongs. The business group is formulated according to specific business needs. A business group contains a specific group of virtual organizations.
//            String businessGroupID = getText(itemDevice, "BusinessGroupID");
//            // parent device/area/systemID
//            String parentID = getText(itemDevice, "ParentID");
//            if (parentID != null && parentID.equalsIgnoreCase("null")) {
//                parentID = null;
//            }
//            // Registration method(Required)The default is1;1:Certification registration model that complies with IETFRFC3261 standard;2:Password-based two-way authentication registration mode;3:Two-way authentication registration mode based on digital certificate
//            String registerWay = getText(itemDevice, "RegisterWay");
//            // Confidential attribute(Required)The default is0;0:Not confidential, 1: confidential
//            String secrecy = getText(itemDevice, "Secrecy");
//            // Installation address
//            String address = getText(itemDevice, "Address");
//
//            switch (code){
//                case 200:
//                    // System directory
//                    if (!ObjectUtils.isEmpty(manufacturer)) {
//                        deviceChannel.setManufacture(manufacturer);
//                    }
//                    if (!ObjectUtils.isEmpty(model)) {
//                        deviceChannel.setModel(model);
//                    }
//                    if (!ObjectUtils.isEmpty(owner)) {
//                        deviceChannel.setOwner(owner);
//                    }
//                    if (!ObjectUtils.isEmpty(civilCode)) {
//                        deviceChannel.setCivilCode(civilCode);
//                        deviceChannel.setParentId(civilCode);
//                    }else {
//                        if (!ObjectUtils.isEmpty(parentID)) {
//                            deviceChannel.setParentId(parentID);
//                        }
//                    }
//                    if (!ObjectUtils.isEmpty(address)) {
//                        deviceChannel.setAddress(address);
//                    }
//                    deviceChannel.setStatus(true);
//                    if (!ObjectUtils.isEmpty(registerWay)) {
//                        try {
//                            deviceChannel.setRegisterWay(Integer.parseInt(registerWay));
//                        }catch (NumberFormatException exception) {
//                            logger.warn("[xmlparse] Failed to get registerWay from channel data： {}", registerWay);
//                        }
//                    }
//                    if (!ObjectUtils.isEmpty(secrecy)) {
//                        deviceChannel.setSecrecy(secrecy);
//                    }
//                    return deviceChannel;
//                case 215:
//                    // business grouping
//                    deviceChannel.setStatus(true);
//                    if (!ObjectUtils.isEmpty(parentID)) {
//                        if (!parentID.trim().equalsIgnoreCase(device.getDeviceId())) {
//                            deviceChannel.setParentId(parentID);
//                        }
//                    }else {
//                        logger.warn("[xmlparse] Key information is missing from business grouping data->ParentId");
//                        if (!ObjectUtils.isEmpty(civilCode)) {
//                            deviceChannel.setCivilCode(civilCode);
//                        }
//                    }
//                    break;
//                case 216:
//                    // virtual organization
//                    deviceChannel.setStatus(true);
//                    if (!ObjectUtils.isEmpty(businessGroupID)) {
//                        deviceChannel.setBusinessGroupId(businessGroupID);
//                    }
//
//                    if (!ObjectUtils.isEmpty(parentID)) {
//                        if (parentID.contains("/")) {
//                            String[] parentIdArray = parentID.split("/");
//                            parentID = parentIdArray[parentIdArray.length - 1];
//                        }
//                        deviceChannel.setParentId(parentID);
//                    }else {
//                        if (!ObjectUtils.isEmpty(businessGroupID)) {
//                            deviceChannel.setParentId(businessGroupID);
//                        }
//                    }
//                    break;
//                default:
//                    // Device catalog
//                    if (!ObjectUtils.isEmpty(manufacturer)) {
//                        deviceChannel.setManufacture(manufacturer);
//                    }
//                    if (!ObjectUtils.isEmpty(model)) {
//                        deviceChannel.setModel(model);
//                    }
//                    if (!ObjectUtils.isEmpty(owner)) {
//                        deviceChannel.setOwner(owner);
//                    }
//                    if (!ObjectUtils.isEmpty(civilCode)
//                            && civilCode.length() <= 8
//                            && NumberUtils.isParsable(civilCode)
//                            && civilCode.length()%2 == 0
//                    ) {
//                        deviceChannel.setCivilCode(civilCode);
//                    }
//                    if (!ObjectUtils.isEmpty(businessGroupID)) {
//                        deviceChannel.setBusinessGroupId(businessGroupID);
//                    }
//
//                    // police district
//                    String block = getText(itemDevice, "Block");
//                    if (!ObjectUtils.isEmpty(block)) {
//                        deviceChannel.setBlock(block);
//                    }
//                    if (!ObjectUtils.isEmpty(address)) {
//                        deviceChannel.setAddress(address);
//                    }
//
//                    if (!ObjectUtils.isEmpty(secrecy)) {
//                        deviceChannel.setSecrecy(secrecy);
//                    }
//
//                    // When it is a device, whether there are sub-devices(Required)1Yes, 0 No
//                    String parental = getText(itemDevice, "Parental");
//                    if (!ObjectUtils.isEmpty(parental)) {
//                        try {
//                            // Since Hikvision will mistakenly send 65535 as the value here, it is considered to be 0 unless it is 0.1
//                            if (!ObjectUtils.isEmpty(parental) && parental.length() == 1 && Integer.parseInt(parental) == 0) {
//                                deviceChannel.setParental(0);
//                            }else {
//                                deviceChannel.setParental(1);
//                            }
//                        }catch (NumberFormatException e) {
//                            logger.warn("[xmlparse] Failed to get parental from channel data： {}", parental);
//                        }
//                    }
//                    // parent device/area/systemID
//
//                    if (!ObjectUtils.isEmpty(parentID) ) {
//                        if (parentID.contains("/")) {
//                            String[] parentIdArray = parentID.split("/");
//                            deviceChannel.setParentId(parentIdArray[parentIdArray.length - 1]);
//                        }else {
//                            if (parentID.length()%2 == 0) {
//                                deviceChannel.setParentId(parentID);
//                            }else {
//                                logger.warn("[xmlparse] irregularparentID：{}, Abandoned", parentID);
//                            }
//                        }
//                    }else {
//                        if (!ObjectUtils.isEmpty(businessGroupID)) {
//                            deviceChannel.setParentId(businessGroupID);
//                        }else {
//                            if (!ObjectUtils.isEmpty(deviceChannel.getCivilCode())) {
//                                deviceChannel.setParentId(deviceChannel.getCivilCode());
//                            }
//                        }
//                    }
//                    // Registration method
//                    if (!ObjectUtils.isEmpty(registerWay)) {
//                        try {
//                            int registerWayInt = Integer.parseInt(registerWay);
//                            deviceChannel.setRegisterWay(registerWayInt);
//                        }catch (NumberFormatException exception) {
//                            logger.warn("[xmlparse] Failed to get registerWay from channel data： {}", registerWay);
//                            deviceChannel.setRegisterWay(1);
//                        }
//                    }else {
//                        deviceChannel.setRegisterWay(1);
//                    }
//
//                    // Signaling security mode(Optional)The default is0; 0:Not adopted;2:S/MIME Signature method;3:S/MIMEEncrypted signature simultaneous use method;4:digital summary method
//                    String safetyWay = getText(itemDevice, "SafetyWay");
//                    if (!ObjectUtils.isEmpty(safetyWay)) {
//                        try {
//                            deviceChannel.setSafetyWay(Integer.parseInt(safetyWay));
//                        }catch (NumberFormatException e) {
//                            logger.warn("[xmlparse] Failed to get safetyWay from channel data： {}", safetyWay);
//                        }
//                    }
//
//                    // Certificate serial number(Devices with certificates are required)
//                    String certNum = getText(itemDevice, "CertNum");
//                    if (!ObjectUtils.isEmpty(certNum)) {
//                        deviceChannel.setCertNum(certNum);
//                    }
//
//                    // Certificate valid identifier(Devices with certificates are required)The default is0;Certificate valid identification: 0: invalid 1: valid
//                    String certifiable = getText(itemDevice, "Certifiable");
//                    if (!ObjectUtils.isEmpty(certifiable)) {
//                        try {
//                            deviceChannel.setCertifiable(Integer.parseInt(certifiable));
//                        }catch (NumberFormatException e) {
//                            logger.warn("[xmlparse] Failed to get Certifiable from channel data： {}", certifiable);
//                        }
//                    }
//
//                    // Invalid reason code(Required for devices with certificates and invalid certificates)
//                    String errCode = getText(itemDevice, "ErrCode");
//                    if (!ObjectUtils.isEmpty(errCode)) {
//                        try {
//                            deviceChannel.setErrCode(Integer.parseInt(errCode));
//                        }catch (NumberFormatException e) {
//                            logger.warn("[xmlparse] Failed to get ErrCode from channel data： {}", errCode);
//                        }
//                    }
//
//                    // Certificate expiry date(Devices with certificates are required)
//                    String endTime = getText(itemDevice, "EndTime");
//                    if (!ObjectUtils.isEmpty(endTime)) {
//                        deviceChannel.setEndTime(endTime);
//                    }
//
//
//                    // Equipment/area/System IP address
//                    String ipAddress = getText(itemDevice, "IPAddress");
//                    if (!ObjectUtils.isEmpty(ipAddress)) {
//                        deviceChannel.setIpAddress(ipAddress);
//                    }
//
//                    // Equipment/area/system port
//                    String port = getText(itemDevice, "Port");
//                    if (!ObjectUtils.isEmpty(port)) {
//                        try {
//                            deviceChannel.setPort(Integer.parseInt(port));
//                        }catch (NumberFormatException e) {
//                            logger.warn("[xmlparse] Failed to obtain Port from channel data： {}", port);
//                        }
//                    }
//
//                    // Device password
//                    String password = getText(itemDevice, "Password");
//                    if (!ObjectUtils.isEmpty(password)) {
//                        deviceChannel.setPassword(password);
//                    }
//
//
//                    // Device status
//                    String status = getText(itemDevice, "Status");
//                    if (status != null) {
//                        // ONLINE OFFLINE HIKVISION DS-7716N-E4 NVRCompatibility processing
//                        if (status.equalsIgnoreCase("ON") || status.equalsIgnoreCase("On") || status.equalsIgnoreCase("ONLINE") || status.equalsIgnoreCase("OK")) {
//                            deviceChannel.setStatus(true);
//                        }
//                        if (status.equalsIgnoreCase("OFF") || status.equalsIgnoreCase("Off") || status.equalsIgnoreCase("OFFLINE")) {
//                            deviceChannel.setStatus(false);
//                        }
//                    }else {
//                        deviceChannel.setStatus(true);
//                    }
////                    logger.info("status string： {}", status);
////                    logger.info("status result： {}", deviceChannel.isStatus());
//                    // longitude
//                    String longitude = getText(itemDevice, "Longitude");
//                    if (NumericUtil.isDouble(longitude)) {
//                        deviceChannel.setLongitude(Double.parseDouble(longitude));
//                    } else {
//                        deviceChannel.setLongitude(0.00);
//                    }
//
//                    // Latitude
//                    String latitude = getText(itemDevice, "Latitude");
//                    if (NumericUtil.isDouble(latitude)) {
//                        deviceChannel.setLatitude(Double.parseDouble(latitude));
//                    } else {
//                        deviceChannel.setLatitude(0.00);
//                    }
//
//                    deviceChannel.setGpsTime(DateUtil.getNow());
//
//                    // -Camera type extension, identifying camera type:1-ball machine;2-hemisphere;3-Fixed bolt;4-Remote control gun. Optional when the directory item is a camera
//                    String ptzType = getText(itemDevice, "PTZType");
//                    if (ObjectUtils.isEmpty(ptzType)) {
//                        //Compatible with information in INFO
//                        Element info = itemDevice.element("Info");
//                        String ptzTypeFromInfo = XmlUtil.getText(info, "PTZType");
//                        if(!ObjectUtils.isEmpty(ptzTypeFromInfo)){
//                            try {
//                                deviceChannel.setPtzType(Integer.parseInt(ptzTypeFromInfo));
//                            }catch (NumberFormatException e){
//                                logger.warn("[xmlparse] Failed to obtain PTZType from channel data info： {}", ptzTypeFromInfo);
//                            }
//                        }
//                    } else {
//                        try {
//                            deviceChannel.setPtzType(Integer.parseInt(ptzType));
//                        }catch (NumberFormatException e){
//                            logger.warn("[xmlparse] Failed to get PTZType from channel data： {}", ptzType);
//                        }
//                    }
//
//                    // TODO Camera position type extension。
//                    // 1-interprovincial checkpoint、
//                    // 2-Party and government organs、
//                    // 3-Station Pier、
//                    // 4-central square、
//                    // 5-sports venues、
//                    // 6-business center、
//                    // 7-religious place、
//                    // 8-Around campus、
//                    // 9-Complex security area、
//                    // 10-traffic arteries。
//                    // String positionType = getText(itemDevice, "PositionType");
//
//                    // TODO Camera installation location outdoor and indoor attributes。1-outdoor、2-indoor。
//                    // String roomType = getText(itemDevice, "RoomType");
//                    // TODO Camera usage properties
//                    // String useType = getText(itemDevice, "UseType");
//                    // TODO Camera fill light properties。1-No fill light、2-Infrared fill light、3-white light fill light
//                    // String supplyLightType = getText(itemDevice, "SupplyLightType");
//                    // TODO Camera surveillance orientation attributes。1-East、2-west、3-South、4-north、5-Southeast、6-Northeast、7-Southwest、8-Northwest。
//                    // String directionType = getText(itemDevice, "DirectionType");
//                    // TODO The resolution supported by the camera can have multiple resolution values, and each value must be“/”Separate. For the resolution value, please refer to the SDPf field provisions in Appendix F.
//                    // String resolution = getText(itemDevice, "Resolution");
//
//                    // TODO Download speed range(Optional),Each optional parameter starts with“/”Separate, if the device supports 1, 2, 4 times download speed, it should be written as“1/2/4
//                    // String downloadSpeed = getText(itemDevice, "DownloadSpeed");
//                    // TODO Airspace coding capability, value 0: not supported;1:1level enhancement(1enhancement layer);2:2level enhancement(2enhancement layer);3:3level enhancement(3enhancement layer)
//                    // String svcSpaceSupportMode = getText(itemDevice, "SVCSpaceSupportMode");
//                    // TODO Time domain coding capability, value 0: not supported;1:1level enhancement;2:2level enhancement;3:3level enhancement
//                    // String svcTimeSupportMode = getText(itemDevice, "SVCTimeSupportMode");
//
//
//                    deviceChannel.setSecrecy(secrecy);
//                    break;
//            }
//        }
//
//        return deviceChannel;
//    }

    /**
     * New method supports internal nesting
     *
     * @param element xmlElement
     * @param clazz result class
     * @param <T> Generics
     * @return result object
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static <T> T loadElement(Element element, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        T t = clazz.getDeclaredConstructor().newInstance();
        for (Field field : fields) {
            ReflectionUtils.makeAccessible(field);
            MessageElement annotation = field.getAnnotation(MessageElement.class);
            if (annotation == null) {
                continue;
            }
            String value = annotation.value();
            String subVal = annotation.subVal();
            Element element1 = element.element(value);
            if (element1 == null) {
                continue;
            }
            if ("".equals(subVal)) {
                // No subordinate data
                Object fieldVal = element1.isTextOnly() ? element1.getText() : loadElement(element1, field.getType());
                Object o = simpleTypeDeal(field.getType(), fieldVal);
                ReflectionUtils.setField(field, t,  o);
            } else {
                // Subordinate data exists
                ArrayList<Object> list = new ArrayList<>();
                Type genericType = field.getGenericType();
                if (!(genericType instanceof ParameterizedType)) {
                    continue;
                }
                Class<?> aClass = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                for (Element element2 : element1.elements(subVal)) {
                    list.add(loadElement(element2, aClass));
                }
                ReflectionUtils.setField(field, t, list);
            }
        }
        return t;
    }

    public static <T> T elementDecode(Element element, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        T t = clazz.getDeclaredConstructor().newInstance();
        for (Field field : fields) {
            ReflectionUtils.makeAccessible(field);
            MessageElementForCatalog annotation = field.getAnnotation(MessageElementForCatalog.class);
            if (annotation == null) {
                continue;
            }
            String[] values = annotation.value();
            for (String value : values) {
                boolean subVal = value.contains(".");
                if (!subVal) {
                    Element element1 = element.element(value);
                    if (element1 == null) {
                        continue;
                    }
                    // No subordinate data
                    Object fieldVal = element1.isTextOnly() ? element1.getText() : loadElement(element1, field.getType());
                    Object o = simpleTypeDeal(field.getType(), fieldVal);
                    ReflectionUtils.setField(field, t,  o);
                    break;
                } else {
                    String[] pathArray = value.split("\\.");
                    Element subElement = element;
                    for (String path : pathArray) {
                        subElement = subElement.element(path);
                        if (subElement == null) {
                            break;
                        }
                    }
                    if (subElement == null) {
                        continue;
                    }
                    Object fieldVal = subElement.isTextOnly() ? subElement.getText() : loadElement(subElement, field.getType());
                    Object o = simpleTypeDeal(field.getType(), fieldVal);
                    ReflectionUtils.setField(field, t,  o);
                }
            }

        }
        return t;
    }

    /**
     * Simple type handling
     *
     * @param tClass
     * @param val
     * @return
     */
    private static Object simpleTypeDeal(Class<?> tClass, Object val) {
        try {
            if (val == null || val.toString().equalsIgnoreCase("null")) {
                return null;
            }
            if (tClass.equals(String.class)) {
                return val.toString();
            }
            if (tClass.equals(Integer.class)) {
                return Integer.valueOf(val.toString());
            }
            if (tClass.equals(Double.class)) {
                return Double.valueOf(val.toString());

            }
            if (tClass.equals(Long.class)) {
                return Long.valueOf(val.toString());
            }
            return val;
        }catch (Exception e) {
            return null;
        }
    }
}