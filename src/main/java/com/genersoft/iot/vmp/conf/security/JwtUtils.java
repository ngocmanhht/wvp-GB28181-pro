package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.dto.JwtUser;
import com.genersoft.iot.vmp.service.IUserApiKeyService;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.storager.dao.dto.UserApiKey;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtils implements InitializingBean {

    public static final String HEADER = "access-token";

    public static final String API_KEY_HEADER = "api-key";

    private static final String AUDIENCE = "Audience";

    private static final String keyId = "3e79646c4dbc408383a9eed09f2b85ae";

    /**
     * tokenExpiration time(minutes)
     */
    public static final long EXPIRATION_TIME = 30;

    private static RsaJsonWebKey rsaJsonWebKey;

    private static IUserService userService;

    private static IUserApiKeyService userApiKeyService;

    private static UserSetting userSetting;

    public static String getApiKeyHeader() {
        return API_KEY_HEADER;
    }

    @Resource
    public void setUserService(IUserService userService) {
        JwtUtils.userService = userService;
    }

    @Resource
    public void setUserApiKeyService(IUserApiKeyService userApiKeyService) {
        JwtUtils.userApiKeyService = userApiKeyService;
    }

    @Resource
    public void setUserSetting(UserSetting userSetting) {
        JwtUtils.userSetting = userSetting;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            rsaJsonWebKey = generateRsaJsonWebKey();
        } catch (JoseException e) {
            log.error("Generate RsaJsonWebKey error report。", e);
        }
    }

    /**
     * Create key pair (fix allbug+classpathwarning+Key persistence）
     */
    private RsaJsonWebKey generateRsaJsonWebKey() throws JoseException {
        // Pre-validation: avoid null pointers (to prevent userSetting from being uninitialized or jwkFile not being configured)）
        if (userSetting == null) {
            log.error("[API AUTH] userSetting not initialized！");
            return createDefaultRsaKey();
        }
        String jwkFile = userSetting.getJwkFile();
        if (jwkFile == null || jwkFile.trim().isEmpty()) {
            log.warn("[API AUTH] JWKFile path is not configured! Use default configuration path：./config/jwk.json");
            jwkFile = "config" + File.separator + "jwk.json"; // Default external path
        }

        // Try to read JWK file (handled automaticallyclasspath/local files, usetry-with-resourcesAutomatically shut off the flow, no leakage）
        try (InputStream inputStream = getJwkInputStream(jwkFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            // Read JSON (without skipping any lines, fix the originalbug）
            String jwkJson = reader.lines().collect(Collectors.joining());
            JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(jwkJson);
            List<JsonWebKey> jsonWebKeys = jsonWebKeySet.getJsonWebKeys();

            // Filter: Take the first valid RSA private key (the private key is required for signature to avoid subsequent errors)）
            for (JsonWebKey jsonWebKey : jsonWebKeys) {
                if (jsonWebKey instanceof RsaJsonWebKey) {
                    RsaJsonWebKey rsaKey = (RsaJsonWebKey) jsonWebKey;
                    // Verify whether the private key is included
                    if (rsaKey.getPrivateKey() != null) {
                        log.info("[API AUTH] Reading RSA key from JWK file successfully，keyId: {}", rsaKey.getKeyId());
                        return rsaKey;
                    }
                }
            }
            log.error("[API AUTH] JWKThere is no valid RSA private key in the file (only the public key cannot signJWT）");

        } catch (IOException e) {
            log.error("[API AUTH] Failed to read JWK file (path：{}）", jwkFile);
        } catch (Exception e) {
            log.error("[API AUTH] Failed to parse JWK file (JSON format error or invalid key）", e);
        }

        // All failure scenarios: generate default keys and persist them (to avoid restart failure）
        return createAndPersistDefaultRsaKey(jwkFile);
    }

    /**
     * Get the JWK file input stream (supportsclasspath/Local files, classpath reading plus security warning）
     */
    private InputStream getJwkInputStream(String jwkFile) throws IOException {
        if (jwkFile.startsWith("classpath:")) {
            String filePath = jwkFile.substring("classpath:".length());
            ClassPathResource resource = new ClassPathResource(filePath);
            if (resource.exists()) {
                // Key: Print a security warning when classpath is read to remind the user to confirm the source of the key
                log.warn("[API AUTH] Read built-in JWK files from classpath：{}！Please confirm that the key is issued by yourself，" +
                        "classpathThere is a risk of leakage of built-in keys. In production environments, it is recommended to use external file configuration instead.", filePath);
                return resource.getInputStream();
            }
            // throw new IOException("classpathThe next JWK file does not exist：" + filePath);
        }
        {
            File file = determinePersistPath(jwkFile).toFile();// In the scenario of external configuration and classpath failure
            if (file.exists() && file.canRead()) {
                log.debug("[API AUTH] Read JWK file from local file：{}", file.getAbsolutePath());
                return Files.newInputStream(file.toPath());
            }
            throw new IOException("The local JWK file does not exist or does not have read permission：" + file.getAbsolutePath());
        }
    }

        /**
     * Generate the default RSA key (extract it separately to fix the problem of missing writing before)）
     */
    private RsaJsonWebKey createDefaultRsaKey() throws JoseException {
        RsaJsonWebKey defaultKey = RsaJwkGenerator.generateJwk(4096);
        defaultKey.setKeyId(keyId);
        log.warn("[API AUTH] Use the default generated RSA key (not persisted and will become invalid upon restart)），keyId: {}", defaultKey.getKeyId());
        return defaultKey;
    }

    /**
     * Generate a default RSA key and persist it to a file (fix the original duplicate code to avoid restart failure）
     */
    private RsaJsonWebKey createAndPersistDefaultRsaKey(String configJwkFile) throws JoseException {
        // 1. Generate 4096-bit RSA key (original 2048-bit upgrade, more secure）
        RsaJsonWebKey defaultKey = RsaJwkGenerator.generateJwk(4096);
        defaultKey.setKeyId(keyId); // keyIdConfiguration

        // 2. Determine the persistence path: give priority to the non-classpath path configured by the user, otherwise use the default external path
        Path persistPath = determinePersistPath(configJwkFile);
        if (persistPath == null) {
            log.warn("[API AUTH] Generate default RSA key（keyId: {}），But the configuration path is classpath (read-only）！" +
                    "The key will become invalid after the service is restarted. Please modify jwkFile to an external writable path (such as：/opt/config/jwk.json）", defaultKey.getKeyId());
            return defaultKey;
        }

        // 3. Save the key to a file (standard JWK Set format, which can be read directly next time it is started)）
        try {
            // Automatically create parent directories (e.g../configIt will be automatically created if it does not exist.）
            Files.createDirectories(persistPath.getParent());
            // Build standardsJWK Set JSON（jose4joftoString()Comes with the correct format）
            JsonWebKeySet jwkSet = new JsonWebKeySet(defaultKey);
            String jwkJson = jwkSet.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE);
            // Write to file (overwrite existing file to avoid duplication）
            Files.writeString(persistPath, jwkJson, StandardCharsets.UTF_8);
            log.info("[API AUTH] Generate default RSA key（keyId: {}）and persist to：{}",
                    defaultKey.getKeyId(), persistPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("[API AUTH] Generating the default RSA key succeeds, but persistence fails (path：{}）！The key will become invalid after the service is restarted.",
                    persistPath.toAbsolutePath(), e);
        }

        return defaultKey;
    }

    /**
     * Determine the key persistence path (compatible with classpath read-only scenarios）
     */
    private Path determinePersistPath(String configJwkFile) {
        // If the configuration path is not classpath, directly use the path configured by the user (externally writable）
        if (!configJwkFile.startsWith("classpath:")) {
            return Paths.get(configJwkFile);
        }
        // If the configuration is classpath, save it to the default external path：./config/jwk.json（config folder in the project root directory）
        Path defaultPath = Paths.get("config", "jwk.json");
        log.warn("[API AUTH] The configured jwkFile is a classpath path (read-only) and the default key will be saved to an external path：{}",
                defaultPath.toAbsolutePath());
        return defaultPath;
    }


    public static String createToken(String username, Long expirationTime, Map<String, Object> extra) {
        try {
            /*
             * “iss” (issuer)  Issuer
             * “sub” (subject)  Topic
             * “aud” (audience) Receiver User
             * “exp” (expiration time) Expiration time
             * “nbf” (not before)  Not available before then
             * “iat” (issued at)  jwtissuance time
             */
            JwtClaims claims = new JwtClaims();
            claims.setGeneratedJwtId();
            claims.setIssuedAtToNow();
            // The token will expire in minutes
            if (expirationTime != null) {
                claims.setExpirationTimeMinutesInTheFuture(expirationTime);
            }
            claims.setNotBeforeMinutesInThePast(0);
            claims.setSubject("login");
            claims.setAudience(AUDIENCE);
            //Add custom parameters, which must be of string type
            claims.setClaim("userName", username);
            if (extra != null) {
                extra.forEach(claims::setClaim);
            }
            //jws
            JsonWebSignature jws = new JsonWebSignature();
            //Signature algorithmRS256
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
            jws.setKeyIdHeaderValue(keyId);
            jws.setPayload(claims.toJson());

            jws.setKey(rsaJsonWebKey.getPrivateKey());

            //get token
            return jws.getCompactSerialization();
        } catch (JoseException e) {
            log.error("[TokenBuild failed]： {}", e.getMessage());
        }
        return null;
    }

    public static String createToken(String username, Long expirationTime) {
        return createToken(username, expirationTime, null);
    }

    public static String createToken(String username) {
        return createToken(username, userSetting.getLoginTimeout());
    }

    public static String getHeader() {
        return HEADER;
    }

    public static JwtUser verifyToken(String token) {

        JwtUser jwtUser = new JwtUser();

        try {
            JwtConsumer consumer = new JwtConsumerBuilder()
                    //.setRequireExpirationTime()
                    //.setMaxFutureValidityInMinutes(5256000)
                    .setAllowedClockSkewInSeconds(30)
                    .setRequireSubject()
                    //.setExpectedIssuer("")
                    .setExpectedAudience(AUDIENCE)
                    .setVerificationKey(rsaJsonWebKey.getPublicKey())
                    .build();

            JwtClaims claims = consumer.processToClaims(token);
            NumericDate expirationTime = claims.getExpirationTime();
            if (expirationTime != null) {
                // Determine whether it is about to expire. The default remaining time is less than 5 minutes and it is not about to expire.
                // Remaining time (seconds）
                long timeRemaining = expirationTime.getValue() - LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8));
                if (timeRemaining < 5 * 60) {
                    jwtUser.setStatus(JwtUser.TokenStatus.EXPIRING_SOON);
                } else {
                    jwtUser.setStatus(JwtUser.TokenStatus.NORMAL);
               }
            } else {
                jwtUser.setStatus(JwtUser.TokenStatus.NORMAL);
            }

            Long apiKeyId = claims.getClaimValue("apiKeyId", Long.class);
            if (apiKeyId != null) {
                UserApiKey userApiKey = userApiKeyService.getUserApiKeyById(apiKeyId.intValue());
                if (userApiKey == null || !userApiKey.isEnable()) {
                    jwtUser.setStatus(JwtUser.TokenStatus.EXPIRED);
                }
            }

            String username = (String) claims.getClaimValue("userName");
            User user = userService.getUserByUsername(username);

            jwtUser.setUserName(username);
            jwtUser.setPassword(user.getPassword());
            jwtUser.setRoleId(user.getRole().getId());
            jwtUser.setUserId(user.getId());

            return jwtUser;
        } catch (InvalidJwtException e) {
            if (e.hasErrorCode(ErrorCodes.EXPIRED)) {
                jwtUser.setStatus(JwtUser.TokenStatus.EXPIRED);
            } else {
                jwtUser.setStatus(JwtUser.TokenStatus.EXCEPTION);
            }
            return jwtUser;
        } catch (Exception e) {
            log.error("[TokenParsing failed]： {}", e.getMessage());
            jwtUser.setStatus(JwtUser.TokenStatus.EXPIRED);
            return jwtUser;
        }
    }
}
