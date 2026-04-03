package net.hwyz.iov.cloud.sec.ciam.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 长文本字段序列化/反序列化工具。
 * <p>
 * 为 CIAM 系统中各类长文本字段提供统一的序列化格式：
 * <ul>
 *   <li>{@code request_snapshot} — JSON 对象（脱敏后的请求上下文快照）</li>
 *   <li>{@code hit_rules} — 逗号分隔字符串，如 {@code "rule_new_device,rule_geo_change"}</li>
 *   <li>{@code redirect_uris} — JSON 数组，如 {@code ["https://a.com/cb","https://b.com/cb"]}</li>
 *   <li>{@code scopes} — 空格分隔字符串，如 {@code "openid profile email"}</li>
 *   <li>{@code grant_types} — 逗号分隔字符串，如 {@code "authorization_code,refresh_token"}</li>
 * </ul>
 */
public final class LongTextSerializer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LongTextSerializer() {
    }

    // ========== JSON 对象（审计快照） ==========

    /**
     * 将 Map 序列化为 JSON 字符串，用于 {@code request_snapshot} 列。
     */
    public static String toJson(Map<String, Object> map) {
        Objects.requireNonNull(map, "map must not be null");
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize map to JSON", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Map。
     */
    public static Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON to map", e);
        }
    }

    // ========== JSON 数组（回调地址） ==========

    /**
     * 将字符串列表序列化为 JSON 数组，用于 {@code redirect_uris} 列。
     */
    public static String toJsonArray(List<String> list) {
        Objects.requireNonNull(list, "list must not be null");
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize list to JSON array", e);
        }
    }

    /**
     * 将 JSON 数组字符串反序列化为字符串列表。
     */
    public static List<String> fromJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON array to list", e);
        }
    }

    // ========== 逗号分隔（命中规则、授权类型） ==========

    /**
     * 将字符串列表序列化为逗号分隔字符串，用于 {@code hit_rules}、{@code grant_types} 列。
     */
    public static String toCommaSeparated(List<String> list) {
        Objects.requireNonNull(list, "list must not be null");
        return list.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(","));
    }

    /**
     * 将逗号分隔字符串反序列化为字符串列表。
     */
    public static List<String> fromCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Stream.of(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // ========== 空格分隔（OAuth scopes） ==========

    /**
     * 将字符串列表序列化为空格分隔字符串，用于 {@code scopes} 列。
     */
    public static String toSpaceSeparated(List<String> list) {
        Objects.requireNonNull(list, "list must not be null");
        return list.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
    }

    /**
     * 将空格分隔字符串反序列化为字符串列表。
     */
    public static List<String> fromSpaceSeparated(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Stream.of(value.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
