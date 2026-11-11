package net.hwyz.iov.cloud.sec.ciam.service.adapter.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.hwyz.iov.cloud.framework.common.constant.SecurityConstants;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.MobileAccountController;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AvatarResultDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AvatarUploadDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 手机端头像上传接口契约测试（SEC-CIAM-DSN-CR-004 §6、TC-004-1/5/9）。
 * <p>
 * 使用独立 MockMvc 装配：不启动安全过滤器，鉴权边界由 SEC-CIAM-DSN-CR-003 的过滤器测试覆盖；
 * 本测试只验证路由、DTO 契约与「通用资料更新忽略 avatarUrl」的写入面收敛行为。
 */
class MobileAccountAvatarControllerTest {

    private static final String USER_ID = "u-avatar-1";

    private UserProfileAppService userProfileAppService;
    private AvatarUploadAppService avatarUploadAppService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userProfileAppService = mock(UserProfileAppService.class);
        avatarUploadAppService = mock(AvatarUploadAppService.class);

        MobileAccountController controller = new MobileAccountController(
                userProfileAppService,
                avatarUploadAppService,
                mock(AccountBindingAppService.class),
                mock(SessionAppService.class),
                mock(PasswordChangeAppService.class),
                mock(PasswordResetAppService.class),
                mock(AccountLifecycleAppService.class),
                mock(ConsentAppService.class),
                mock(OwnerCertificationAppService.class));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // 与 Spring Boot 默认一致：未知字段不导致 400，avatarUrl 被忽略而不是报错
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        SecurityContextHolder.set(SecurityConstants.USER_ID, USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.getLocalMap().clear();
    }

    @Test
    @DisplayName("TC-004-1 申请头像上传返回 uploadId、预签名地址与过期时间")
    void requestAvatarUpload() throws Exception {
        when(avatarUploadAppService.requestUpload(USER_ID, "photo.png", "image/png", 204_800L))
                .thenReturn(AvatarUploadDto.builder()
                        .uploadId("obj-1")
                        .uploadUrl("https://cos.example.com/presigned")
                        .httpMethod("PUT")
                        .contentType("image/png")
                        .expiresAt(1_762_900_000_000L)
                        .build());

        mockMvc.perform(post("/api/mobile/account/v1/profile/avatar/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalFilename\":\"photo.png\",\"contentType\":\"image/png\",\"size\":204800}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("000000"))
                .andExpect(jsonPath("$.data.uploadId").value("obj-1"))
                .andExpect(jsonPath("$.data.uploadUrl").value("https://cos.example.com/presigned"))
                .andExpect(jsonPath("$.data.httpMethod").value("PUT"))
                .andExpect(jsonPath("$.data.expiresAt").value(1_762_900_000_000L));

        verify(avatarUploadAppService).requestUpload(USER_ID, "photo.png", "image/png", 204_800L);
    }

    @Test
    @DisplayName("TC-004-5 完成确认返回已生效头像地址")
    void completeAvatarUpload() throws Exception {
        when(avatarUploadAppService.completeUpload(USER_ID, "obj-1"))
                .thenReturn(AvatarResultDto.builder()
                        .avatarUrl("https://bucket.cos.example.com/images/sec-ciam/obj-1.png")
                        .objectId("obj-1")
                        .contentType("image/png")
                        .size(204_800L)
                        .build());

        mockMvc.perform(post("/api/mobile/account/v1/profile/avatar/obj-1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl")
                        .value("https://bucket.cos.example.com/images/sec-ciam/obj-1.png"))
                .andExpect(jsonPath("$.data.objectId").value("obj-1"));

        verify(avatarUploadAppService).completeUpload(USER_ID, "obj-1");
    }

    @Test
    @DisplayName("TC-004-9 通用资料更新忽略 avatarUrl，且不再透传给应用服务")
    void updateProfileIgnoresAvatarUrl() throws Exception {
        mockMvc.perform(put("/api/mobile/account/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"新昵称\",\"avatarUrl\":\"https://evil.example.com/x.png\"}"))
                .andExpect(status().isOk());

        verify(userProfileAppService).updateProfile(eq(USER_ID), eq("新昵称"),
                isNull(), isNull(), isNull(), isNull());
        verify(userProfileAppService, never()).updateAvatarUrl(anyString(), anyString());
    }
}
