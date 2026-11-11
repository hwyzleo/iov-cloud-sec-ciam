package net.hwyz.iov.cloud.sec.ciam.service.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 头像上传相关配置契约测试（SEC-CIAM-DSN-CR-004 §7）。
 * <p>
 * 重点防止 Feign 配置键写错：edd-oss-api 的 ImageService 使用
 * {@code @FeignClient(name = "oss-service", contextId = "imageService")}，
 * 而 spring-cloud-openfeign 按 contextId 查找 {@code spring.cloud.openfeign.client.config.*}，
 * 因此配置键必须是 {@code imageService}，不是服务发现名 {@code oss-service}。
 */
class AvatarConfigBindingTest {

    private Map<String, Object> loadApplicationYaml() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application.yml", new ClassPathResource("application.yml"));
        Map<String, Object> flat = new java.util.HashMap<>();
        sources.forEach(source -> {
            if (source.getSource() instanceof Map<?, ?> map) {
                flatten("", map, flat);
            }
        });
        return flat;
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<?, ?> source, Map<String, Object> target) {
        source.forEach((key, value) -> {
            String path = prefix.isEmpty() ? String.valueOf(key) : prefix + "." + key;
            if (value instanceof Map<?, ?> nested) {
                flatten(path, nested, target);
            } else {
                target.put(path, value);
            }
        });
    }

    @Test
    @DisplayName("Feign 超时以 contextId 为键，且不使用服务发现名 oss-service")
    void feignTimeoutsKeyedByContextId() throws IOException {
        Map<String, Object> yaml = loadApplicationYaml();

        assertEquals("2000", String.valueOf(yaml.get(
                "spring.cloud.openfeign.client.config.imageService.connect-timeout")));
        assertEquals("5000", String.valueOf(yaml.get(
                "spring.cloud.openfeign.client.config.imageService.read-timeout")));
        assertFalse(yaml.keySet().stream().anyMatch(key -> key.contains("client.config.oss-service")),
                "Feign 配置键必须是 contextId(imageService)，不能用服务发现名 oss-service");
    }

    @Test
    @DisplayName("头像上传开关与归属登记 TTL 具有默认值")
    void avatarUploadPropertiesPresent() throws IOException {
        Map<String, Object> yaml = loadApplicationYaml();

        assertEquals("${CIAM_AVATAR_UPLOAD_ENABLED:true}",
                String.valueOf(yaml.get("ciam.avatar.upload.enabled")));
        assertEquals("${CIAM_AVATAR_UPLOAD_REGISTRY_TTL:960}",
                String.valueOf(yaml.get("ciam.avatar.upload.registry-ttl-seconds")));
    }

    @Test
    @DisplayName("配置可被 Spring Binder 正常绑定，且 Feign 配置键为 imageService")
    @SuppressWarnings("unchecked")
    void bindsThroughSpringBinder() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        new YamlPropertySourceLoader()
                .load("application.yml", new ClassPathResource("application.yml"))
                .forEach(environment.getPropertySources()::addFirst);

        // 使用 environment 绑定的 Binder 以支持 ${ENV:default} 占位符解析
        Binder binder = Binder.get(environment);
        Map<String, Object> feignConfig = binder
                .bind("spring.cloud.openfeign.client.config", Map.class)
                .orElse(Map.of());
        Long ttl = binder.bind("ciam.avatar.upload.registry-ttl-seconds", Long.class).orElse(null);

        assertTrue(feignConfig.containsKey("imageService"),
                "Feign 客户端配置必须以 contextId(imageService) 为键，实际: " + feignConfig.keySet());
        Object clientConfig = feignConfig.get("imageService");
        assertTrue(String.valueOf(clientConfig).contains("2000"),
                "connect-timeout 应为 2000，实际: " + clientConfig);
        assertTrue(String.valueOf(clientConfig).contains("5000"),
                "read-timeout 应为 5000，实际: " + clientConfig);
        assertFalse(feignConfig.containsKey("oss-service"),
                "不能用服务发现名 oss-service 作为 Feign 配置键");
        assertEquals(960L, ttl);
    }
}
