package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.security.sasl.AuthenticationException;
import java.time.LocalDateTime;

public class SecurityUtils {

    /**
     * Description: Call security for authentication and authorization based on account password. Active adjustment
     * useAuthenticationManagerThe authenticate method implementation
     * After successful authorization, the user information is stored in the SecurityContext.
     * @param username Username
     * @param password Password
     * @param authenticationManager Authentication Authorization Manager,
     * @see  AuthenticationManager
     * @return UserInfo  User information
     */
    public static LoginUser login(String username, String password, AuthenticationManager authenticationManager) throws AuthenticationException {
        //Use the verification token generator that comes with the security framework or you can customize it。
        UsernamePasswordAuthenticationToken token =new UsernamePasswordAuthenticationToken(username,password);
        //If the authentication fails, it will automatically return after an exception, so there is no need to judge whether the return value is empty to determine whether the login is successful.
        Authentication authenticate = authenticationManager.authenticate(token);
        LoginUser user = (LoginUser) authenticate.getPrincipal();

        SecurityContextHolder.getContext().setAuthentication(token);

        return user;
    }

    /**
     * Get all authentication information for the current login
     * @return
     */
    public static Authentication getAuthentication(){
        SecurityContext context = SecurityContextHolder.getContext();
        return context.getAuthentication();
    }

    /**
     * Get current logged in user information
     * @return
     */
    public static LoginUser getUserInfo(){
        Authentication authentication = getAuthentication();
        if(authentication!=null){
            Object principal = authentication.getPrincipal();
            if(principal!=null && !"anonymousUser".equals(principal.toString())){

                User user = (User) principal;
                return new LoginUser(user, LocalDateTime.now());
            }
        }
        return null;
    }

    /**
     * Get the currently logged in userID
     * @return
     */
    public static int getUserId(){
        LoginUser user = getUserInfo();
        return user.getId();
    }

    /**
     * generateBCryptPasswordEncoderPassword
     *
     * @param password Password
     * @return encrypted string
     */
    public static String encryptPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }
}
