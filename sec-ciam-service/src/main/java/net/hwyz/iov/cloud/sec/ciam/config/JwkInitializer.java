package net.hwyz.iov.cloud.sec.ciam.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.domain.service.JwkDomainService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * JWK 密钥初始化监听器。
 * <p>
 * 应用启动完成后自动初始化 JWK 密钥：
 * <ul>
 *   <li>如数据库中已有主密钥 → 加载到内存</li>
 *   <li>如无主密钥 → 生成新密钥并存储</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwkInitializer {

    private final JwkDomainService jwkDomainService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("开始初始化 JWK 密钥...");
        jwkDomainService.initialize();
        log.info("JWK 密钥初始化完成");
    }
}
