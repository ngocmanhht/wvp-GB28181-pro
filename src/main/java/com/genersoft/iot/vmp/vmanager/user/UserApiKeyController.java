package com.genersoft.iot.vmp.vmanager.user;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.service.IUserApiKeyService;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.storager.dao.dto.UserApiKey;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "User ApiKey management")
@RestController
@RequestMapping("/api/userApiKey")
public class UserApiKeyController {

    public static final int EXPIRATION_TIME = Integer.MAX_VALUE;
    @Autowired
    private IUserService userService;

    @Autowired
    private IUserApiKeyService userApiKeyService;

    /**
     * Add userApiKey
     *
     * @param userId
     * @param app
     * @param remark
     * @param expiresAt
     * @param enable
     */
    @PostMapping("/add")
    @Operation(summary = "Add userApiKey", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "userId", description = "UserId", required = true)
    @Parameter(name = "app", description = "Application name", required = false)
    @Parameter(name = "remark", description = "Remarks", required = false)
    @Parameter(name = "expiredAt", description = "Expiration time (not passed means it will never expire）", required = false)
    @Transactional
    public synchronized void add(
            @RequestParam(required = true) int userId,
            @RequestParam(required = false) String app,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String expiresAt,
            @RequestParam(required = false) Boolean enable
    ) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "User does not exist");
        }

        Long expirationTime = null;
        if (expiresAt != null) {
            expirationTime = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(expiresAt);
            long difference = (expirationTime - System.currentTimeMillis()) / (60 * 1000);
            if (difference < 0) {
                throw new ControllerException(ErrorCode.ERROR400.getCode(), "The expiration time cannot be earlier than the current time");
            }
        }

        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setUserId(userId);
        userApiKey.setApp(app);
        userApiKey.setApiKey(null);
        userApiKey.setRemark(remark);
        userApiKey.setExpiredAt(expirationTime != null ? expirationTime : 0);
        userApiKey.setEnable(enable != null ? enable : false);
        userApiKey.setCreateTime(DateUtil.getNow());
        userApiKey.setUpdateTime(DateUtil.getNow());

        int addResult = userApiKeyService.addApiKey(userApiKey);

        if (addResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }

        String apiKey;
        do {
            Map<String, Object> extra = new HashMap<>(1);
            extra.put("apiKeyId", userApiKey.getId());
            apiKey = JwtUtils.createToken(user.getUsername(), expirationTime, extra);
        } while (userApiKeyService.isApiKeyExists(apiKey));

        int resetResult = userApiKeyService.reset(userApiKey.getId(), apiKey);

        if (resetResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    /**
     * Page queryApiKey
     *
     * @param page  Current page
     * @param count Number of queries per page
     * @return Paginated ApiKey list
     */
    @GetMapping("/userApiKeys")
    @Operation(summary = "Query users by pageApiKey", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "Current page", required = true)
    @Parameter(name = "count", description = "Number of queries per page", required = true)
    @Transactional
    public PageInfo<UserApiKey> userApiKeys(@RequestParam(required = true) int page, @RequestParam(required = true) int count, @RequestParam(required = false) Integer userId) {
        return userApiKeyService.getUserApiKeys(page, count, userId);
    }

    @PostMapping("/enable")
    @Operation(summary = "enable userApiKey", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "UserApiKeyId", required = true)
    @Transactional
    public void enable(@RequestParam(required = true) Integer id) {
        // Get the currently logged in userid
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // It can only be managed if the role ID is 1UserApiKey
            throw new ControllerException(ErrorCode.ERROR403);
        }
        UserApiKey userApiKey = userApiKeyService.getUserApiKeyById(id);
        if (userApiKey == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "ApiKeydoes not exist");
        }

        int enableResult = userApiKeyService.enable(id);

        if (enableResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @PostMapping("/disable")
    @Operation(summary = "Deactivate userApiKey", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "UserApiKeyId", required = true)
    @Transactional
    public void disable(@RequestParam(required = true) Integer id) {
        // Get the currently logged in userid
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // It can only be managed if the role ID is 1UserApiKey
            throw new ControllerException(ErrorCode.ERROR403);
        }
        UserApiKey userApiKey = userApiKeyService.getUserApiKeyById(id);
        if (userApiKey == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "ApiKeydoes not exist");
        }

        int disableResult = userApiKeyService.disable(id);

        if (disableResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @PostMapping("/reset")
    @Operation(summary = "reset userApiKey", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "UserApiKeyId", required = true)
    @Transactional
    public void reset(@RequestParam(required = true) Integer id) {
        // Get the currently logged in userid
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // It can only be managed if the role ID is 1UserApiKey
            throw new ControllerException(ErrorCode.ERROR403);
        }
        UserApiKey userApiKey = userApiKeyService.getUserApiKeyById(id);
        if (userApiKey == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "ApiKeydoes not exist");
        }
        User user = userService.getUserById(userApiKey.getUserId());
        if (user == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "User does not exist");
        }
        Long expirationTime = null;
        if (userApiKey.getExpiredAt() > 0) {
            long timestamp = userApiKey.getExpiredAt();
            expirationTime = (timestamp - System.currentTimeMillis()) / (60 * 1000);
            if (expirationTime < 0) {
                throw new ControllerException(ErrorCode.ERROR400.getCode(), "ApiKeyExpired");
            }
        }
        String apiKey;
        do {
            Map<String, Object> extra = new HashMap<>(1);
            extra.put("apiKeyId", userApiKey.getId());
            apiKey = JwtUtils.createToken(user.getUsername(), expirationTime, extra);
        } while (userApiKeyService.isApiKeyExists(apiKey));

        int resetResult = userApiKeyService.reset(id, apiKey);

        if (resetResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @PostMapping("/remark")
    @Operation(summary = "Note userApiKey", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "UserApiKeyId", required = true)
    @Parameter(name = "remark", description = "User ApiKey remarks", required = false)
    @Transactional
    public void remark(@RequestParam(required = true) Integer id, @RequestParam(required = false) String remark) {
        // Get the currently logged in userid
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // It can only be managed if the role ID is 1UserApiKey
            throw new ControllerException(ErrorCode.ERROR403);
        }
        UserApiKey userApiKey = userApiKeyService.getUserApiKeyById(id);
        if (userApiKey == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "ApiKeydoes not exist");
        }
        int remarkResult = userApiKeyService.remark(id, remark);

        if (remarkResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete userApiKey", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "UserApiKeyId", required = true)
    @Transactional
    public void delete(@RequestParam(required = true) Integer id) {
        // Get the currently logged in userid
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // It can only be managed if the role ID is 1UserApiKey
            throw new ControllerException(ErrorCode.ERROR403);
        }
        UserApiKey userApiKey = userApiKeyService.getUserApiKeyById(id);
        if (userApiKey == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "ApiKeydoes not exist");
        }

        int deleteResult = userApiKeyService.delete(id);

        if (deleteResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }
}
