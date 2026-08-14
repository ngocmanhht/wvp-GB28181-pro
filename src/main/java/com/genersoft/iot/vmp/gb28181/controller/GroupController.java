package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.GroupTree;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Group management")
@RestController
@RequestMapping("/api/group")
public class GroupController {

    @Autowired
    private IGroupService groupService;

    @Operation(summary = "Add group")
    @Parameter(name = "group", description = "group", required = true)
    @ResponseBody
    @PostMapping("/add")
    public void add(@RequestBody Group group){
        groupService.add(group);
    }

    @Operation(summary = "Query grouping nodes")
    @Parameter(name = "query", description = "What to search for", required = true)
    @Parameter(name = "parent", description = "Group number to which it belongs", required = true)
    @ResponseBody
    @GetMapping("/tree/list")
    public List<GroupTree> queryForTree(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer parent,
            @RequestParam(required = false) Boolean hasChannel
    ){
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        return groupService.queryForTree(query, parent, hasChannel);
    }

    @Operation(summary = "Query grouping")
    @Parameter(name = "query", description = "What to search for", required = true)
    @Parameter(name = "channel", description = "trueis the query channel, false is the query node", required = true)
    @ResponseBody
    @GetMapping("/tree/query")
    public PageInfo<Group> queryTree(Integer page, Integer count,
                                      @RequestParam(required = true) String query
    ){
        return groupService.queryList(page, count, query);
    }

    @Operation(summary = "Update group")
    @Parameter(name = "group", description = "Group", required = true)
    @ResponseBody
    @PostMapping("/update")
    public void update(@RequestBody Group group){
        groupService.update(group);
    }

    @Operation(summary = "Delete group")
    @Parameter(name = "id", description = "Groupid", required = true)
    @ResponseBody
    @DeleteMapping("/delete")
    public void delete(Integer id){
        Assert.notNull(id, "The group id (deviceId) does not need to exist");
        boolean result = groupService.delete(id);
        if (!result) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Removal failed");
        }
    }

    @Operation(summary = "Get the administrative division under the administrative division to which it belongs")
    @Parameter(name = "deviceId", description = "Current administrative divisions", required = false)
    @ResponseBody
    @GetMapping("/path")
    public List<Group> getPath(String deviceId, String businessGroup){
        return groupService.getPath(deviceId, businessGroup);
    }

//    @Operation(summary = "Query groups based on group ID")
//    @Parameter(name = "groupDeviceId", description = "Group node number", required = true)
//    @ResponseBody
//    @GetMapping("/one")
//    public Group queryGroupByDeviceId(
//            @RequestParam(required = true) String deviceId
//    ){
//        Assert.hasLength(deviceId, "");
//        return groupService.queryGroupByDeviceId(deviceId);
//    }

//    @Operation(summary = "Synchronize packets from channels")
//    @ResponseBody
//    @GetMapping("/sync")
//    public void sync(){
//        groupService.syncFromChannel();
//    }
}
