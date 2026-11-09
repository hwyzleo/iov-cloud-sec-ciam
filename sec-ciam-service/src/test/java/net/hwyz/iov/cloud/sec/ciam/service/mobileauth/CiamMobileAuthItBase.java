package net.hwyz.iov.cloud.sec.ciam.service.mobileauth;

import org.flywaydb.core.Flyway;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SEC-CIAM-DSN-CR-003 手机认证集成测试基类（TC-003-2/3/5/6/7/8/9/12/14）。
 * <p>
 * 环境门禁（对齐 SEC-CIAM-DSN-CR-002 IT 模式与「环境缺失不得自动跳过后算通过」）：
 * <ul>
 *   <li>MySQL：系统属性 {@code ciam.it.mysql.*} 优先，其次环境变量 {@code CIAM_IT_MYSQL_*}；
 *       缺省值仅指向本机候选环境，不包含任何账号口令；</li>
 *   <li>Redis：{@code ciam.it.redis.*} / {@code CIAM_IT_REDIS_*}，默认 DB 15 隔离；</li>
 *   <li>{@code CIAM_IT_MYSQL_REQUIRED=true} / {@code CIAM_IT_REDIS_REQUIRED=true} 时，
 *       依赖不可用一律 FAIL，不允许静默跳过；</li>
 *   <li>隔离性：仅使用专用库 {@code ciam_cr003_it}（utf8mb4）与 Redis DB 15，
 *       迁移 V1~V6 由类加载期 Flyway 执行；</li>
 *   <li>测试仅使用合成数据，不接触真实私钥/令牌/生产记录。</li>
 * </ul>
 */
abstract class CiamMobileAuthItBase {

    static final String RUN_SUFFIX = String.valueOf(ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999));

    static final String FULL_URL = System.getProperty("ciam.it.mysql.url",
            System.getenv().getOrDefault("CIAM_IT_MYSQL_URL",
                    "jdbc:mysql://127.0.0.1:3306/ciam_cr003_it?useSSL=false&allowPublicKeyRetrieval=true"
                            + "&characterEncoding=utf8&connectionTimeZone=UTC"
                            + "&forceConnectionTimeZoneToSession=true&preserveInstants=true"));
    static final String USER = System.getProperty("ciam.it.mysql.user",
            System.getenv().getOrDefault("CIAM_IT_MYSQL_USER", "root"));
    static final String PASSWORD = System.getProperty("ciam.it.mysql.password",
            System.getenv().getOrDefault("CIAM_IT_MYSQL_PASSWORD", ""));
    static final boolean MYSQL_REQUIRED = Boolean.parseBoolean(System.getProperty("ciam.it.mysql.required",
            System.getenv().getOrDefault("CIAM_IT_MYSQL_REQUIRED", "false")));

    static final String REDIS_HOST = System.getProperty("ciam.it.redis.host",
            System.getenv().getOrDefault("CIAM_IT_REDIS_HOST", "127.0.0.1"));
    static final int REDIS_PORT = Integer.parseInt(System.getProperty("ciam.it.redis.port",
            System.getenv().getOrDefault("CIAM_IT_REDIS_PORT", "6379")));
    static final String REDIS_PASSWORD = System.getProperty("ciam.it.redis.password",
            System.getenv().getOrDefault("CIAM_IT_REDIS_PASSWORD", ""));
    static final int REDIS_DATABASE = Integer.parseInt(System.getProperty("ciam.it.redis.database",
            System.getenv().getOrDefault("CIAM_IT_REDIS_DATABASE", "15")));
    static final boolean REDIS_REQUIRED = Boolean.parseBoolean(System.getProperty("ciam.it.redis.required",
            System.getenv().getOrDefault("CIAM_IT_REDIS_REQUIRED", "false")));

    private static final String SCHEMA = extractSchema(FULL_URL);

    static {
        try {
            ensureSchemaAndMigrate();
        } catch (Exception e) {
            if (MYSQL_REQUIRED) {
                throw new IllegalStateException(
                        "CIAM CR-003 集成测试 MySQL 环境缺失或迁移失败（CIAM_IT_MYSQL_REQUIRED=true）："
                                + "不允许静默跳过。错误: " + e.getMessage(), e);
            }
            System.err.println("[CiamMobileAuthItBase] MySQL 不可用且未强制：相关测试将如实失败，不计为通过。"
                    + "错误: " + e.getMessage());
        }
        System.err.println("[CiamMobileAuthItBase] 环境: mysql=" + SCHEMA
                + ", redis=" + REDIS_HOST + ":" + REDIS_PORT + "/" + REDIS_DATABASE);
    }

    @DynamicPropertySource
    static void dataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> FULL_URL);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
        registry.add("ciam.it.redis.host", () -> REDIS_HOST);
        registry.add("ciam.it.redis.port", () -> REDIS_PORT);
        registry.add("ciam.it.redis.password", () -> REDIS_PASSWORD);
        registry.add("ciam.it.redis.database", () -> REDIS_DATABASE);
    }

    /**
     * 类加载期（早于任何 Spring 上下文/Flyway 自动配置）：
     * 1) 确保隔离库存在；2) 用 Flyway API 执行 V1~V6 迁移（幂等，checksum 校验）。
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

    private static String serverLevelUrl(String url) {
        int q = url.indexOf('?');
        String head = q >= 0 ? url.substring(0, q) : url;
        int slash = head.lastIndexOf('/');
        return head.substring(0, slash + 1) + (q >= 0 ? url.substring(q) : "");
    }
}
