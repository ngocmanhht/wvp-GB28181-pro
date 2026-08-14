package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Region;
import com.genersoft.iot.vmp.gb28181.bean.RegionTree;
import com.genersoft.iot.vmp.gb28181.service.IRegionService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Area management")
@RestController
@RequestMapping("/api/region")
public class RegionController {

    private final static Logger logger = LoggerFactory.getLogger(RegionController.class);

    @Autowired
    private IRegionService regionService;

    @Operation(summary = "Add area")
    @Parameter(name = "region", description = "Region", required = true)
    @ResponseBody
    @PostMapping("/add")
    public void add(@RequestBody Region region){
        regionService.add(region);
    }

    @Operation(summary = "Query area")
    @Parameter(name = "query", description = "What to search for", required = true)
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    @ResponseBody
    @GetMapping("/page/list")
    public PageInfo<Region> query(
            @RequestParam(required = false) String query,
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int count
    ){
        return regionService.query(query, page, count);
    }

    @Operation(summary = "Query area node")
    @Parameter(name = "query", description = "What to search for", required = true)
    @Parameter(name = "parent", description = "Administrative division number", required = true)
    @Parameter(name = "hasChannel", description = "Whether to query the channel", required = true)
    @ResponseBody
    @GetMapping("/tree/list")
    public List<RegionTree> queryForTree(
            @RequestParam(required = false) Integer parent,
            @RequestParam(required = false) Boolean hasChannel
    ){
        return regionService.queryForTree(parent, hasChannel);
    }


    @Operation(summary = "Query area")
    @Parameter(name = "query", description = "What to search for", required = true)
    @Parameter(name = "channel", description = "trueis the query channel, false is the query node", required = true)
    @ResponseBody
    @GetMapping("/tree/query")
    public PageInfo<Region> queryTree(Integer page, Integer count,
            @RequestParam(required = true) String query
    ){
        return regionService.queryList(page, count, query);
    }

    @Operation(summary = "update area")
    @Parameter(name = "region", description = "Region", required = true)
    @ResponseBody
    @PostMapping("/update")
    public void update(@RequestBody Region region){
        regionService.update(region);
    }

    @Operation(summary = "delete area")
    @Parameter(name = "id", description = "areaID", required = true)
    @ResponseBody
    @DeleteMapping("/delete")
    public void delete(Integer id){
        Assert.notNull(id, "Region ID needs to exist");
        boolean result = regionService.deleteByDeviceId(id);
        if (!result) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Removal failed");
        }
    }

    @Operation(summary = "Query area based on area ID")
    @Parameter(name = "regionDeviceId", description = "Administrative division node number", required = true)
    @ResponseBody
    @GetMapping("/one")
    public Region queryRegionByDeviceId(
            @RequestParam(required = true) String regionDeviceId
    ){
        if (ObjectUtils.isEmpty(regionDeviceId.trim())) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        return regionService.queryRegionByDeviceId(regionDeviceId);
    }

    @Operation(summary = "Get the administrative division under the administrative division to which it belongs")
    @Parameter(name = "parent", description = "Administrative division to which it belongs", required = false)
    @ResponseBody
    @GetMapping("/base/child/list")
    public List<Region> getAllChild(@RequestParam(required = false) String parent){
        if (ObjectUtils.isEmpty(parent)) {
            parent = null;
        }
        return regionService.getAllChild(parent);
    }

    @Operation(summary = "Get the administrative division under the administrative division to which it belongs")
    @Parameter(name = "deviceId", description = "Current administrative divisions", required = false)
    @ResponseBody
    @GetMapping("/path")
    public List<Region> getPath(String deviceId){
        return regionService.getPath(deviceId);
    }

    @Operation(summary = "Synchronize administrative divisions from channels")
    @ResponseBody
    @GetMapping("/sync")
    public void sync(){
        regionService.syncFromChannel();
    }

    @Operation(summary = "Query the level and description from the file based on the administrative division number")
    @ResponseBody
    @GetMapping("/description")
    public String getDescription(String civilCode){
        return regionService.getDescription(civilCode);
    }

    @Operation(summary = "Query the level from the file based on the administrative division number and add")
    @ResponseBody
    @GetMapping("/addByCivilCode")
    public void addByCivilCode(String civilCode){
        regionService.addByCivilCode(civilCode);
    }


}
