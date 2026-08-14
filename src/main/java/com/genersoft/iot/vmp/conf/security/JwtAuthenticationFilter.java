package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.dto.JwtUser;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.ArrayList;

/**
 * jwt token filter
 */

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final static String WSHeader = "sec-websocket-protocol";


    @Autowired
    private UserSetting userSetting;


    @Override
    protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(servletRequest);
        // Ignore token verification for login requests
        String requestURI = request.getRequestURI();
        if ((requestURI.startsWith("/doc.html") || requestURI.startsWith("/swagger-ui")  ) && !userSetting.getDocEnable()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (requestURI.equalsIgnoreCase("/api/user/login")) {
            chain.doFilter(request, response);
            return;
        }

        if (!userSetting.getInterfaceAuthentication()) {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(null, null, new ArrayList<>() );
            SecurityContextHolder.getContext().setAuthentication(token);
            chain.doFilter(request, response);
            return;
        }

        String jwt = request.getHeader(JwtUtils.getHeader());
        // If there is no jwt here, continue to go back, because there is an authentication manager and so on to determine whether you have the identity certificate, so it can be released.
        // No jwt is equivalent to anonymous access. If there are some interfaces that require permissions, these interfaces cannot be accessed.

        // websocket Authentication information is stored here by default
        String secWebsocketProtocolHeader = request.getHeader(WSHeader);
        if (StringUtils.isBlank(jwt)) {

            if (secWebsocketProtocolHeader != null) {
                jwt = secWebsocketProtocolHeader;
                response.setHeader(WSHeader, secWebsocketProtocolHeader);
            }else {
                jwt = request.getParameter(JwtUtils.getHeader());
            }
            if (StringUtils.isBlank(jwt)) {
                jwt = request.getHeader(JwtUtils.getApiKeyHeader());
                if (StringUtils.isBlank(jwt)) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        JwtUser jwtUser = JwtUtils.verifyToken(jwt);
        String username = jwtUser.getUserName();
        // TODO Handle various states
        switch (jwtUser.getStatus()){
            case EXPIRED:
                response.setStatus(401);
                chain.doFilter(request, response);
                // Abnormal
                return;
            case EXCEPTION:
                // Expired
                response.setStatus(400);
                chain.doFilter(request, response);
                return;
            case EXPIRING_SOON:
                // Expires soon
//                return;
            default:
        }
        // buildUsernamePasswordAuthenticationToken,The password here is null because the correct JWT is provided to achieve automatic login.
        User user = new User();
        user.setId(jwtUser.getUserId());
        user.setUsername(jwtUser.getUserName());
        user.setPassword(jwtUser.getPassword());
        Role role = new Role();
        role.setId(jwtUser.getRoleId());
        user.setRole(role);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user, jwtUser.getPassword(), new ArrayList<>() );
        SecurityContextHolder.getContext().setAuthentication(token);
        chain.doFilter(request, response);
    }
}
