package com.genersoft.iot.vmp.vmanager.user;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.service.IRoleService;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.security.sasl.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name  = "User management")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IUserService userService;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private UserSetting userSetting;

    @GetMapping("/login")
    @PostMapping("/login")
    @Operation(summary = "Login", description = "AccessToken is returned after successful login, which can be obtained from the return value or from the response header.，" +
            "Subsequent requests need to add request headers 'access-token'Or put it in the parameters")
    @Parameter(name = "username", description = "Username", required = true)
    @Parameter(name = "password", description = "Password (32-bit md5 encryption）", required = true)
    public LoginUser login(HttpServletRequest request, HttpServletResponse response, @RequestParam String username, @RequestParam String password){
        LoginUser user;
        try {
            user = SecurityUtils.login(username, password, authenticationManager);
        } catch (AuthenticationException e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
        }
        if (user == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "Wrong username or password");
        }else {
            String jwt = JwtUtils.createToken(username);
            response.setHeader(JwtUtils.getHeader(), jwt);
            user.setAccessToken(jwt);
            user.setServerId(userSetting.getServerId());
        }
        return user;
    }


    @PostMapping("/changePassword")
    @Operation(summary = "Change password", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "username", description = "Username", required = true)
    @Parameter(name = "oldpassword", description = "Old password (md5 encrypted password）", required = true)
    @Parameter(name = "password", description = "New password (unmd5 encrypted password)）", required = true)
    public void changePassword(@RequestParam String oldPassword, @RequestParam String password){
        // Get the currently logged in userid
        LoginUser userInfo = SecurityUtils.getUserInfo();
        if (userInfo== null) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
        String username = userInfo.getUsername();
        LoginUser user = null;
        try {
            user = SecurityUtils.login(username, oldPassword, authenticationManager);
            if (user == null) {
                throw new ControllerException(ErrorCode.ERROR100);
            }
            //int userId = SecurityUtils.getUserId();
            boolean result = userService.changePassword(user.getId(), DigestUtils.md5DigestAsHex(password.getBytes()));
            if (!result) {
                throw new ControllerException(ErrorCode.ERROR100);
            }
        } catch (AuthenticationException e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
        }
    }


    @PostMapping("/add")
    @Operation(summary = "Add user", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "username", description = "Username", required = true)
    @Parameter(name = "password", description = "Password (unmd5 encrypted password)）", required = true)
    @Parameter(name = "roleId", description = "roleID", required = true)
    public void add(@RequestParam String username,
                                                 @RequestParam String password,
                                                 @RequestParam Integer roleId){
        if (ObjectUtils.isEmpty(username) || ObjectUtils.isEmpty(password) || roleId == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "Parameters cannot be empty");
        }
        // Get the currently logged in userid
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // Users can only be deleted and added with a role ID of 1
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "User has no permission");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        //The generation rules for pushKey for new users are:md5(Timestamp+Username)
        user.setPushKey(DigestUtils.md5DigestAsHex((System.currentTimeMillis()+password).getBytes()));
        Role role = roleService.getRoleById(roleId);

        if (role == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "role does not exist");
        }
        user.setRole(role);
        user.setCreateTime(DateUtil.getNow());
        user.setUpdateTime(DateUtil.getNow());
        int addResult = userService.addUser(user);
        if (addResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete user", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "UserId", required = true)
    public void delete(@RequestParam Integer id){
        // Get the currently logged in userid
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // Users can only be deleted and added with a role ID of 0
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "User has no permission");
        }
        int deleteResult = userService.deleteUser(id);
        if (deleteResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @GetMapping("/all")
    @Operation(summary = "Query all users", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<User> all(){
        return userService.getAllUsers();
    }

    /**
     * Query users by page
     *
     * @param page  Current page
     * @param count Number of queries per page
     * @return Paginated user list
     */
    @GetMapping("/users")
    @Operation(summary = "Query users by page", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    public PageInfo<User> users(int page, int count) {
        return userService.getUsers(page, count);
    }

    @RequestMapping("/changePushKey")
    @Operation(summary = "Modifypushkey", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "userId", description = "UserId", required = true)
    @Parameter(name = "pushKey", description = "newpushKey", required = true)
    public void changePushKey(@RequestParam Integer userId,@RequestParam String pushKey) {
        // Get the currently logged in userid
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        WVPResult<String> result = new WVPResult<>();
        if (currenRoleId != 1) {
            // Users can only be deleted and added with a role ID of 0
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "User has no permission");
        }
        int resetPushKeyResult = userService.changePushKey(userId,pushKey);
        if (resetPushKeyResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @PostMapping("/changePasswordForAdmin")
    @Operation(summary = "Administrator changes ordinary user password", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "adminId", description = "Administratorid", required = true)
    @Parameter(name = "userId", description = "Userid", required = true)
    @Parameter(name = "password", description = "New password (unmd5 encrypted password)）", required = true)
    public void changePasswordForAdmin(@RequestParam int userId, @RequestParam String password) {
        // Get the currently logged in userid
        LoginUser userInfo = SecurityUtils.getUserInfo();
        if (userInfo == null) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
        Role role = userInfo.getRole();
        if (role != null && role.getId() == 1) {
            boolean result = userService.changePassword(userId, DigestUtils.md5DigestAsHex(password.getBytes()));
            if (!result) {
                throw new ControllerException(ErrorCode.ERROR100);
            }
        }
    }

    @PostMapping("/userInfo")
    @Operation(summary = "Query the currently logged in user information")
    public LoginUser getUserInfo() {
        // Get the currently logged in userid
        LoginUser userInfo = SecurityUtils.getUserInfo();

        if (userInfo == null) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
        User user = userService.getUser(userInfo.getUsername(), userInfo.getPassword());
        return new LoginUser(user, LocalDateTime.now());
    }
}
