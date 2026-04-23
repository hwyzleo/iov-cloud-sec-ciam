package net.hwyz.iov.cloud.sec.ciam.service.common.security;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LongTextSerializerTest {

    // ========== JSON 对象（审计快照 request_snapshot） ==========

    @Test
    void toJson_map_producesValidJson() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("uri", "/api/v1/auth/login");
        snapshot.put("method", "POST");
        snapshot.put("phone", "138****1234");
        String json = LongTextSerializer.toJson(snapshot);
        assertTrue(json.contains("\"uri\":\"/api/v1/auth/login\""));
        assertTrue(json.contains("\"phone\":\"138****1234\""));
    }

    @Test
    void fromJson_validJson_returnsMap() {
        String json = "{\"uri\":\"/api/v1/auth/login\",\"result\":\"success\"}";
        Map<String, Object> map = LongTextSerializer.fromJson(json);
        assertEquals("/api/v1/auth/login", map.get("uri"));
        assertEquals("success", map.get("result"));
    }

    @Test
    void fromJson_nullOrBlank_returnsEmptyMap() {
        assertTrue(LongTextSerializer.fromJson(null).isEmpty());
        assertTrue(LongTextSerializer.fromJson("").isEmpty());
        assertTrue(LongTextSerializer.fromJson("  ").isEmpty());
    }

    @Test
    void toJson_nullMap_throws() {
        assertThrows(NullPointerException.class, () -> LongTextSerializer.toJson(null));
    }

    @Test
    void fromJson_invalidJson_throws() {
        assertThrows(IllegalArgumentException.class, () -> LongTextSerializer.fromJson("{bad}"));
    }

    // ========== JSON 数组（回调地址 redirect_uris） ==========

    @Test
    void toJsonArray_list_producesJsonArray() {
        List<String> uris = List.of("https://a.com/cb", "https://b.com/cb");
        String json = LongTextSerializer.toJsonArray(uris);
        assertEquals("[\"https://a.com/cb\",\"https://b.com/cb\"]", json);
    }

    @Test
    void fromJsonArray_validJson_returnsList() {
        String json = "[\"https://a.com/cb\",\"https://b.com/cb\"]";
        List<String> list = LongTextSerializer.fromJsonArray(json);
        assertEquals(2, list.size());
        assertEquals("https://a.com/cb", list.get(0));
    }

    @Test
    void fromJsonArray_nullOrBlank_returnsEmptyList() {
        assertTrue(LongTextSerializer.fromJsonArray(null).isEmpty());
        assertTrue(LongTextSerializer.fromJsonArray("").isEmpty());
    }

    @Test
    void toJsonArray_nullList_throws() {
        assertThrows(NullPointerException.class, () -> LongTextSerializer.toJsonArray(null));
    }

    // ========== 逗号分隔（命中规则 hit_rules、授权类型 grant_types） ==========

    @Test
    void toCommaSeparated_list_producesString() {
        List<String> rules = List.of("rule_new_device", "rule_geo_change");
        assertEquals("rule_new_device,rule_geo_change", LongTextSerializer.toCommaSeparated(rules));
    }

    @Test
    void fromCommaSeparated_string_returnsList() {
        List<String> list = LongTextSerializer.fromCommaSeparated("authorization_code,refresh_token");
        assertEquals(List.of("authorization_code", "refresh_token"), list);
    }

    @Test
    void fromCommaSeparated_withSpaces_trimmed() {
        List<String> list = LongTextSerializer.fromCommaSeparated(" a , b , c ");
        assertEquals(List.of("a", "b", "c"), list);
    }

    @Test
    void fromCommaSeparated_nullOrBlank_returnsEmptyList() {
        assertTrue(LongTextSerializer.fromCommaSeparated(null).isEmpty());
        assertTrue(LongTextSerializer.fromCommaSeparated("").isEmpty());
    }

    @Test
    void toCommaSeparated_filtersBlankEntries() {
        List<String> list = List.of("a", "", "b");
        assertEquals("a,b", LongTextSerializer.toCommaSeparated(list));
    }

    // ========== 空格分隔（OAuth scopes） ==========

    @Test
    void toSpaceSeparated_list_producesString() {
        List<String> scopes = List.of("openid", "profile", "email");
        assertEquals("openid profile email", LongTextSerializer.toSpaceSeparated(scopes));
    }

    @Test
    void fromSpaceSeparated_string_returnsList() {
        List<String> list = LongTextSerializer.fromSpaceSeparated("openid profile email");
        assertEquals(List.of("openid", "profile", "email"), list);
    }

    @Test
    void fromSpaceSeparated_multipleSpaces_handled() {
        List<String> list = LongTextSerializer.fromSpaceSeparated("openid  profile   email");
        assertEquals(List.of("openid", "profile", "email"), list);
    }

    @Test
    void fromSpaceSeparated_nullOrBlank_returnsEmptyList() {
        assertTrue(LongTextSerializer.fromSpaceSeparated(null).isEmpty());
        assertTrue(LongTextSerializer.fromSpaceSeparated("").isEmpty());
    }

    // ========== 往返一致性 ==========

    @Test
    void jsonRoundTrip_snapshot() {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("key", "value");
        assertEquals(original, LongTextSerializer.fromJson(LongTextSerializer.toJson(original)));
    }

    @Test
    void jsonArrayRoundTrip_redirectUris() {
        List<String> original = List.of("https://x.com/cb");
        assertEquals(original, LongTextSerializer.fromJsonArray(LongTextSerializer.toJsonArray(original)));
    }

    @Test
    void commaSeparatedRoundTrip_hitRules() {
        List<String> original = List.of("rule_a", "rule_b");
        assertEquals(original, LongTextSerializer.fromCommaSeparated(LongTextSerializer.toCommaSeparated(original)));
    }

    @Test
    void spaceSeparatedRoundTrip_scopes() {
        List<String> original = List.of("openid", "profile");
        assertEquals(original, LongTextSerializer.fromSpaceSeparated(LongTextSerializer.toSpaceSeparated(original)));
    }
}
