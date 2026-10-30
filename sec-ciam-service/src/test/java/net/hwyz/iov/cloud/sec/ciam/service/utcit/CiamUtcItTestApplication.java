package net.hwyz.iov.cloud.sec.ciam.service.utcit;

import com.alibaba.druid.pool.DruidDataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * SEC-CIAM-DSN-CR-002 C1 隔离集成测试最小装配（真实 MySQL + MyBatis-Plus + Druid + Flyway 手动）。
 * <p>
 * 仅装配数据访问所需自动配置，排除依赖外部基础设施的框架 starter 自动装配
 * （security/redis/kafka/audit/datascope 等，按名称排除避免编译期依赖），
 * 不加载 CIAM 业务组件，不使用 Nacos。
 * {@code MysqlTimeAutoConfiguration}（framework-mysql-starter 注册）随 DataSource bean 自动生效：
 * {@code iov.mysql.time.enabled} 由各测试类属性控制（映射测试 false / 严格探针 true）。
 *
 * @author hwyz_leo
 * @since 2026-10-30
 */
@Configuration
@EnableAutoConfiguration(excludeName = {
        // spring-boot redis（无 Redis 基础设施）
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        // Flyway 自动装配（本测试在静态块手动执行 V1~V5 迁移，避免双 Flyway 与 Druid 连接污染级联）
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        // 框架 security
        "net.hwyz.iov.cloud.framework.security.config.WebMvcConfig",
        "net.hwyz.iov.cloud.framework.security.service.TokenService",
        "net.hwyz.iov.cloud.framework.security.aspect.PreAuthorizeAspect",
        "net.hwyz.iov.cloud.framework.security.aspect.InnerAuthAspect",
        "net.hwyz.iov.cloud.framework.security.handler.GlobalExceptionHandler",
        "net.hwyz.iov.cloud.framework.security.crypto.config.CryptoAutoConfiguration",
        "net.hwyz.iov.cloud.framework.security.crypto.revocation.config.PkiRevocationAutoConfiguration",
        // 框架 audit / kafka / datascope / redis
        "net.hwyz.iov.cloud.framework.audit.service.AsyncLogService",
        "net.hwyz.iov.cloud.framework.audit.aspect.LogAspect",
        "net.hwyz.iov.cloud.framework.kafka.autoconfigure.KafkaAutoConfiguration",
        "net.hwyz.iov.cloud.framework.kafka.autoconfigure.KafkaAdminOperationsAutoConfiguration",
        "net.hwyz.iov.cloud.framework.kafka.autoconfigure.KafkaTopicProvisioningAutoConfiguration",
        "net.hwyz.iov.cloud.framework.datascope.aspect.DataScopeAspect",
        "net.hwyz.iov.cloud.framework.redis.config.RedisConfig",
        "net.hwyz.iov.cloud.framework.redis.config.LoadBalancerCacheConfig",
        "net.hwyz.iov.cloud.framework.redis.service.RedisService",
        // 分页（非本批范围）
        "com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration"
})
@MapperScan("net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper")
public class CiamUtcItTestApplication {

    /**
     * 显式 Druid 数据源：惰性连接（bean 创建不连库），URL/凭据来自
     * {@link CiamUtcItBase} 的 {@link org.springframework.test.context.DynamicPropertySource}
     * （env/系统属性注入，不落盘）。
     */
    @Bean
    public DataSource dataSource(@Value("${spring.datasource.url}") String url,
                                 @Value("${spring.datasource.username:root}") String user,
                                 @Value("${spring.datasource.password:}") String password) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        return dataSource;
    }
}
