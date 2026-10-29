package net.hwyz.iov.cloud.sec.ciam.service.timecontract;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.SessionVo;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.UserProfileVo;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.UserSearchResponse;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserSearchDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-CIAM-DSN-CR-001 MVC 端到端集成测试（对应设计 §4.1：真实 MVC mapper、两模式）。
 * <p>
 * 生产配置（application.yml：epoch-millis + Asia/Shanghai）下，真实 CIAM VO/DTO
 * 经 MVC HttpMessageConverter（生产 ObjectMapper）输出 epoch 毫秒 golden；
 * POST 双读兼容历史 13 位毫秒 / 10 位秒 / ISO 字符串；非法格式拒绝。
 */
@SpringBootTest(classes = CiamJacksonMvcIntegrationTest.TestApplication.class,
        webEnvironment = WebEnvironment.MOCK,
        properties = {
                "spring.cloud.bootstrap.enabled=false",
                "spring.autoconfigure.exclude=" + CiamJacksonMvcIntegrationTest.EXCLUDE_AUTO_CONFIGS
        })
@AutoConfigureMockMvc
class CiamJacksonMvcIntegrationTest {

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

    private static final Instant LOGIN_TIME = Instant.parse("2026-10-28T12:43:32Z");
    private static final OffsetDateTime CREATE_TIME = OffsetDateTime.parse("2026-10-28T20:43:32+08:00");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void GET会话VO_Instant输出epoch毫秒golden() throws Exception {
        mockMvc.perform(get("/ciam-time/session"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"sessionId\":\"s-001\",\"userId\":\"u-001\","
                                + "\"loginTime\":" + LOGIN_TIME.toEpochMilli() + ","
                                + "\"lastActiveTime\":" + LOGIN_TIME.plusSeconds(60).toEpochMilli() + ","
                                + "\"expireTime\":" + LOGIN_TIME.plusSeconds(3600).toEpochMilli() + "}"));
    }

    @Test
    void GET资料VO_LocalDate保持字符串_Instant为毫秒() throws Exception {
        mockMvc.perform(get("/ciam-time/profile"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"userId\":\"u-001\",\"birthday\":\"1990-05-01\","
                                + "\"lastLoginTime\":" + LOGIN_TIME.toEpochMilli() + "}"));
    }

    @Test
    void GET搜索VO_Instant输出epoch毫秒golden() throws Exception {
        mockMvc.perform(get("/ciam-time/search"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"userId\":\"u-002\",\"lastLoginTime\":" + LOGIN_TIME.toEpochMilli() + ","
                                + "\"createTime\":" + LOGIN_TIME.toEpochMilli() + "}"));
    }

    @Test
    void GET搜索DTO_OffsetDateTime输出epoch毫秒golden() throws Exception {
        mockMvc.perform(get("/ciam-time/dto"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"userId\":\"u-002\","
                                + "\"lastLoginTime\":" + CREATE_TIME.toInstant().toEpochMilli() + ","
                                + "\"createTime\":" + CREATE_TIME.toInstant().toEpochMilli() + "}"));
    }

    @Test
    void POST兼容历史13位毫秒输入() throws Exception {
        mockMvc.perform(post("/ciam-time/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s-002\",\"loginTime\":" + LOGIN_TIME.toEpochMilli() + "}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"sessionId\":\"s-002\",\"loginTime\":" + LOGIN_TIME.toEpochMilli() + "}"));
    }

    @Test
    void POST兼容历史10位秒输入() throws Exception {
        mockMvc.perform(post("/ciam-time/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s-003\",\"loginTime\":" + LOGIN_TIME.getEpochSecond() + "}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"sessionId\":\"s-003\",\"loginTime\":" + LOGIN_TIME.toEpochMilli() + "}"));
    }

    @Test
    void POST兼容ISO字符串输入() throws Exception {
        mockMvc.perform(post("/ciam-time/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s-004\",\"loginTime\":\"" + LOGIN_TIME + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"sessionId\":\"s-004\",\"loginTime\":" + LOGIN_TIME.toEpochMilli() + "}"));
    }

    @Test
    void POST秒小数输入_拒绝() throws Exception {
        mockMvc.perform(post("/ciam-time/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s-005\",\"loginTime\":1790059412.5}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void POST数组输入_拒绝() throws Exception {
        mockMvc.perform(post("/ciam-time/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s-006\",\"loginTime\":[2026,10,28]}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void POST非法ISO输入_拒绝() throws Exception {
        mockMvc.perform(post("/ciam-time/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"s-007\",\"loginTime\":\"not-a-date\"}"))
                .andExpect(status().is5xxServerError());
    }

    @Configuration
    @EnableAutoConfiguration
    @Import(CiamJacksonMvcIntegrationTest.CiamTimeController.class)
    static class TestApplication {
    }

    @RestController
    static class CiamTimeController {

        @GetMapping("/ciam-time/session")
        public SessionVo session() {
            return SessionVo.builder()
                    .sessionId("s-001")
                    .userId("u-001")
                    .loginTime(LOGIN_TIME)
                    .lastActiveTime(LOGIN_TIME.plusSeconds(60))
                    .expireTime(LOGIN_TIME.plusSeconds(3600))
                    .build();
        }

        @PostMapping("/ciam-time/session")
        public SessionVo echoSession(@RequestBody SessionVo vo) {
            return vo;
        }

        @GetMapping("/ciam-time/profile")
        public UserProfileVo profile() {
            return UserProfileVo.builder()
                    .userId("u-001")
                    .birthday(LocalDate.of(1990, 5, 1))
                    .lastLoginTime(LOGIN_TIME)
                    .build();
        }

        @GetMapping("/ciam-time/search")
        public UserSearchResponse search() {
            return UserSearchResponse.builder()
                    .userId("u-002")
                    .lastLoginTime(LOGIN_TIME)
                    .createTime(LOGIN_TIME)
                    .build();
        }

        @GetMapping("/ciam-time/dto")
        public UserSearchDto searchDto() {
            return UserSearchDto.builder()
                    .userId("u-002")
                    .lastLoginTime(CREATE_TIME)
                    .createTime(CREATE_TIME)
                    .build();
        }
    }
}
