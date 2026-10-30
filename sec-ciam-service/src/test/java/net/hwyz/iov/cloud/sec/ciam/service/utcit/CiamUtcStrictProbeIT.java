package net.hwyz.iov.cloud.sec.ciam.service.utcit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SEC-CIAM-DSN-CR-002 C1：严格 UTC 启动探针验证（US-002-2 / RD-002-2 / TC-002-3，C1 范围）。
 * <p>
 * 覆盖（对齐 {@code MysqlTimeStartupProbe} 四道校验 + CIAM 自身证据）：
 * <ul>
 *   <li>正例（UTC JVM + 合法 URL + 会话初始化 + 真实连接）：上下文启动通过，真实会话
 *       {@code @@session.time_zone} 为 UTC 等价，{@code NOW(3)==UTC_TIMESTAMP(3)}；</li>
 *   <li>负例 A（非 UTC JVM）：探针 fail-fast 拒绝，启动失败消息含「JVM/OS 时区非 UTC」；</li>
 *   <li>负例 B（URL 缺 UTC 参数）：URL 解析式校验拒绝，启动失败消息含「缺少 UTC 时区参数」。</li>
 * </ul>
 * 边界：
 * <ul>
 *   <li>探针只校验，不运行时 SET 纠正；失败阻断启动/就绪（不静默修正）；</li>
 *   <li>本测试 {@code iov.mysql.time.enabled=true} 仅为隔离测试环境（边界 4：测试专用配置，
 *       不动共享 mysql.yaml、不动正常启动默认）；</li>
 *   <li>负例 A 使用进程内 {@code TimeZone.setDefault} 模拟非 UTC JVM（TC-002-2 分离要求：
 *       严格拒绝与 codec 可移植性分开验证）；另以独立 JVM 运行（{@code -Duser.timezone=Asia/Shanghai}）
 *       的启动失败输出作为附加证据。</li>
 * </ul>
 */
@SpringBootTest(classes = CiamUtcItTestApplication.class,
        properties = {
                "spring.cloud.bootstrap.enabled=false",
                "spring.flyway.enabled=false",
                "iov.mysql.time.enabled=true"
        })
@DisplayName("C1: 严格 UTC 启动探针（隔离库，enabled=true）")
class CiamUtcStrictProbeIT extends CiamUtcItBase {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("正例: 严格启动通过，真实会话 UTC 等价且 NOW(3)==UTC_TIMESTAMP(3)")
    void strictStartup_pass_realSessionUtc() throws Exception {
        // 上下文能加载即探针通过（SmartInitializingSingleton fail-fast）
        assertNotNull(dataSource);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT @@session.time_zone AS session_tz, NOW(3) AS now3, UTC_TIMESTAMP(3) AS utc3");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            String sessionTz = rs.getString("session_tz");
            Timestamp now3 = rs.getTimestamp("now3");
            Timestamp utc3 = rs.getTimestamp("utc3");
            String normalized = sessionTz == null ? "" : sessionTz.trim().toLowerCase();
            assertTrue(normalized.equals("+00:00") || normalized.equals("utc")
                            || normalized.equals("etc/utc") || normalized.equals("gmt") || normalized.equals("z"),
                    "实际会话时区应为 UTC 等价: " + sessionTz);
            assertNotNull(now3);
            assertNotNull(utc3);
            assertEquals(now3, utc3, "会话 UTC 下 NOW(3) 与 UTC_TIMESTAMP(3) 必须一致");
        }
    }

    @Test
    @DisplayName("负例 A: 非 UTC JVM 严格启动拒绝（进程内 TimeZone.setDefault 模拟）")
    void strictStartup_rejectNonUtcJvm() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            ApplicationContextRunner runner = new ApplicationContextRunner()
                    .withUserConfiguration(CiamUtcItTestApplication.class)
                    .withPropertyValues(
                            "spring.cloud.bootstrap.enabled=false",
                            "spring.flyway.enabled=false",
                            "iov.mysql.time.enabled=true",
                            "spring.datasource.url=" + FULL_URL,
                            "spring.datasource.username=" + USER,
                            "spring.datasource.password=" + PASSWORD);
            runner.run(ctx -> {
                // 探针 fail-fast：上下文必须启动失败，且失败原因必须是 JVM 非 UTC
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure()).isNotNull();
                assertThat(ctx.getStartupFailure().getMessage())
                        .contains("JVM/OS 时区非 UTC");
            });
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    @DisplayName("负例 B: URL 缺 UTC 参数严格启动拒绝")
    void strictStartup_rejectUrlWithoutUtcParams() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(CiamUtcItTestApplication.class)
                .withPropertyValues(
                        "spring.cloud.bootstrap.enabled=false",
                        "spring.flyway.enabled=false",
                        "iov.mysql.time.enabled=true",
                        "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/ciam_utc_it?useSSL=false",
                        "spring.datasource.username=" + USER,
                        "spring.datasource.password=" + PASSWORD);
        runner.run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).isNotNull();
            assertThat(ctx.getStartupFailure().getMessage())
                    .contains("缺少 UTC 时区参数");
        });
    }
}
