package com.genersoft.iot.vmp.conf.security.dto;

import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;

public class LoginUser implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    /**
     * User
     */
    private User user;

    @Getter
    @Setter
    private String accessToken;

    @Setter
    @Getter
    private String serverId;

    /**
     * Login time
     */
    private LocalDateTime loginTime;

    public LoginUser(User user, LocalDateTime loginTime) {
        this.user = user;
        this.loginTime = loginTime;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * Whether the account has not expired or not, it cannot be verified if it has expired.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Specifies whether the user is unlocked. Locked users cannot authenticate.
     * <p>
     * Password lock
     * </p>
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials have expired(Password)，Expired credentials prevent authentication
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Whether the user is enabled or disabled. Disabled users cannot authenticate。
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * After authentication is complete, erase the password
     */
    @Override
    public void eraseCredentials() {
        user.setPassword(null);
    }


    public int getId() {
        return user.getId();
    }

    public Role getRole() {
        return user.getRole();
    }

    public String getPushKey() {
        return user.getPushKey();
    }

}
