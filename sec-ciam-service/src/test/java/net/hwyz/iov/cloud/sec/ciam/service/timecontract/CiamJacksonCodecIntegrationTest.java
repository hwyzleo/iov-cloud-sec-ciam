package net.hwyz.iov.cloud.sec.ciam.service.timecontract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.framework.web.autoconfigure.IovJacksonAutoConfiguration;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.SessionVo;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.UserProfileVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SEC-CIAM-DSN-CR-001 时间契约 codec 集成测试（对应设计 §4.1 真实测试证据）。
 * <p>
 * 加载真实 Boot 自动配置 + 框架 iov.web.jackson 自动配置，注入生产 ObjectMapper
 * （应用 CIAM application.yml 的 date-format=epoch-millis / zone-id=Asia/Shanghai）：
 * <ul>
 *   <li>epoch-millis 三类型写出 golden（含真实 CIAM VO）；</li>
 *   <li>双读：13 位毫秒 / 10 位秒 / ISO 字符串归一至同一时刻；</li>
 *   <li>非法格式拒绝（数组/秒小数/非法 ISO/溢出/错误类型/数字字符串），null 不转 epoch0；</li>
 *   <li>LocalDate（birthday）保持字符串、普通 long 不受影响（排除范围）；</li>
 *   <li>不依赖 JVM 默认时区（UTC/上海重跑一致）；</li>
 *   <li>iso 模式输出含 Z/offset；未配置 zone-id 探针阻断启动。</li>
 * </ul>
 */
@SpringBootTest(classes = CiamJacksonCodecIntegrationTest.TestApplication.class,
        webEnvironment = WebEnvironment.MOCK,
        properties = {
                "spring.cloud.bootstrap.enabled=false",
                "spring.autoconfigure.exclude=" + CiamJacksonCodecIntegrationTest.EXCLUDE_AUTO_CONFIGS
        })
class CiamJacksonCodecIntegrationTest {

    /**
     * 测试仅需要 Jackson + Web 自动配置；排除数据源/审计/安全/数据权限/Kafka 等
     * 依赖外部基础设施或其他服务 bean 的框架自动配置（真实装配由生产环境提供）。
     */
    static final String EXCLUDE_AUTO_CONFIGS = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
            + "com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration,"
            + "net.hwyz.iov.cloud.framework.mysql.config.MyBatisPlusConfig,"
            + "net.hwyz.iov.cloud.framework.audit.aspect.LogAspect,"
            + "net.hwyz.iov.cloud.framework.audit.service.AsyncLogService,"
            + "net.hwyz.iov.cloud.framework.security.config.WebMvcConfig,"
            + "net.hwyz.iov.cloud.framework.security.service.TokenService,"
            + "net.hwyz.iov.cloud.framework.security.aspect.PreAuthorizeAspect,"
            + "net.hwyz.iov.cloud.framework.security.aspect.InnerAuthAspect,"
            + "net.hwyz.iov.cloud.framework.security.handler.GlobalExceptionHandler,"
            + "net.hwyz.iov.cloud.framework.security.crypto.config.CryptoAutoConfiguration,"
            + "net.hwyz.iov.cloud.framework.security.crypto.revocation.config.PkiRevocationAutoConfiguration,"
            + "net.hwyz.iov.cloud.framework.datascope.aspect.DataScopeAspect,"
            + "net.hwyz.iov.cloud.framework.kafka.autoconfigure.KafkaAutoConfiguration,"
            + "net.hwyz.iov.cloud.framework.kafka.autoconfigure.KafkaAdminOperationsAutoConfiguration,"
            + "net.hwyz.iov.cloud.framework.kafka.autoconfigure.KafkaTopicProvisioningAutoConfiguration";

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final Instant SAMPLE_INSTANT = Instant.parse("2026-10-28T12:43:32Z");
    private static final OffsetDateTime SAMPLE_OFFSET = OffsetDateTime.parse("2026-10-28T20:43:32+08:00");
    private static final LocalDateTime SAMPLE_LOCAL = LocalDateTime.parse("2026-10-14T14:26:13");

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 生产配置_epochMillis模式_三类型输出13位毫秒golden() {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("instant", SAMPLE_INSTANT);
        sample.put("offset", SAMPLE_OFFSET);
        sample.put("local", SAMPLE_LOCAL);
        JsonNode node = objectMapper.valueToTree(sample);
        assertThat(node.get("instant").isIntegralNumber()).isTrue();
        assertThat(node.get("instant").longValue()).isEqualTo(SAMPLE_INSTANT.toEpochMilli());
        assertThat(node.get("offset").longValue()).isEqualTo(SAMPLE_OFFSET.toInstant().toEpochMilli());
        assertThat(node.get("local").longValue())
                .isEqualTo(SAMPLE_LOCAL.atZone(SHANGHAI).toInstant().toEpochMilli());
    }

    @Test
    void 真实CIAM会话VO_Instant字段输出epoch毫秒() {
        SessionVo vo = SessionVo.builder()
                .sessionId("s-001")
                .userId("u-001")
                .loginTime(SAMPLE_INSTANT)
                .lastActiveTime(SAMPLE_INSTANT.plusSeconds(60))
                .expireTime(SAMPLE_INSTANT.plusSeconds(3600))
                .build();
        JsonNode node = objectMapper.valueToTree(vo);
        assertThat(node.get("loginTime").longValue()).isEqualTo(SAMPLE_INSTANT.toEpochMilli());
        assertThat(node.get("lastActiveTime").longValue())
                .isEqualTo(SAMPLE_INSTANT.plusSeconds(60).toEpochMilli());
        assertThat(node.get("expireTime").longValue())
                .isEqualTo(SAMPLE_INSTANT.plusSeconds(3600).toEpochMilli());
        assertThat(node.get("sessionId").asText()).isEqualTo("s-001");
    }

    @Test
    void 真实CIAM资料VO_LocalDate保持字符串_Instant为毫秒() {
        UserProfileVo vo = UserProfileVo.builder()
                .userId("u-001")
                .birthday(LocalDate.of(1990, 5, 1))
                .lastLoginTime(SAMPLE_INSTANT)
                .build();
        JsonNode node = objectMapper.valueToTree(vo);
        assertThat(node.get("birthday").isTextual()).isTrue();
        assertThat(node.get("birthday").asText()).isEqualTo("1990-05-01");
        assertThat(node.get("lastLoginTime").longValue()).isEqualTo(SAMPLE_INSTANT.toEpochMilli());
    }

    @Test
    void 双读_13位毫秒_10位秒_ISO字符串_归一至同一时刻() throws Exception {
        long millis = SAMPLE_INSTANT.toEpochMilli();
        long seconds = SAMPLE_INSTANT.getEpochSecond();
        assertThat(objectMapper.readValue(String.valueOf(millis), Instant.class)).isEqualTo(SAMPLE_INSTANT);
        assertThat(objectMapper.readValue(String.valueOf(seconds), Instant.class)).isEqualTo(SAMPLE_INSTANT);
        assertThat(objectMapper.readValue("\"" + SAMPLE_INSTANT + "\"", Instant.class)).isEqualTo(SAMPLE_INSTANT);
    }

    @Test
    void null_不转换为epoch0() throws Exception {
        assertThat(objectMapper.readValue("null", Instant.class)).isNull();
        assertThat(objectMapper.readValue("null", LocalDateTime.class)).isNull();
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("instant", null);
        JsonNode node = objectMapper.valueToTree(sample);
        assertThat(node.get("instant").isNull()).isTrue();
    }

    @Test
    void 非法格式_数组_拒绝() {
        assertThatThrownBy(() -> objectMapper.readValue("[2026,10,28]", Instant.class))
                .hasMessageContaining("仅支持");
    }

    @Test
    void 非法格式_秒小数_拒绝() {
        assertThatThrownBy(() -> objectMapper.readValue("1790059412.5", Instant.class))
                .hasMessageContaining("拒绝小数");
    }

    @Test
    void 非法格式_非法ISO_拒绝() {
        assertThatThrownBy(() -> objectMapper.readValue("\"not-a-date\"", Instant.class))
                .isInstanceOf(Exception.class);
    }

    @Test
    void 非法格式_溢出及窗口外位数_拒绝() {
        assertThatThrownBy(() -> objectMapper.readValue("99999999999999999999", Instant.class))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> objectMapper.readValue("12345", Instant.class))
                .hasMessageContaining("窗口内");
    }

    @Test
    void 非法格式_错误类型_拒绝() {
        assertThatThrownBy(() -> objectMapper.readValue("true", Instant.class))
                .isInstanceOf(Exception.class);
    }

    @Test
    void 非法格式_数字字符串_拒绝() {
        assertThatThrownBy(() -> objectMapper.readValue("\"1790059412000\"", Instant.class))
                .isInstanceOf(Exception.class);
    }

    @Test
    void 普通long_不受javaTimeSerializer影响() throws Exception {
        JsonNode node = objectMapper.valueToTree(Map.of("ttl", 3600L, "count", 42));
        assertThat(node.get("ttl").isIntegralNumber()).isTrue();
        assertThat(node.get("ttl").longValue()).isEqualTo(3600L);
        assertThat(node.get("count").longValue()).isEqualTo(42);
    }

    @Test
    void 不依赖JVM默认时区_UTC与上海重跑结果一致() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("instant", SAMPLE_INSTANT);
            sample.put("offset", SAMPLE_OFFSET);
            sample.put("local", SAMPLE_LOCAL);

            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            String utcJson = objectMapper.writeValueAsString(sample);

            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            String shanghaiJson = objectMapper.writeValueAsString(sample);

            assertThat(utcJson).isEqualTo(shanghaiJson);
            assertThat(shanghaiJson)
                    .contains("\"local\":" + SAMPLE_LOCAL.atZone(SHANGHAI).toInstant().toEpochMilli());
        } finally {
            TimeZone.setDefault(original);
        }
    }

    // ---------- 模式与探针（ApplicationContextRunner，镜像生产装配） ----------

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        IovJacksonAutoConfiguration.class));
    }

    @Test
    void iso模式_输出含Z与offset() {
        runner()
                .withPropertyValues("iov.web.jackson.date-format=iso", "iov.web.jackson.zone-id=Asia/Shanghai")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ObjectMapper mapper = context.getBean(ObjectMapper.class);
                    Map<String, Object> sample = new LinkedHashMap<>();
                    sample.put("instant", SAMPLE_INSTANT);
                    sample.put("offset", SAMPLE_OFFSET);
                    sample.put("local", SAMPLE_LOCAL);
                    JsonNode node = mapper.valueToTree(sample);
                    assertThat(node.get("instant").asText()).isEqualTo("2026-10-28T12:43:32Z");
                    assertThat(node.get("offset").asText()).isEqualTo("2026-10-28T20:43:32+08:00");
                    assertThat(node.get("local").asText()).isEqualTo("2026-10-14T14:26:13");
                });
    }

    @Test
    void epochMillis模式_未配置zone_id_启动探针阻断() {
        runner()
                .withPropertyValues("iov.web.jackson.date-format=epoch-millis")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void epochMillis模式_配置zone_id_探针通过() {
        runner()
                .withPropertyValues("iov.web.jackson.date-format=epoch-millis", "iov.web.jackson.zone-id=Asia/Shanghai")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
