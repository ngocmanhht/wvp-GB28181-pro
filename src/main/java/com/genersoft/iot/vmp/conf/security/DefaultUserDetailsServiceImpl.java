package com.genersoft.iot.vmp.conf.security;

import com.alibaba.excel.util.StringUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * User login authentication logic
 */
@Slf4j
@Component
public class DefaultUserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private IUserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (StringUtils.isBlank(username)) {
            log.info("Login user：{} does not exist", username);
            throw new UsernameNotFoundException("Login user：" + username + " does not exist");
        }

        // find out password
        User user = userService.getUserByUsername(username);
        if (user == null) {
            log.info("Login user：{} does not exist", username);
            throw new UsernameNotFoundException("Login user：" + username + " does not exist");
        }
        String password = SecurityUtils.encryptPassword(user.getPassword());
        user.setPassword(password);
        return new LoginUser(user, LocalDateTime.now());
    }






}
