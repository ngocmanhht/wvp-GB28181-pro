package com.genersoft.iot.vmp.web.custom.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.service.IMapService;
import com.genersoft.iot.vmp.vmanager.bean.MapConfig;
import com.genersoft.iot.vmp.vmanager.bean.MapModelIcon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Third-party platform adaptation
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "sy.enable", havingValue = "true")
public class SyServiceImpl implements IMapService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<MapConfig> getConfig() {
        List<MapConfig> configList = new ArrayList<>();
        JSONObject configObject = (JSONObject)redisTemplate.opsForValue().get("interfaceConfig1");
        if (configObject == null) {
            return configList;
        }
        // light map
        MapConfig mapConfigForDefault = readConfig("FRAGMENTIMG_SERVER", configObject);
        if (mapConfigForDefault != null) {
            mapConfigForDefault.setName("light map");
            configList.add(mapConfigForDefault);
        }

        // dark map
        MapConfig mapConfigForDark = readConfig("POLARNIGHTBLUE_FRAGMENTIMG_SERVER", configObject);
        if (mapConfigForDark != null) {
            mapConfigForDark.setName("dark map");
            configList.add(mapConfigForDark);
        }

        // satellite map
        MapConfig mapConfigForSatellited = readConfig("SATELLITE_FRAGMENTIMG_SERVER", configObject);
        if (mapConfigForSatellited != null) {
            mapConfigForSatellited.setName("satellite map");
            configList.add(mapConfigForSatellited);
        }
        return configList;
    }

    private MapConfig readConfig(String key, JSONObject jsonObject) {
        JSONArray fragmentimgServerArray = jsonObject.getJSONArray(key);
        if (fragmentimgServerArray == null || fragmentimgServerArray.isEmpty()) {
            return null;
        }
        JSONObject fragmentimgServer = fragmentimgServerArray.getJSONObject(0);
        // coordinate system
        String geoCoordSys = fragmentimgServer.getString("csysType").toUpperCase();
        // Get address
        String path = fragmentimgServer.getString("path");
        String ip = fragmentimgServer.getString("ip");
        JSONObject portJson = fragmentimgServer.getJSONObject("port");
        JSONObject httpPortJson = portJson.getJSONObject("httpPort");
        String protocol = httpPortJson.getString("portType");
        Integer port = httpPortJson.getInteger("port");
        String tileUrl = String.format("%s://%s:%s%s", protocol, ip, port, path);
        MapConfig mapConfig = new MapConfig();
        mapConfig.setCoordinateSystem(geoCoordSys);
        mapConfig.setTilesUrl(tileUrl);
        return mapConfig;

    }

    @Override
    public List<MapModelIcon> getModelList() {
        // Read redis icon information
        /*
          {
              "brand": "WVP",
              "createdTime": 1715845840000,
              "displayInSelect": true,
              "id": 12,
              "imagesPath": "images/lt132",
              "machineName": "Picture transmission intercom individual soldier",
              "machineType": "LT132"
           },
         */
        List<MapModelIcon> mapModelIconList = new ArrayList<>();
        JSONArray jsonArray = (JSONArray) redisTemplate.opsForValue().get("machineInfo");
        if (jsonArray != null && !jsonArray.isEmpty()) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String machineType = jsonObject.getString("machineType");
                String machineName = jsonObject.getString("machineName");
                String imagesPath = jsonObject.getString("imagesPath");

                mapModelIconList.add(MapModelIcon.getInstance(machineType, machineName, imagesPath));
            }
        }
        return mapModelIconList;
    }
}
