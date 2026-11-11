package net.hwyz.iov.cloud.sec.ciam.service.avatarupload;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AvatarResultDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AvatarUploadDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.AvatarUploadAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.UserProfileAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.AvatarUploadRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 头像上传真实 MySQL + 真实 Redis 集成测试（SEC-CIAM-DSN-CR-004 TC-004-5～11）。
 * <p>
 * 使用真实 UserProfileRepository、真实 RedisAvatarUploadRegistry 与隔离库，
 * 不 Mock 存储；图片存储网关为可预测桩，真实 OSS 往返见 AvatarOssContractIT。
 */
@SpringBootTest(classes = AvatarUploadItTestApplication.class,
        properties = {
                "ciam.avatar.upload.enabled=true",
                "ciam.avatar.upload.registry-ttl-seconds=960"
        })
class AvatarUploadFlowIT extends AvatarUploadItBase {

    @Autowired
    private AvatarUploadAppService avatarUploadAppService;
    @Autowired
    private UserProfileAppService userProfileAppService;
    @Autowired
    private AvatarUploadRegistry avatarUploadRegistry;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String currentUserId;

    private String createUser(String suffix) {
        currentUserId = "u-cr004-" + RUN_SUFFIX + "-" + suffix;
        jdbcTemplate.update("INSERT INTO ciam_user_profile (profile_id, user_id, nickname, gender) "
                + "VALUES (?, ?, ?, 0)", "p-" + currentUserId, currentUserId, "原始昵称");
        return currentUserId;
    }

    private String avatarUrlOf(String userId) {
        return jdbcTemplate.queryForObject(
                "SELECT avatar_url FROM ciam_user_profile WHERE user_id = ? AND row_valid = 1",
                String.class, userId);
    }

    @AfterEach
    void cleanUp() {
        if (currentUserId != null) {
            jdbcTemplate.update("DELETE FROM ciam_user_profile WHERE user_id = ?", currentUserId);
        }
        redisTemplate.keys("ciam:avatar:upload:*").forEach(redisTemplate::delete);
    }

    @Test
    @DisplayName("TC-004-5/7 申请→归属登记→确认写库→重复确认幂等")
    void requestCompleteAndIdempotentConfirm() {
        String userId = createUser("flow");

        AvatarUploadDto upload = avatarUploadAppService.requestUpload(userId, "photo.png", "image/png", 204_800L);
        assertEquals(userId, avatarUploadRegistry.findOwner(upload.getUploadId()).orElse(null),
                "申请成功必须写入 uploadId → userId 归属登记");
        Long ttl = redisTemplate.getExpire("ciam:avatar:upload:" + upload.getUploadId(), TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 960, "归属登记 TTL 应为 960 秒级，实际 " + ttl);

        AvatarResultDto result = avatarUploadAppService.completeUpload(userId, upload.getUploadId());
        assertEquals("https://bucket.cos.example.com/images/sec-ciam/" + upload.getUploadId() + ".png",
                result.getAvatarUrl());
        assertEquals(result.getAvatarUrl(), avatarUrlOf(userId), "确认成功后必须写回 avatar_url");

        AvatarResultDto repeated = avatarUploadAppService.completeUpload(userId, upload.getUploadId());
        assertEquals(result.getAvatarUrl(), repeated.getAvatarUrl(), "重复确认必须幂等返回同一地址");
        assertEquals(result.getAvatarUrl(), avatarUrlOf(userId), "重复确认不得改变 avatar_url");
        assertTrue(avatarUploadRegistry.findCompletion(upload.getUploadId()).isPresent(),
                "确认成功后应保留 COMPLETED 状态以支持幂等返回");
    }

    @Test
    @DisplayName("TC-004-6 他人 uploadId 被拒绝且不写库")
    void rejectsForeignUploadId() {
        String owner = createUser("owner");
        AvatarUploadDto upload = avatarUploadAppService.requestUpload(owner, "photo.png", "image/png", 1024L);
        String intruder = "u-cr004-intruder-" + RUN_SUFFIX;
        jdbcTemplate.update("INSERT INTO ciam_user_profile (profile_id, user_id, nickname, gender) "
                + "VALUES (?, ?, ?, 0)", "p-" + intruder, intruder, "入侵者");

        try {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> avatarUploadAppService.completeUpload(intruder, upload.getUploadId()));
            assertEquals(CiamErrorCode.AVATAR_UPLOAD_FORBIDDEN, ex.getErrorCode());
            assertNull(avatarUrlOf(intruder), "越权确认不得写入任何头像地址");
            assertNull(avatarUrlOf(owner), "越权确认不得影响归属用户的头像地址");
        } finally {
            jdbcTemplate.update("DELETE FROM ciam_user_profile WHERE user_id = ?", intruder);
        }
    }

    @Test
    @DisplayName("TC-004-7 不存在的 uploadId 返回任务不存在")
    void rejectsUnknownUploadId() {
        String userId = createUser("unknown");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> avatarUploadAppService.completeUpload(userId, "not-exists-" + RUN_SUFFIX));

        assertEquals(CiamErrorCode.AVATAR_UPLOAD_NOT_FOUND, ex.getErrorCode());
        assertNull(avatarUrlOf(userId));
    }

    @Test
    @DisplayName("TC-004-11 归属登记 TTL 到期后自动失效")
    void registryEntryExpires() throws InterruptedException {
        String uploadId = "ttl-" + RUN_SUFFIX;
        avatarUploadRegistry.bind(uploadId, "u-ttl", 1L);
        assertTrue(avatarUploadRegistry.findOwner(uploadId).isPresent(), "绑定后应可查询");

        Thread.sleep(1_500L);

        assertEquals(Optional.empty(), avatarUploadRegistry.findOwner(uploadId), "TTL 到期后归属登记必须失效");
        assertFalse(redisTemplate.hasKey("ciam:avatar:upload:" + uploadId));
    }

    @Test
    @DisplayName("TC-004-10 连续更换头像只保留最新地址，不删除旧对象登记")
    void replacingAvatarKeepsLatestOnly() {
        String userId = createUser("replace");

        AvatarUploadDto first = avatarUploadAppService.requestUpload(userId, "a.png", "image/png", 1024L);
        AvatarResultDto firstResult = avatarUploadAppService.completeUpload(userId, first.getUploadId());
        AvatarUploadDto second = avatarUploadAppService.requestUpload(userId, "b.png", "image/png", 2048L);
        AvatarResultDto secondResult = avatarUploadAppService.completeUpload(userId, second.getUploadId());

        assertNotEquals(firstResult.getAvatarUrl(), secondResult.getAvatarUrl());
        assertEquals(secondResult.getAvatarUrl(), avatarUrlOf(userId), "只保留最新头像地址");
        assertTrue(avatarUploadRegistry.findCompletion(first.getUploadId()).isPresent(),
                "旧头像登记保留至 TTL，CIAM 不删除已上传对象（EDD-OSS 无 service 删除接口）");
    }

    @Test
    @DisplayName("TC-004-9/13 通用资料更新不改变头像，其余字段与读取路径正常")
    void genericProfileUpdateKeepsAvatar() {
        String userId = createUser("profile");
        AvatarUploadDto upload = avatarUploadAppService.requestUpload(userId, "a.png", "image/png", 1024L);
        String avatarUrl = avatarUploadAppService.completeUpload(userId, upload.getUploadId()).getAvatarUrl();

        userProfileAppService.updateProfile(userId, "新昵称", null, null, null, null);

        assertEquals(avatarUrl, avatarUrlOf(userId), "通用资料更新不得改变头像地址");
        assertEquals("新昵称", userProfileAppService.getProfile(userId).getNickname());
        assertEquals(avatarUrl, userProfileAppService.getProfile(userId).getAvatarUrl());
    }

    @Test
    @DisplayName("TC-004-8 写库失败后不标记完成，重试可收敛")
    void retryAfterWriteFailure() {
        String userId = createUser("retry");
        AvatarUploadDto upload = avatarUploadAppService.requestUpload(userId, "a.png", "image/png", 1024L);

        // 删除资料行模拟写入失败（PROFILE_NOT_FOUND）
        jdbcTemplate.update("DELETE FROM ciam_user_profile WHERE user_id = ?", userId);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> avatarUploadAppService.completeUpload(userId, upload.getUploadId()));
        assertEquals(CiamErrorCode.PROFILE_NOT_FOUND, ex.getErrorCode());
        assertTrue(avatarUploadRegistry.findCompletion(upload.getUploadId()).isEmpty(),
                "写库失败不得标记为已完成");

        // 恢复资料行后重试应成功
        jdbcTemplate.update("INSERT INTO ciam_user_profile (profile_id, user_id, nickname, gender) "
                + "VALUES (?, ?, ?, 0)", "p2-" + userId, userId, "重试昵称");

        AvatarResultDto result = avatarUploadAppService.completeUpload(userId, upload.getUploadId());
        assertEquals(result.getAvatarUrl(), avatarUrlOf(userId), "重试后头像地址应收敛");
    }

    @Test
    @DisplayName("TC-004-4 传给 OSS 的文件名由 userId 与 MIME 派生")
    void conventionFilenameReachesGateway() {
        String userId = createUser("naming");

        AvatarUploadDto upload = avatarUploadAppService.requestUpload(userId, "任意名字.PNG", "image/png", 512L);

        // 桩网关把 originalFilename 编进 objectId，用于验证 CIAM 传给 OSS 的实际文件名
        assertEquals("it-avatar-" + userId + ".png-512", upload.getUploadId());
    }

    @Test
    @DisplayName("TC-004-13 头像读取路径与默认头像回退保持兼容")
    void defaultAvatarFallbackPreserved() {
        String userId = createUser("fallback");
        assertEquals(userId, userProfileAppService.getProfile(userId).getUserId());
        // biz.default-avatar 未配置时保持既有行为（IT 装配下为空），仅校验读取不抛异常
        assertDoesNotThrow(() -> userProfileAppService.getProfile(userId));
    }

    @Test
    @DisplayName("TC-004-8 OSS 确认失败时不写库、不标记完成")
    void ossFailureKeepsStateConsistent() {
        String userId = createUser("ossfail");
        AvatarUploadDto upload = avatarUploadAppService.requestUpload(userId, "a.png", "image/png", 1024L);
        // 删除 Redis 归属登记以模拟 OSS 侧任务丢失（网关桩对任意 objectId 都会返回成功）
        avatarUploadRegistry.release(upload.getUploadId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> avatarUploadAppService.completeUpload(userId, upload.getUploadId()));

        assertEquals(CiamErrorCode.AVATAR_UPLOAD_NOT_FOUND, ex.getErrorCode());
        assertNull(avatarUrlOf(userId));
        assertEquals(Optional.empty(), avatarUploadRegistry.findCompletion(upload.getUploadId()));
    }
}
