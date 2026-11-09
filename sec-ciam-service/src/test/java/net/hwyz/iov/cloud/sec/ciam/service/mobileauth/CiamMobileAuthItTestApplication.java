package net.hwyz.iov.cloud.sec.ciam.service.mobileauth;

import com.alibaba.druid.pool.DruidDataSource;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.AuthenticationAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.SessionAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.TokenAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.cache.RedisVerificationCodeStore;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.config.CiamAuthenticationProperties;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * SEC-CIAM-DSN-CR-003 隔离集成测试最小装配（真实 MySQL + 真实 Redis）。
 * <p>
 * 只装配本 CR 需要的仓储/领域服务与应用服务，不使用 Nacos，不加载 CIAM 全量业务组件。
 * 依赖外部基础设施的框架自动装配按名称排除（security/redis/kafka/audit/datascope 等）。
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
        // 本 CR 的 IT 不验证框架时间治理探针（属 SEC-CIAM-DSN-CR-002 范围）；
        // Instant 映射由 JDBC URL 的 connectionTimeZone=UTC&preserveInstants=true 保证，
        // 使 IT 不依赖运行 JVM 的时区（避免与 CR-001/CR-002 测试互相污染）。
        "net.hwyz.iov.cloud.framework.mysql.time.MysqlTimeAutoConfiguration"
})
@MapperScan("net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper")
@ComponentScan(basePackages = {
        "net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository",
        "net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter"
})
public class CiamMobileAuthItTestApplication {

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

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${ciam.it.redis.host}") String host,
            @Value("${ciam.it.redis.port}") int port,
            @Value("${ciam.it.redis.password:}") String password,
            @Value("${ciam.it.redis.database:15}") int database) {
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
    public FieldEncryptor fieldEncryptor() {
        // 测试专用合成密钥（32 字节 base64），不使用生产密钥
        return new FieldEncryptor("Y2lhbS1jcjAwMy1pdC10ZXN0LWtleS0zMmJ5dGVzISE=");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder(4);
    }

    @Bean
    public AuditLogger auditLogger() {
        return event -> {
        };
    }

    @Bean
    public SmsAdapter smsAdapter() {
        return (mobile, countryCode, code) -> AdapterResult.ok("it-sms");
    }

    @Bean
    public EmailAdapter emailAdapter() {
        return (email, code) -> AdapterResult.ok("it-email");
    }

    /**
     * 手机认证配置：值由 {@code @SpringBootTest(properties = ...)} 注入，
     * 不使用编程式赋值（{@code @ConfigurationProperties} 后处理器会按环境重新绑定）。
     */
    @Bean
    public CiamAuthenticationProperties authenticationProperties() {
        return new CiamAuthenticationProperties();
    }

    @Bean
    public JwkDomainService jwkDomainService(JwkRepository jwkRepository) {
        JwkDomainService service = new JwkDomainService(jwkRepository);
        service.initialize();
        return service;
    }

    @Bean
    public JwtTokenService jwtTokenService(JwkDomainService jwkDomainService) {
        return new JwtTokenService(jwkDomainService);
    }

    @Bean
    public SessionDomainService sessionDomainService(SessionRepository sessionRepository,
                                                     RefreshTokenRepository refreshTokenRepository,
                                                     DeviceRepository deviceRepository) {
        return new SessionDomainService(sessionRepository, refreshTokenRepository, deviceRepository);
    }

    @Bean
    public RefreshTokenDomainService refreshTokenDomainService(RefreshTokenRepository repository) {
        return new RefreshTokenDomainService(repository);
    }

    @Bean
    public OAuthClientDomainService oauthClientDomainService(OAuthClientRepository repository,
                                                             PasswordEncoder passwordEncoder) {
        return new OAuthClientDomainService(repository, passwordEncoder);
    }

    @Bean
    public IdentityDomainService identityDomainService(UserIdentityRepository repository,
                                                       FieldEncryptor fieldEncryptor) {
        return new IdentityDomainService(repository, fieldEncryptor);
    }

    @Bean
    public UserDomainService userDomainService(UserRepository userRepository,
                                               UserProfileRepository userProfileRepository) {
        return new UserDomainService(userRepository, userProfileRepository);
    }

    @Bean
    public DeviceDomainService deviceDomainService(DeviceRepository deviceRepository) {
        return new DeviceDomainService(deviceRepository);
    }

    @Bean
    public VerificationCodeStore verificationCodeStore(StringRedisTemplate redisTemplate) {
        return new RedisVerificationCodeStore(redisTemplate);
    }

    @Bean
    public VerificationCodeService verificationCodeService(VerificationCodeStore codeStore,
                                                           SmsAdapter smsAdapter,
                                                           EmailAdapter emailAdapter,
                                                           CiamAuthenticationProperties properties) {
        return new VerificationCodeService(codeStore, smsAdapter, emailAdapter, properties);
    }

    @Bean
    public AuthenticationAppService authenticationAppService(
            VerificationCodeService verificationCodeService,
            IdentityDomainService identityDomainService,
            UserDomainService userDomainService,
            UserRepository userRepository,
            AuditLogger auditLogger,
            SessionDomainService sessionDomainService,
            JwtTokenService jwtTokenService,
            RefreshTokenDomainService refreshTokenDomainService,
            DeviceDomainService deviceDomainService,
            OAuthClientDomainService oauthClientDomainService,
            CiamAuthenticationProperties properties,
            PlatformTransactionManager transactionManager) {
        return new AuthenticationAppService(
                verificationCodeService, identityDomainService, userDomainService, userRepository,
                auditLogger, null, null, sessionDomainService,
                null, null, null, null,
                jwtTokenService, refreshTokenDomainService, deviceDomainService,
                oauthClientDomainService, properties,
                new TransactionTemplate(transactionManager));
    }

    @Bean
    public SessionAppService sessionAppService(SessionDomainService sessionDomainService) {
        return new SessionAppService(sessionDomainService);
    }

    @Bean
    public TokenAppService tokenAppService(JwtTokenService jwtTokenService,
                                           SessionDomainService sessionDomainService) {
        return new TokenAppService(jwtTokenService, sessionDomainService);
    }
}
