package net.hwyz.iov.cloud.sec.ciam.service.avatarupload;

import com.alibaba.druid.pool.DruidDataSource;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.AvatarUploadAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.UserProfileAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.AvatarUploadRegistry;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageObjectInfo;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageStorageGateway;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageUploadTicket;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.cache.RedisAvatarUploadRegistry;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * SEC-CIAM-DSN-CR-004 隔离集成测试最小装配（真实 MySQL + 真实 Redis）。
 * <p>
 * 不使用 Nacos、不加载 CIAM 全量业务组件；图片存储网关使用可控的桩实现，
 * 与真实 EDD-OSS 的往返由 {@link AvatarOssContractIT} 单独承担。
 */
@Configuration
@EnableAutoConfiguration(excludeName = {
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "net.hwyz.iov.cloud.framework.security.config.WebMvcConfig",
        "net.hwyz.iov.cloud.framework.security.service.TokenService",
        "net.hwyz.iov.cloud.framework.security.aspect.PreAuthorizeAspect",
        "net.hwyz.iov.cloud.framework.security.aspect.InnerAuthAspect",
        "net.hwyz.iov.cloud.framework.security.handler.GlobalExceptionHandler",
        "net.hwyz.iov.cloud.framework.security.crypto.config.CryptoAutoConfiguration",
        "net.hwyz.iov.cloud.framework.security.crypto.revocation.config.PkiRevocationAutoConfiguration",
        "net.hwyz.iov.cloud.framework.audit.service.AsyncLogService",
        "net.hwyz.iov.cloud.framework.audit.aspect.LogAspect",
        "net.hwyz.iov.cloud.framework.kafka.autoconfigure.KafkaAutoConfiguration",
        "net.hwyz.iov.cloud.framework.kafka.autoconfigure.KafkaAdminOperationsAutoConfiguration",
        "net.hwyz.iov.cloud.framework.kafka.autoconfigure.KafkaTopicProvisioningAutoConfiguration",
        "net.hwyz.iov.cloud.framework.datascope.aspect.DataScopeAspect",
        "net.hwyz.iov.cloud.framework.redis.config.RedisConfig",
        "net.hwyz.iov.cloud.framework.redis.config.LoadBalancerCacheConfig",
        "net.hwyz.iov.cloud.framework.redis.service.RedisService",
        "com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration",
        // 本 CR 不验证框架时间治理探针（属 CR-002 范围）
        "net.hwyz.iov.cloud.framework.mysql.time.MysqlTimeAutoConfiguration"
})
@MapperScan("net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper")
@ComponentScan(basePackages = {
        "net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository",
        "net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter"
})
public class AvatarUploadItTestApplication {

    @Bean
    public DataSource dataSource(@Value("${spring.datasource.url}") String url,
                                 @Value("${spring.datasource.username:root}") String user,
                                 @Value("${spring.datasource.password:}") String password) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        // 环境缺失时快速失败，避免无限制重连导致测试挂起（环境不可用不得静默跳过）
        dataSource.setInitialSize(0);
        dataSource.setMaxWait(5000L);
        dataSource.setConnectionErrorRetryAttempts(0);
        dataSource.setBreakAfterAcquireFailure(true);
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${ciam.it.redis.host}") String host,
            @Value("${ciam.it.redis.port}") int port,
            @Value("${ciam.it.redis.password:}") String password,
            @Value("${ciam.it.redis.database:14}") int database) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }
        config.setDatabase(database);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public AuditLogger auditLogger() {
        return event -> {
        };
    }

    @Bean
    public AvatarUploadRegistry avatarUploadRegistry(StringRedisTemplate redisTemplate) {
        return new RedisAvatarUploadRegistry(redisTemplate);
    }

    /**
     * 可控的图片存储网关桩：objectId 即上传任务标识，accessUrl 由 record 生成。
     */
    @Bean
    public ImageStorageGateway imageStorageGateway() {
        return new ImageStorageGateway() {
            @Override
            public ImageUploadTicket generateUpload(String originalFilename, String contentType, long size) {
                String objectId = "it-" + originalFilename + "-" + size;
                return ImageUploadTicket.builder()
                        .objectId(objectId)
                        .objectKey("images/sec-ciam/" + objectId + ".png")
                        .uploadUrl("https://cos.example.com/presigned/" + objectId)
                        .httpMethod("PUT")
                        .contentType(contentType)
                        .expiresAt(System.currentTimeMillis() + 900_000L)
                        .build();
            }

            @Override
            public ImageObjectInfo completeUpload(String objectId) {
                return ImageObjectInfo.builder()
                        .objectId(objectId)
                        .accessUrl("https://bucket.cos.example.com/images/sec-ciam/" + objectId + ".png")
                        .contentType("image/png")
                        .size(204_800L)
                        .build();
            }
        };
    }

    @Bean
    public UserProfileAppService userProfileAppService(
            net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserProfileRepository profileRepository,
            AuditLogger auditLogger) {
        return new UserProfileAppService(profileRepository, auditLogger);
    }

    @Bean
    public AvatarUploadAppService avatarUploadAppService(UserProfileAppService userProfileAppService,
                                                         ImageStorageGateway imageStorageGateway,
                                                         AvatarUploadRegistry avatarUploadRegistry,
                                                         AuditLogger auditLogger) {
        return new AvatarUploadAppService(userProfileAppService, imageStorageGateway,
                avatarUploadRegistry, auditLogger);
    }
}
