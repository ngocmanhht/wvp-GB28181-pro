package com.genersoft.iot.vmp.conf.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User login credentials payload")
public class LoginDTO {

    @Schema(description = "Username", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    private String username;

    @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    private String password;
}
