package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.UserSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ConfigurationSpring Security
 *
 * @author lin
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(1)
@Slf4j
public class WebSecurityConfig {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DefaultUserDetailsServiceImpl userDetailsService;
    /**
     * Successful logout processing
     */
    @Autowired
    private LogoutHandler logoutHandler;
    /**
     * Not logged in processing
     */
    @Autowired
    private AnonymousAuthenticationEntryPoint anonymousAuthenticationEntryPoint;
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // Set not to hide user exception not found
        provider.setHideUserNotFoundExceptions(true);
        // User authenticationservice - Query database logic
        provider.setUserDetailsService(userDetailsService);
        // Set password encryption algorithm
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<String> defaultExcludes = new ArrayList<>();
        defaultExcludes.add("/");
        defaultExcludes.add("/#/**");
        defaultExcludes.add("/static/**");

        defaultExcludes.add("/swagger-ui.html");
        defaultExcludes.add("/swagger-ui/**");
        defaultExcludes.add("/swagger-resources/**");
        defaultExcludes.add("/doc.html");
        defaultExcludes.add("/doc.html#/**");
        defaultExcludes.add("/v3/api-docs/**");

        defaultExcludes.add("/index.html");
        defaultExcludes.add("/webjars/**");

        defaultExcludes.add("/js/**");
        defaultExcludes.add("/api/device/query/snap/**");
        defaultExcludes.add("/api/alarm/snap/**");
        defaultExcludes.add("/record_proxy/*/**");
        defaultExcludes.add("/api/emit");
        defaultExcludes.add("/favicon.ico");
        defaultExcludes.add("/api/user/login");
        defaultExcludes.add("/index/hook/**");
        defaultExcludes.add("/api/device/query/snap/**");
        defaultExcludes.add("/index/hook/abl/**");
        defaultExcludes.add("/api/jt1078/playback/download");
        defaultExcludes.add("/api/jt1078/snap");

        if (userSetting.getInterfaceAuthentication() && !userSetting.getInterfaceAuthenticationExcludes().isEmpty()) {
            defaultExcludes.addAll(userSetting.getInterfaceAuthenticationExcludes());
        }

        http
                .headers(headers -> headers.contentTypeOptions(contentType -> contentType.disable()))
                .cors(cors -> cors.configurationSource(configurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Configure interception rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers(defaultExcludes.toArray(new String[0])).permitAll()
                        .anyRequest().authenticated()
                )
                // exception handler
                .exceptionHandling(exception -> exception.authenticationEntryPoint(anonymousAuthenticationEntryPoint))
                .logout(logout -> logout.logoutUrl("/api/user/logout")
                        .permitAll()
                        .logoutSuccessHandler(logoutHandler));

        return http.build();
    }

    CorsConfigurationSource configurationSource() {
        // Configure cross-domain
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
        corsConfiguration.setAllowedMethods(Arrays.asList("*"));
        corsConfiguration.setMaxAge(3600L);
        if (userSetting.getAllowedOrigins() != null && !userSetting.getAllowedOrigins().isEmpty()) {
            corsConfiguration.setAllowCredentials(true);
            corsConfiguration.setAllowedOrigins(userSetting.getAllowedOrigins());
        } else {
            // When handling cross-domain in SpringBoot 2.4 and above, an error message is encountered: WhenallowCredentialsWhen true, allowedOrigins cannot contain special values"*"。
            // The workaround is to specify allowedOrigins explicitly or useallowedOriginPatterns。
            corsConfiguration.setAllowCredentials(true);
            corsConfiguration.addAllowedOriginPattern(CorsConfiguration.ALL); // Default all allows all cross-domain
        }

        corsConfiguration.setExposedHeaders(Arrays.asList(JwtUtils.getHeader()));

        UrlBasedCorsConfigurationSource url = new UrlBasedCorsConfigurationSource();
        url.registerCorsConfiguration("/**", corsConfiguration);
        return url;
    }

    /**
     * Description: Password encryption algorithm BCrypt recommended
     **/
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}