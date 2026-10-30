package net.hwyz.iov.cloud.sec.ciam.service.utcit;

import org.flywaydb.core.Flyway;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SEC-CIAM-DSN-CR-002 C1 隔离集成测试基类（US-002-2/3/7，TC-002-2/3/4 范围）。
 * <p>
 * 环境门禁（对齐 FW-MYSQL IT 模式与 CR-002「缺 MySQL 不得自动跳过后算通过」）：
 * <ul>
 *   <li>凭据读取：系统属性 {@code ciam.it.mysql.*} 优先，其次环境变量 {@code CIAM_IT_MYSQL_*}；
 *       缺省值仅指向本机候选环境（127.0.0.1:3306），<b>不包含任何账号口令</b>；</li>
 *   <li>{@code CIAM_IT_MYSQL_REQUIRED=true}（或 {@code -Dciam.it.mysql.required=true}）时，
 *       MySQL 不可用/凭据缺失/迁移失败一律 FAIL，不允许静默跳过；</li>
 *   <li>隔离性：仅使用专用库 {@code ciam_utc_it}（utf8mb4），不动 db_ciam 等既有库；
 *       迁移（V1~V5）由类加载期 Flyway 执行，每轮校验 checksum（已应用则跳过）；</li>
 *   <li>测试仅使用合成数据，不接触真实私钥/令牌/生产记录（边界 4）。</li>
 * </ul>
 * 双 JVM 可移植性（TC-002-2）：本基类测试在 {@code -Duser.timezone=UTC} 与
 * {@code -Duser.timezone=Asia/Shanghai} 两次独立 JVM 运行下断言同一 Instant 结果，
 * 两次输出即为可移植性证据（与「严格启动在非 UTC 拒绝」分开，见 CiamUtcStrictProbeIT）。
 */
abstract class CiamUtcItBase {

    /** 本批每次运行唯一后缀，避免同库重复运行主键冲突。 */
    static final String RUN_SUFFIX = String.valueOf(ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999));

    static final String FULL_URL = System.getProperty("ciam.it.mysql.url",
            System.getenv().getOrDefault("CIAM_IT_MYSQL_URL",
                    "jdbc:mysql://127.0.0.1:3306/ciam_utc_it?useSSL=false&allowPublicKeyRetrieval=true"
                            + "&characterEncoding=utf8&connectionTimeZone=UTC"
                            + "&forceConnectionTimeZoneToSession=true&preserveInstants=true"));
    static final String USER = System.getProperty("ciam.it.mysql.user",
            System.getenv().getOrDefault("CIAM_IT_MYSQL_USER", "root"));
    static final String PASSWORD = System.getProperty("ciam.it.mysql.password",
            System.getenv().getOrDefault("CIAM_IT_MYSQL_PASSWORD", ""));
    static final boolean REQUIRED = Boolean.parseBoolean(System.getProperty("ciam.it.mysql.required",
            System.getenv().getOrDefault("CIAM_IT_MYSQL_REQUIRED", "false")));

    private static final String SCHEMA = extractSchema(FULL_URL);

    static {
        try {
            ensureSchemaAndMigrate();
        } catch (Exception e) {
            if (REQUIRED) {
                throw new IllegalStateException(
                        "CIAM UTC 集成测试环境缺失或迁移失败（CIAM_IT_MYSQL_REQUIRED=true）：不允许静默跳过。"
                                + "错误: " + e.getMessage(), e);
            }
            System.err.println("[CiamUtcItBase] MySQL 不可用且未强制：相关测试将如实失败，不计为通过。"
                    + "错误: " + e.getMessage());
        }
    }

    @DynamicPropertySource
    static void utcDataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> FULL_URL);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    /**
     * 类加载期（早于任何 Spring 上下文/Flyway 自动配置）：
     * 1) 确保隔离库存在；2) 用 Flyway API 执行 V1~V5 迁移（幂等，checksum 校验）。
     * 上下文内 {@code spring.flyway.enabled=false}，避免双 Flyway。
     */
    private static void ensureSchemaAndMigrate() throws Exception {
        String serverUrl = serverLevelUrl(FULL_URL);
        try (Connection connection = DriverManager.getConnection(serverUrl, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS `" + SCHEMA
                    + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
        }
        Flyway flyway = Flyway.configure()
                .dataSource(FULL_URL, USER, PASSWORD)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    /** 从完整 JDBC URL 提取库名（最后一个 '/' 与 '?' 之间）。 */
    private static String extractSchema(String url) {
        String head = url;
        int q = url.indexOf('?');
        if (q >= 0) {
            head = url.substring(0, q);
        }
        int slash = head.lastIndexOf('/');
        if (slash < 0 || slash == head.length() - 1) {
            throw new IllegalArgumentException("无法从 JDBC URL 解析库名: " + head);
        }
        return head.substring(slash + 1);
    }

    /** 构造 server 级 URL（去掉库名，保留参数），用于 CREATE DATABASE。 */
    private static String serverLevelUrl(String url) {
        int q = url.indexOf('?');
        String head = q >= 0 ? url.substring(0, q) : url;
        int slash = head.lastIndexOf('/');
        return head.substring(0, slash + 1) + (q >= 0 ? url.substring(q) : "");
    }
}
