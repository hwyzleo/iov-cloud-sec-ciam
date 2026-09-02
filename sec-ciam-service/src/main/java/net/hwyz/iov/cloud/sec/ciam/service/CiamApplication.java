package net.hwyz.iov.cloud.sec.ciam.service;

import net.hwyz.iov.cloud.framework.security.annotation.EnableCustomFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CIAM 用户身份及访问管理系统启动类
 */
@EnableCustomFeignClients
@SpringBootApplication
public class CiamApplication {

    public static void main(String[] args) {
        SpringApplication.run(CiamApplication.class, args);
    }
}
