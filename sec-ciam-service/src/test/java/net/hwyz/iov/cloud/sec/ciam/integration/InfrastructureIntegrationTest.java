package net.hwyz.iov.cloud.sec.ciam.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.event.DomainEvent;
import net.hwyz.iov.cloud.sec.ciam.domain.event.DomainEventType;
import net.hwyz.iov.cloud.sec.ciam.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.domain.search.SearchService;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.event.KafkaDomainEventPublisher;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.CiamAuditLogRepositoryImpl;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.CiamSessionRepositoryImpl;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.CiamUserIdentityRepositoryImpl;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.CiamUserRepositoryImpl;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamAuditLogMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamSessionMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamUserIdentityMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamUserMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamAuditLogDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamSessionDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.StubSearchService;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.AuditLogSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.RiskEventSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.UserSearchDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 基础设施层集成测试。
 * <p>
 * 覆盖 MySQL 仓储层、Redis 验证码存储、Kafka 事件发布、Elasticsearch 检索服务
 * 四大基础设施组件的关键集成场景。测试不依赖外部服务，使用 Mock 替代真实中间件。
 */
class InfrastructureIntegrationTest {

    // ========================================================================
    // 1. MySQL 仓储层集成测试
    // ========================================================================

    @Nested
    @DisplayName("MySQL - 用户仓储集成")
    class UserRepositoryIntegrationTest {

        private CiamUserMapper mapper;
        private CiamUserRepositoryImpl repository;

        @BeforeEach
        void setUp() {
            mapper = mock(CiamUserMapper.class);
            repository = new CiamUserRepositoryImpl(mapper);
        }

        @Test
        @DisplayName("插入用户后可通过 userId 查询")
        void insertAndFindByUserId() {
            CiamUserDo user = buildUser("U001", 1, "mobile");
            when(mapper.insert(user)).thenReturn(1);
            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            int rows = repository.insert(user);
            assertEquals(1, rows);

            Optional<CiamUserDo> found = repository.findByUserId("U001");
            assertTrue(found.isPresent());
            assertEquals("U001", found.get().getUserId());
            assertEquals(1, found.get().getUserStatus());
        }

        @Test
        @DisplayName("查询不存在的用户返回空")
        void findByUserId_notFound() {
            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            Optional<CiamUserDo> found = repository.findByUserId("NONEXISTENT");
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("按用户状态查询列表")
        void findByUserStatus() {
            CiamUserDo u1 = buildUser("U001", 1, "mobile");
            CiamUserDo u2 = buildUser("U002", 1, "email");
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(u1, u2));

            List<CiamUserDo> result = repository.findByUserStatus(1);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("更新用户状态")
        void updateByUserId() {
            CiamUserDo user = buildUser("U001", 2, "mobile");
            when(mapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            int rows = repository.updateByUserId(user);
            assertEquals(1, rows);
            verify(mapper).update(eq(user), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("物理删除用户（注销场景）")
        void physicalDeleteByUserId() {
            when(mapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            int rows = repository.physicalDeleteByUserId("U001");
            assertEquals(1, rows);
            verify(mapper).delete(any(LambdaQueryWrapper.class));
        }

        private CiamUserDo buildUser(String userId, int status, String source) {
            CiamUserDo user = new CiamUserDo();
            user.setUserId(userId);
            user.setUserStatus(status);
            user.setBrandCode("OPENIOV");
            user.setRegisterSource(source);
            user.setRowValid(1);
            user.setRowVersion(1);
            user.setCreateTime(LocalDateTime.now());
            user.setModifyTime(LocalDateTime.now());
            return user;
        }
    }

    @Nested
    @DisplayName("MySQL - 登录标识仓储集成")
    class UserIdentityRepositoryIntegrationTest {

        private CiamUserIdentityMapper mapper;
        private CiamUserIdentityRepositoryImpl repository;

        @BeforeEach
        void setUp() {
            mapper = mock(CiamUserIdentityMapper.class);
            repository = new CiamUserIdentityRepositoryImpl(mapper);
        }

        @Test
        @DisplayName("通过标识类型和哈希值查询唯一标识")
        void findByTypeAndHash() {
            CiamUserIdentityDo identity = buildIdentity("ID001", "U001", "mobile", "hash123");
            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(identity);

            Optional<CiamUserIdentityDo> found = repository.findByTypeAndHash("mobile", "hash123");
            assertTrue(found.isPresent());
            assertEquals("mobile", found.get().getIdentityType());
            assertEquals("hash123", found.get().getIdentityHash());
        }

        @Test
        @DisplayName("查询用户所有有效标识")
        void findByUserId() {
            CiamUserIdentityDo mobile = buildIdentity("ID001", "U001", "mobile", "mhash");
            CiamUserIdentityDo email = buildIdentity("ID002", "U001", "email", "ehash");
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mobile, email));

            List<CiamUserIdentityDo> identities = repository.findByUserId("U001");
            assertEquals(2, identities.size());
        }

        @Test
        @DisplayName("标识唯一性校验 - 不存在时返回空")
        void findByTypeAndHash_notFound() {
            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            Optional<CiamUserIdentityDo> found = repository.findByTypeAndHash("wechat", "wxhash");
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("物理删除用户所有标识（注销场景）")
        void physicalDeleteByUserId() {
            when(mapper.delete(any(LambdaQueryWrapper.class))).thenReturn(3);

            int rows = repository.physicalDeleteByUserId("U001");
            assertEquals(3, rows);
        }

        private CiamUserIdentityDo buildIdentity(String identityId, String userId,
                                                   String type, String hash) {
            CiamUserIdentityDo identity = new CiamUserIdentityDo();
            identity.setIdentityId(identityId);
            identity.setUserId(userId);
            identity.setIdentityType(type);
            identity.setIdentityHash(hash);
            identity.setIdentityValue("encrypted_value");
            identity.setVerifiedFlag(1);
            identity.setPrimaryFlag(1);
            identity.setIdentityStatus(1);
            identity.setRowValid(1);
            identity.setRowVersion(1);
            identity.setBindTime(LocalDateTime.now());
            identity.setCreateTime(LocalDateTime.now());
            identity.setModifyTime(LocalDateTime.now());
            return identity;
        }
    }

    @Nested
    @DisplayName("MySQL - 会话仓储集成")
    class SessionRepositoryIntegrationTest {

        private CiamSessionMapper mapper;
        private CiamSessionRepositoryImpl repository;

        @BeforeEach
        void setUp() {
            mapper = mock(CiamSessionMapper.class);
            repository = new CiamSessionRepositoryImpl(mapper);
        }

        @Test
        @DisplayName("插入会话后可通过 sessionId 查询")
        void insertAndFindBySessionId() {
            CiamSessionDo session = buildSession("S001", "U001", "D001");
            when(mapper.insert(session)).thenReturn(1);
            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);

            repository.insert(session);
            Optional<CiamSessionDo> found = repository.findBySessionId("S001");

            assertTrue(found.isPresent());
            assertEquals("S001", found.get().getSessionId());
            assertEquals(SessionStatus.ACTIVE.getCode(), found.get().getSessionStatus());
        }

        @Test
        @DisplayName("按用户和状态查询有效会话列表")
        void findByUserIdAndStatus() {
            CiamSessionDo s1 = buildSession("S001", "U001", "D001");
            CiamSessionDo s2 = buildSession("S002", "U001", "D002");
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s1, s2));

            List<CiamSessionDo> sessions = repository.findByUserIdAndStatus("U001", SessionStatus.ACTIVE.getCode());
            assertEquals(2, sessions.size());
        }

        @Test
        @DisplayName("批量失效用户所有有效会话（密码修改场景）")
        void invalidateAllByUserId() {
            when(mapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(3);

            int rows = repository.invalidateAllByUserId("U001");
            assertEquals(3, rows);

            ArgumentCaptor<CiamSessionDo> captor = ArgumentCaptor.forClass(CiamSessionDo.class);
            verify(mapper).update(captor.capture(), any(LambdaUpdateWrapper.class));
            assertEquals(SessionStatus.INVALID.getCode(), captor.getValue().getSessionStatus());
            assertNotNull(captor.getValue().getLogoutTime());
        }

        @Test
        @DisplayName("按设备 ID 查询会话")
        void findByDeviceId() {
            CiamSessionDo session = buildSession("S001", "U001", "D001");
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(session));

            List<CiamSessionDo> sessions = repository.findByDeviceId("D001");
            assertEquals(1, sessions.size());
            assertEquals("D001", sessions.get(0).getDeviceId());
        }

        private CiamSessionDo buildSession(String sessionId, String userId, String deviceId) {
            CiamSessionDo session = new CiamSessionDo();
            session.setSessionId(sessionId);
            session.setUserId(userId);
            session.setDeviceId(deviceId);
            session.setClientType("app");
            session.setSessionStatus(SessionStatus.ACTIVE.getCode());
            session.setRiskLevel(0);
            session.setLoginTime(LocalDateTime.now());
            session.setLastActiveTime(LocalDateTime.now());
            session.setExpireTime(LocalDateTime.now().plusHours(24));
            session.setRowValid(1);
            session.setRowVersion(1);
            session.setCreateTime(LocalDateTime.now());
            session.setModifyTime(LocalDateTime.now());
            return session;
        }
    }

    @Nested
    @DisplayName("MySQL - 审计日志仓储集成")
    class AuditLogRepositoryIntegrationTest {

        private CiamAuditLogMapper mapper;
        private CiamAuditLogRepositoryImpl repository;

        @BeforeEach
        void setUp() {
            mapper = mock(CiamAuditLogMapper.class);
            repository = new CiamAuditLogRepositoryImpl(mapper);
        }

        @Test
        @DisplayName("插入审计日志并按 auditId 查询")
        void insertAndFindByAuditId() {
            CiamAuditLogDo log = buildAuditLog("A001", "U001", "LOGIN", "登录成功");
            when(mapper.insert(log)).thenReturn(1);
            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(log);

            repository.insert(log);
            Optional<CiamAuditLogDo> found = repository.findByAuditId("A001");

            assertTrue(found.isPresent());
            assertEquals("LOGIN", found.get().getEventType());
        }

        @Test
        @DisplayName("按用户和时间范围查询审计日志")
        void findByUserIdAndTimeRange() {
            CiamAuditLogDo log1 = buildAuditLog("A001", "U001", "LOGIN", "登录成功");
            CiamAuditLogDo log2 = buildAuditLog("A002", "U001", "LOGOUT", "退出登录");
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(log1, log2));

            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59);
            List<CiamAuditLogDo> logs = repository.findByUserIdAndTimeRange("U001", start, end);

            assertEquals(2, logs.size());
        }

        @Test
        @DisplayName("按 traceId 查询审计日志")
        void findByTraceId() {
            CiamAuditLogDo log = buildAuditLog("A001", "U001", "LOGIN", "登录成功");
            log.setTraceId("TRACE001");
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(log));

            List<CiamAuditLogDo> logs = repository.findByTraceId("TRACE001");
            assertEquals(1, logs.size());
            assertEquals("TRACE001", logs.get(0).getTraceId());
        }

        private CiamAuditLogDo buildAuditLog(String auditId, String userId,
                                               String eventType, String eventName) {
            CiamAuditLogDo log = new CiamAuditLogDo();
            log.setAuditId(auditId);
            log.setUserId(userId);
            log.setEventType(eventType);
            log.setEventName(eventName);
            log.setOperationResult(1);
            log.setEventTime(LocalDateTime.now());
            log.setRowValid(1);
            log.setRowVersion(1);
            log.setCreateTime(LocalDateTime.now());
            log.setModifyTime(LocalDateTime.now());
            return log;
        }
    }

    // ========================================================================
    // 2. Redis 验证码存储集成测试
    // ========================================================================

    @Nested
    @DisplayName("Redis - 验证码存储集成（InMemory 实现验证 VerificationCodeStore 契约）")
    class RedisVerificationCodeStoreIntegrationTest {

        private net.hwyz.iov.cloud.sec.ciam.infrastructure.store.InMemoryVerificationCodeStore store;

        @BeforeEach
        void setUp() {
            store = new net.hwyz.iov.cloud.sec.ciam.infrastructure.store.InMemoryVerificationCodeStore();
        }

        @Test
        @DisplayName("保存验证码后可正确获取")
        void saveAndGetCode() {
            store.saveCode("sms:U001:app", "123456", 300);

            Optional<String> code = store.getCode("sms:U001:app");
            assertTrue(code.isPresent());
            assertEquals("123456", code.get());
        }

        @Test
        @DisplayName("获取不存在的验证码返回空")
        void getCode_returnsEmpty_whenNotFound() {
            Optional<String> code = store.getCode("sms:U999:app");
            assertTrue(code.isEmpty());
        }

        @Test
        @DisplayName("删除验证码后无法获取")
        void deleteCode_removesEntry() {
            store.saveCode("sms:U001:app", "123456", 300);
            store.deleteCode("sms:U001:app");

            Optional<String> code = store.getCode("sms:U001:app");
            assertTrue(code.isEmpty());
        }

        @Test
        @DisplayName("频控标记 - 首次设置成功")
        void setIfAbsent_firstTime_returnsTrue() {
            assertTrue(store.setIfAbsent("rate:U001:app", 60));
        }

        @Test
        @DisplayName("频控标记 - 已存在时返回 false")
        void setIfAbsent_alreadyExists_returnsFalse() {
            store.setIfAbsent("rate:U001:app", 60);
            assertFalse(store.setIfAbsent("rate:U001:app", 60));
        }

        @Test
        @DisplayName("每日计数器递增 - 连续递增返回正确计数")
        void incrementDailyCount_incrementsCorrectly() {
            assertEquals(1, store.incrementDailyCount("daily:U001", 3600));
            assertEquals(2, store.incrementDailyCount("daily:U001", 3600));
            assertEquals(3, store.incrementDailyCount("daily:U001", 3600));
        }

        @Test
        @DisplayName("不同 key 的计数器互不影响")
        void incrementDailyCount_isolatedByKey() {
            assertEquals(1, store.incrementDailyCount("daily:U001", 3600));
            assertEquals(1, store.incrementDailyCount("daily:U002", 3600));
            assertEquals(2, store.incrementDailyCount("daily:U001", 3600));
        }

        @Test
        @DisplayName("覆盖已有验证码")
        void saveCode_overwritesExisting() {
            store.saveCode("sms:U001:app", "111111", 300);
            store.saveCode("sms:U001:app", "222222", 300);

            Optional<String> code = store.getCode("sms:U001:app");
            assertTrue(code.isPresent());
            assertEquals("222222", code.get());
        }

        @Test
        @DisplayName("clear 清空所有数据")
        void clear_removesAllData() {
            store.saveCode("sms:U001:app", "123456", 300);
            store.incrementDailyCount("daily:U001", 3600);

            store.clear();

            assertTrue(store.getCode("sms:U001:app").isEmpty());
            assertEquals(1, store.incrementDailyCount("daily:U001", 3600));
        }
    }

    // ========================================================================
    // 3. Kafka 事件发布集成测试
    // ========================================================================

    @Nested
    @DisplayName("Kafka - 领域事件发布集成")
    class KafkaEventPublisherIntegrationTest {

        private KafkaOperations<String, String> kafkaOperations;
        private KafkaDomainEventPublisher publisher;
        private ObjectMapper objectMapper;

        @SuppressWarnings("unchecked")
        @BeforeEach
        void setUp() {
            kafkaOperations = mock(KafkaOperations.class);
            publisher = new KafkaDomainEventPublisher(kafkaOperations);
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
        }

        @Test
        @DisplayName("发布事件后 JSON 可反序列化还原")
        void publish_jsonRoundTrip() throws Exception {
            Map<String, Object> payload = Map.of("identityType", "mobile", "channel", "app");
            DomainEvent event = DomainEvent.builder()
                    .eventType(DomainEventType.USER_REGISTERED)
                    .userId("U001")
                    .payload(payload)
                    .build();

            publisher.publish(event);

            ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaOperations).send(eq("ciam-domain-events"), eq("U001"), jsonCaptor.capture());

            // 验证 JSON 可反序列化
            String json = jsonCaptor.getValue();
            var tree = objectMapper.readTree(json);
            assertEquals("USER_REGISTERED", tree.get("eventType").asText());
            assertEquals("U001", tree.get("userId").asText());
            assertNotNull(tree.get("eventId"));
            assertNotNull(tree.get("timestamp"));
            assertEquals("mobile", tree.get("payload").get("identityType").asText());
        }

        @Test
        @DisplayName("连续发布多种事件类型到同一 Topic")
        void publish_multipleEventTypes() {
            for (DomainEventType type : DomainEventType.values()) {
                DomainEvent event = DomainEvent.builder()
                        .eventType(type)
                        .userId("U001")
                        .build();
                publisher.publish(event);
            }

            verify(kafkaOperations, times(DomainEventType.values().length))
                    .send(eq("ciam-domain-events"), anyString(), anyString());
        }

        @Test
        @DisplayName("事件 key 使用 userId 保证分区一致性")
        void publish_usesUserIdAsPartitionKey() {
            DomainEvent event = DomainEvent.builder()
                    .eventType(DomainEventType.LOGIN_SUCCESS)
                    .userId("U123")
                    .build();

            publisher.publish(event);

            verify(kafkaOperations).send(eq("ciam-domain-events"), eq("U123"), anyString());
        }

        @Test
        @DisplayName("Kafka 异常不影响业务主流程")
        void publish_kafkaFailure_doesNotPropagate() {
            when(kafkaOperations.send(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Broker unavailable"));

            DomainEvent event = DomainEvent.builder()
                    .eventType(DomainEventType.PASSWORD_CHANGED)
                    .userId("U001")
                    .build();

            assertDoesNotThrow(() -> publisher.publish(event));
        }

        @Test
        @DisplayName("发布注销完成事件包含完整载荷")
        void publish_deactivationEvent_containsPayload() throws Exception {
            Map<String, Object> payload = Map.of(
                    "retainAuditOnly", true,
                    "deactivationRequestId", "DR001"
            );
            DomainEvent event = DomainEvent.builder()
                    .eventType(DomainEventType.DEACTIVATION_COMPLETED)
                    .userId("U001")
                    .payload(payload)
                    .build();

            publisher.publish(event);

            ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaOperations).send(eq("ciam-domain-events"), eq("U001"), jsonCaptor.capture());

            var tree = objectMapper.readTree(jsonCaptor.getValue());
            assertEquals("DEACTIVATION_COMPLETED", tree.get("eventType").asText());
            assertTrue(tree.get("payload").get("retainAuditOnly").asBoolean());
            assertEquals("DR001", tree.get("payload").get("deactivationRequestId").asText());
        }
    }

    // ========================================================================
    // 4. Elasticsearch 检索服务集成测试
    // ========================================================================

    @Nested
    @DisplayName("Elasticsearch - 检索服务集成")
    class SearchServiceIntegrationTest {

        private SearchService searchService;

        @BeforeEach
        void setUp() {
            searchService = new StubSearchService();
        }

        @Test
        @DisplayName("SearchService 接口契约 - 用户检索返回正确结构")
        void searchUsers_contractCompliance() {
            SearchResult<UserSearchDocument> result = searchService.searchUsers("test", 0, 20);

            assertNotNull(result);
            assertNotNull(result.getItems());
            assertEquals(0, result.getPage());
            assertEquals(20, result.getSize());
            assertTrue(result.getTotal() >= 0);
        }

        @Test
        @DisplayName("SearchService 接口契约 - 审计日志检索支持全参数")
        void searchAuditLogs_fullParams() {
            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59);

            SearchResult<AuditLogSearchDocument> result =
                    searchService.searchAuditLogs("U001", "LOGIN", start, end, 0, 50);

            assertNotNull(result);
            assertNotNull(result.getItems());
            assertEquals(0, result.getPage());
            assertEquals(50, result.getSize());
        }

        @Test
        @DisplayName("SearchService 接口契约 - 风险事件检索支持全参数")
        void searchRiskEvents_fullParams() {
            LocalDateTime start = LocalDateTime.of(2026, 3, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 3, 31, 23, 59);

            SearchResult<RiskEventSearchDocument> result =
                    searchService.searchRiskEvents("U001", 2, start, end, 0, 10);

            assertNotNull(result);
            assertNotNull(result.getItems());
        }

        @Test
        @DisplayName("SearchService 接口契约 - 所有可选参数为 null 时不抛异常")
        void searchWithNullParams_doesNotThrow() {
            assertDoesNotThrow(() -> searchService.searchUsers(null, 0, 10));
            assertDoesNotThrow(() -> searchService.searchAuditLogs(null, null, null, null, 0, 10));
            assertDoesNotThrow(() -> searchService.searchRiskEvents(null, null, null, null, 0, 10));
        }

        @Test
        @DisplayName("UserSearchDocument 构建与字段完整性")
        void userSearchDocument_builderAndFields() {
            UserSearchDocument doc = UserSearchDocument.builder()
                    .userId("U001")
                    .userStatus(1)
                    .registerSource("mobile")
                    .registerChannel("app_store")
                    .lastLoginTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .build();

            assertEquals("U001", doc.getUserId());
            assertEquals(1, doc.getUserStatus());
            assertEquals("mobile", doc.getRegisterSource());
            assertEquals("app_store", doc.getRegisterChannel());
            assertNotNull(doc.getLastLoginTime());
            assertNotNull(doc.getCreateTime());
        }

        @Test
        @DisplayName("分页参数正确传递")
        void pagination_paramsPassedCorrectly() {
            SearchResult<UserSearchDocument> page0 = searchService.searchUsers("q", 0, 10);
            SearchResult<UserSearchDocument> page5 = searchService.searchUsers("q", 5, 20);

            assertEquals(0, page0.getPage());
            assertEquals(10, page0.getSize());
            assertEquals(5, page5.getPage());
            assertEquals(20, page5.getSize());
        }
    }
}
