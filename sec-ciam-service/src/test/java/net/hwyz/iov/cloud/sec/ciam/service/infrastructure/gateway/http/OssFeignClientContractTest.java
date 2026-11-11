package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gateway.http;

import net.hwyz.iov.cloud.edd.oss.api.service.ImageService;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.annotation.AnnotatedElementUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * EDD-OSS Feign 客户端契约对齐测试（SEC-CIAM-DSN-CR-004 §7、§17）。
 * <p>
 * 锁定两个跨仓库约定：
 * <ul>
 *   <li>服务发现名 = OSS 注册名 edd-oss（Nacos 实际注册名，跨仓库一致性）；</li>
 *   <li>contextId = imageService，决定 CIAM 侧 Feign 配置键
 *       spring.cloud.openfeign.client.config.imageService.* 是否生效。</li>
 * </ul>
 */
class OssFeignClientContractTest {

    @Test
    @DisplayName("Feign 客户端使用 edd-oss 服务名与 imageService contextId")
    void feignClientIdMatchesOssServiceName() {
        FeignClient feignClient = AnnotatedElementUtils.getMergedAnnotation(ImageService.class, FeignClient.class);

        assertEquals(ServiceNameConstants.EDD_OSS, feignClient.name(),
                "服务发现名必须与 EDD-OSS 的 spring.application.name 一致；不一致将导致无实例并 fail-closed");
        assertEquals("edd-oss", feignClient.name());
        assertEquals("imageService", feignClient.contextId(),
                "CIAM 的 Feign 配置键必须与 contextId 一致（imageService），不是服务发现名");
        assertEquals("/api/service/image/v1", feignClient.path());
    }
}
