package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.UserIdentityPo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.UserProfilePo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OIDC 能力领域服务。
 * <p>
 * 提供 UserInfo 查询、Discovery Document 生成、ID Token 声明组装等 OIDC 标准能力。
 * {@code sub} 统一使用平台全局唯一用户 ID。
 */
@Service
@RequiredArgsConstructor
public class OidcService {

    static final String ISSUER = "https://account.openiov.com";
    static final int ID_TOKEN_TTL_SECONDS = 3600;

    private final CiamUserProfileRepository profileRepository;
    private final IdentityDomainService identityDomainService;
    private final FieldEncryptor fieldEncryptor;

    /**
     * 获取 OIDC UserInfo。
     *
     * @param userId 用户业务唯一标识
     * @return UserInfo DTO，sub 为 userId
     */
    public OidcUserInfo getUserInfo(String userId) {
        OidcUserInfo.OidcUserInfoBuilder builder = OidcUserInfo.builder().sub(userId);

        profileRepository.findByUserId(userId).ifPresent(profile -> {
            builder.name(profile.getNickname());
            builder.picture(profile.getAvatarUrl());
            builder.gender(mapGender(profile.getGender()));
            if (profile.getBirthday() != null) {
                builder.birthdate(profile.getBirthday().toString());
            }
        });

        List<UserIdentityPo> identities = identityDomainService.findByUserId(userId);
        findIdentityValue(identities, IdentityType.EMAIL).ifPresent(builder::email);
        findIdentityValue(identities, IdentityType.MOBILE).ifPresent(builder::phoneNumber);

        return builder.build();
    }

    /**
     * 获取 OIDC Discovery Document。
     *
     * @return Discovery Document DTO
     */
    public OidcDiscoveryDocument getDiscoveryDocument() {
        return new OidcDiscoveryDocument(ISSUER);
    }

    /**
     * 组装 ID Token 声明。
     *
     * @param userId   用户业务唯一标识
     * @param clientId 客户端标识
     * @param scope    授权范围（逗号分隔）
     * @return 声明 Map，包含 iss、sub、aud、iat、exp 及 scope 对应的用户声明
     */
    public Map<String, Object> getIdTokenClaims(String userId, String clientId, String scope) {
        Instant now = Instant.now();

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", ISSUER);
        claims.put("sub", userId);
        claims.put("aud", clientId);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(ID_TOKEN_TTL_SECONDS).getEpochSecond());

        Set<String> scopes = parseScopes(scope);

        if (scopes.contains("profile") || scopes.contains("email") || scopes.contains("phone")) {
            OidcUserInfo userInfo = getUserInfo(userId);

            if (scopes.contains("profile")) {
                putIfNotNull(claims, "name", userInfo.getName());
                putIfNotNull(claims, "picture", userInfo.getPicture());
                putIfNotNull(claims, "gender", userInfo.getGender());
                putIfNotNull(claims, "birthdate", userInfo.getBirthdate());
            }
            if (scopes.contains("email")) {
                putIfNotNull(claims, "email", userInfo.getEmail());
            }
            if (scopes.contains("phone")) {
                putIfNotNull(claims, "phone_number", userInfo.getPhoneNumber());
            }
        }

        return claims;
    }

    // ---- 内部方法 ----

    private Optional<String> findIdentityValue(List<UserIdentityPo> identities,
                                               IdentityType type) {
        return identities.stream()
                .filter(i -> type.getCode().equals(i.getIdentityType()))
                .findFirst()
                .map(i -> fieldEncryptor.decrypt(i.getIdentityValue()));
    }

    static String mapGender(Integer gender) {
        if (gender == null) {
            return "unknown";
        }
        switch (gender) {
            case 1: return "male";
            case 2: return "female";
            default: return "unknown";
        }
    }

    private Set<String> parseScopes(String scope) {
        if (scope == null || scope.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(scope.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
