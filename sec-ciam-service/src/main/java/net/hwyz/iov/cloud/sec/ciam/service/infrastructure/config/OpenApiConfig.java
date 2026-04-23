package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置 — 按端侧分组展示接口文档。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ciamOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CIAM 用户身份及访问管理系统")
                        .description("OpenIOV CIAM 服务 API 文档")
                        .version("v1.0.0"));
    }

    @Bean
    public GroupedOpenApi mobileApi() {
        return GroupedOpenApi.builder()
                .group("手机端")
                .pathsToMatch("/api/v1/mobile/**")
                .build();
    }

    @Bean
    public GroupedOpenApi vehicleApi() {
        return GroupedOpenApi.builder()
                .group("车机端")
                .pathsToMatch("/api/v1/vehicle/**")
                .build();
    }

    @Bean
    public GroupedOpenApi openApi() {
        return GroupedOpenApi.builder()
                .group("开放平台")
                .pathsToMatch("/api/v1/open/**")
                .build();
    }

    @Bean
    public GroupedOpenApi serviceApi() {
        return GroupedOpenApi.builder()
                .group("服务接口")
                .pathsToMatch("/api/v1/service/**")
                .build();
    }

    @Bean
    public GroupedOpenApi mpApi() {
        return GroupedOpenApi.builder()
                .group("管理后台")
                .pathsToMatch("/api/v1/mp/**")
                .build();
    }
}
