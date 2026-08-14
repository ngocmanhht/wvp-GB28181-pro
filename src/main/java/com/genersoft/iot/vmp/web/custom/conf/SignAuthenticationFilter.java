package com.genersoft.iot.vmp.web.custom.conf;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sip.message.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * sign token filter
 */

@Slf4j
@Component
@ConditionalOnProperty(value = "sy.enable", havingValue = "true")
public class SignAuthenticationFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // Ignore token verification for login requests
        String requestURI = servletRequest.getRequestURI();
        // Wrap the original request and cache the request body
        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(servletRequest);
        if (!requestURI.startsWith("/api/sy")) {
            chain.doFilter(request, response);
            return;
        }
//        if (request.getParameter("ccerty") != null) {
//            chain.doFilter(request, response);
//            return;
//        }
        // Set response content type
        response.setContentType("application/json;charset=UTF-8");

        try {
            String sign = request.getParameter("sign");
            String appKey = request.getParameter("appKey");
            String accessToken = request.getParameter("accessToken");
            String timestampStr = request.getParameter("timestamp");

            if (sign == null || appKey == null || accessToken == null || timestampStr == null) {
                log.info("[SY-Interface signature verification] Missing key parameters：sign/appKey/accessToken/timestamp, Request address: {} ", requestURI);
                response.setStatus(Response.OK);
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(1, "Illegal parameter"));
                out.close();
                return;
            }
            
            // Add null check
            if (SyTokenManager.INSTANCE.appMap == null || SyTokenManager.INSTANCE.appMap.get(appKey) == null) {
                log.info("[SY-Interface signature verification] appKey {} The corresponding secret does not exist, request address: {} ", appKey, requestURI);
                response.setStatus(Response.OK);
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(1, "Illegal parameter"));
                out.close();
                return;
            }

            Map<String, String[]> parameterMap = request.getParameterMap();
            // Parameter sorting
            Set<String> paramKeys = new TreeSet<>(parameterMap.keySet());

            // Splicing signature information
            // Parameter splicing
            StringBuilder beforeSign = new StringBuilder();
            for (String paramKey : paramKeys) {
                if (paramKey.equals("sign")) {
                    continue;
                }
                // Add array length check
                String[] values = parameterMap.get(paramKey);
                if (values != null && values.length > 0) {
                    beforeSign.append(paramKey).append(values[0]);
                }
            }
            // If it is a json message in a post request, concatenate the body string
            if (request.getContentLength() > 0
                    && request.getMethod().equalsIgnoreCase("POST")
                    && request.getContentType() != null 
                    && request.getContentType().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
                // Read body content - Use custom caching mechanism
                String requestBody = request.getCachedBody();
                if (!ObjectUtils.isEmpty(requestBody)) {
                    beforeSign.append(requestBody);
                    log.debug("[SY-Interface signature verification] Read the request body content, length: {}", requestBody.length());
                } else {
                    log.warn("[SY-Interface signature verification] The request body content is empty");
                }
            }
            
            // Add null check
            String secret = SyTokenManager.INSTANCE.appMap.get(appKey);
            if (secret == null) {
                log.info("[SY-Interface signature verification] Unable to obtainappKey {} Corresponding secret, request address: {} ", appKey, requestURI);
                response.setStatus(Response.OK);
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(1, "Illegal parameter"));
                out.close();
                return;
            }
            
            beforeSign.append(secret);
            // Generate signature
            String buildSign = SmUtil.sm3(beforeSign.toString());
            if (!buildSign.equals(sign)) {
                log.info("[SY-Interface signature verification] Failed, content before encryption： {}, Request address: {} ", beforeSign, requestURI);
                response.setStatus(Response.OK);
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(2, "Signature error"));
                out.close();
                return;
            }
            // Verify request timestamp
            long timestamp = Long.parseLong(timestampStr);
            long currentTimeMillis = System.currentTimeMillis();
            // Add null check
            if (SyTokenManager.INSTANCE.expires == null) {
                log.info("[SY-Interface signature verification] expiresConfiguration is empty, request address: {} ", requestURI);
                response.setStatus(Response.OK);
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(2, "Signature error"));
                out.close();
                return;
            }
            if (currentTimeMillis > SyTokenManager.INSTANCE.expires * 60 * 1000 + timestamp ) {
                log.info("[SY-Interface signature verification] Timestamp has expired, request timestamp：{}， current time： {}, Expiration time： {}, Request address: {} ", timestamp, currentTimeMillis, timestamp + SyTokenManager.INSTANCE.expires * 60 * 1000, requestURI);
                response.setStatus(Response.OK);
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(3, "The interface has expired"));
                out.close();
                return;
            }
            // accessTokenVerification
            // Add null check
            if (SyTokenManager.INSTANCE.adminToken == null) {
                log.info("[SY-Interface signature verification] adminTokenConfiguration is empty, request address: {} ", requestURI);
                response.setStatus(Response.OK);
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(2, "Signature error"));
                out.close();
                return;
            }
            if (accessToken.equals(SyTokenManager.INSTANCE.adminToken)) {
                log.info("[SY-Interface signature verification] adminTokenAlready released by default, request address: {} ", requestURI);
                chain.doFilter(request, response);
                return;
            }else {
                // Add null check
                if (SyTokenManager.INSTANCE.sm4Key == null) {
                    log.info("[SY-Interface signature verification] sm4KeyConfiguration is empty, request address: {} ", requestURI);
                    response.setStatus(Response.OK);
                    PrintWriter out = response.getWriter();
                    out.println(getErrorResult(2, "Signature error"));
                    out.close();
                    return;
                }
                // Decrypt the token
                SM4 sm4 = SmUtil.sm4(HexUtil.decodeHex(SyTokenManager.INSTANCE.sm4Key));
                String decryptStr = sm4.decryptStr(accessToken, CharsetUtil.CHARSET_UTF_8);
                if (decryptStr == null) {
                    log.info("[SY-Interface signature verification] accessTokenDecryption failed, request address: {} ", requestURI);
                    response.setStatus(Response.OK);
                    PrintWriter out = response.getWriter();
                    out.println(getErrorResult(2, "Signature error"));
                    out.close();
                    return;
                }
                JSONObject jsonObject = JSON.parseObject(decryptStr);
                Long expirationTime = jsonObject.getLong("expirationTime");
                if (expirationTime == null || expirationTime < System.currentTimeMillis()) {
                    log.info("[SY-Interface signature verification] accessToken Expired, request address: {} ", requestURI);
                    response.setStatus(Response.OK);
                    PrintWriter out = response.getWriter();
                    out.println(getErrorResult(4, "tokenExpired or wrong"));
                    out.close();
                    return;
                }
            }
        }catch (NumberFormatException e) {
            log.info("[SY-Interface signature verification] Timestamp format error, request address: {} ", requestURI);
            response.setStatus(Response.OK);
            if (!response.isCommitted()) {
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(2, "Signature error"));
                out.close();
            }
            return;
        }catch (Exception e) {
            log.info("[SY-Interface signature verification] Failed to read body, request address: {} ", requestURI, e);
            response.setStatus(Response.OK);
            if (!response.isCommitted()) {
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(2, "Signature error"));
                out.close();
            }
            return;
        }
        chain.doFilter(request, response);
    }

    private String getErrorResult(Integer code, String message) {
        WVPResult<Object> wvpResult = new WVPResult<>();
        wvpResult.setCode(code);
        wvpResult.setMsg(message);
        return JSON.toJSONString(wvpResult);
    }

}