package com.genersoft.iot.vmp.vmanager.user;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.service.IRoleService;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name  = "role management")

@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Autowired
    private IRoleService roleService;

    @PostMapping("/add")
    @Operation(summary = "Add role", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "name", description = "Character name", required = true)
    @Parameter(name = "authority", description = "Permissions (self-defined content, currently not used)）", required = true)
    public void add(@RequestParam String name,
                                                  @RequestParam(required = false) String authority){
        // Get the currently logged in userid
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // Users can only be deleted and added with a role ID of 0
            throw new ControllerException(ErrorCode.ERROR403);
        }

        Role role = new Role();
        role.setName(name);
        role.setAuthority(authority);
        role.setCreateTime(DateUtil.getNow());
        role.setUpdateTime(DateUtil.getNow());

        int addResult = roleService.add(role);
        if (addResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete role", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "UserId", required = true)
    public void delete(@RequestParam Integer id){
        // Get the currently logged in userid
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // Users can only be deleted and added with a role ID of 0
            throw new ControllerException(ErrorCode.ERROR403);
        }
        int deleteResult = roleService.delete(id);

        if (deleteResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @GetMapping("/all")
    @Operation(summary = "Query roles", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<Role> all(){
        // Get the currently logged in userid
        return roleService.getAll();
    }
}
