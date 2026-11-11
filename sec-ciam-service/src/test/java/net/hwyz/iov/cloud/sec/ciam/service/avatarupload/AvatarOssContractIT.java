package net.hwyz.iov.cloud.sec.ciam.service.avatarupload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 与真实 EDD-OSS 的契约往返测试（SEC-CIAM-DSN-CR-004 TC-004-12，US-004-6）。
 * <p>
 * 门禁：
 * <ul>
 *   <li>需要 {@code CIAM_IT_OSS_BASE_URL}（例如 http://127.0.0.1:8080）与 {@code CIAM_IT_OSS_CALLER}（默认 sec-ciam）；</li>
 *   <li>{@code CIAM_IT_OSS_REQUIRED=true} 时环境缺失一律 FAIL，不允许静默跳过；</li>
 *   <li>否则中止为「未执行」，并在测试汇总中记为未执行/阻断验收，不得计为通过。</li>
 * </ul>
 * 本测试按 EDD-OSS Service Image API 契约执行：申请 → COS 直传 → 确认，并校验 accessUrl 可访问。
 * 使用 1x1 PNG 合成数据，不接触生产对象与真实用户数据。
 */
class AvatarOssContractIT {

    private static final String BASE_URL = System.getProperty("ciam.it.oss.base-url",
            System.getenv().getOrDefault("CIAM_IT_OSS_BASE_URL", ""));
    private static final String CALLER = System.getProperty("ciam.it.oss.caller",
            System.getenv().getOrDefault("CIAM_IT_OSS_CALLER", "sec-ciam"));
    private static final boolean REQUIRED = Boolean.parseBoolean(System.getProperty("ciam.it.oss.required",
            System.getenv().getOrDefault("CIAM_IT_OSS_REQUIRED", "false")));

    private static final byte[] PNG_1X1 = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 契约响应提取结果。
     *
     * @param objectId    上传任务标识
     * @param uploadUrl   预签名上传地址
     * @param httpMethod  上传方法
     * @param contentType 上传 Content-Type
     * @param accessUrl   已确认图片的公有读地址
     */
    private record Captured(String objectId, String uploadUrl, String httpMethod,
                            String contentType, String accessUrl) {
    }

    private static void requireEnvironmentOrAbort() {
        if (BASE_URL.isBlank()) {
            if (REQUIRED) {
                throw new IllegalStateException(
                        "CIAM_IT_OSS_REQUIRED=true 但 CIAM_IT_OSS_BASE_URL 缺失：真实 OSS 契约验收不得跳过");
            }
            Assumptions.abort("未执行：缺少 CIAM_IT_OSS_BASE_URL，真实 OSS 往返契约验收保持阻断（US-004-6）");
        }
    }

    @Test
    @DisplayName("TC-004-12 CIAM 与 EDD-OSS 的申请→直传→确认真实往返")
    void realOssRoundTrip() throws IOException {
        requireEnvironmentOrAbort();
        Captured upload = requestUpload();
        assertNotNull(upload.objectId(), "OSS 必须返回 objectId");
        assertNotNull(upload.uploadUrl(), "OSS 必须返回预签名 uploadUrl");
        assertEquals("PUT", upload.httpMethod());

        putToCos(upload);
        Captured complete = completeUpload(upload.objectId());

        assertNotNull(complete.accessUrl(), "完成确认必须返回 accessUrl");
        assertTrue(complete.accessUrl().startsWith("http"), "accessUrl 应为可访问的 HTTP 地址");
        assertEquals(200, head(complete.accessUrl()), "公有读对象应可直接访问");
    }

    private Captured requestUpload() throws IOException {
        String body = "{\"originalFilename\":\"avatar-contract-it.png\","
                + "\"contentType\":\"image/png\",\"size\":" + PNG_1X1.length + "}";
        return postJson("/api/service/image/v1/uploads", body);
    }

    private Captured completeUpload(String objectId) throws IOException {
        return postJson("/api/service/image/v1/uploads/" + objectId + "/complete", "{}");
    }

    private Captured postJson(String path, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(BASE_URL + path).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-Caller-Service", CALLER);
        connection.setDoOutput(true);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int status = connection.getResponseCode();
        String response = read(connection.getInputStream());
        assertEquals(200, status, "OSS Service Image API 应返回 200，实际响应: " + response);
        JsonNode node = objectMapper.readTree(response);
        return new Captured(text(node, "objectId"), text(node, "uploadUrl"),
                text(node, "httpMethod"), text(node, "contentType"), text(node, "accessUrl"));
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private void putToCos(Captured upload) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(upload.uploadUrl()).toURL().openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", upload.contentType());
        connection.setDoOutput(true);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(PNG_1X1);
        }
        assertEquals(200, connection.getResponseCode(), "预签名直传应成功");
    }

    private int head(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("HEAD");
        return connection.getResponseCode();
    }

    private String read(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        in.transferTo(buffer);
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
