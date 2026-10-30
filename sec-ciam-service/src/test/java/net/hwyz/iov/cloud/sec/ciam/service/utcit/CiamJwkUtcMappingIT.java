package net.hwyz.iov.cloud.sec.ciam.service.utcit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamJwkMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.JwkPo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SEC-CIAM-DSN-CR-002 C1：ciam_jwk 字段级 {@code UtcInstantTypeHandler} 真实 MP 读写映射测试
 * （US-002-3 / RD-002-3 / TC-002-4，C1 范围）。
 * <p>
 * 语义与边界：
 * <ul>
 *   <li>试点仅验证<b>隔离读写路径</b>（合成数据），不宣示 ciam_jwk 生产安全结论（EX-CIAM-04 属 C3+）；</li>
 *   <li>DB 默认/ON UPDATE 行为<b>只记录现状差异</b>（边界 2），本批不切换审计属主、不改业务代码；</li>
 *   <li>覆盖真实 MP 路径（边界 3）：insert / selectById / updateById / LambdaUpdateWrapper /
 *       LambdaQueryWrapper（含 Instant 条件绑定）/ orderBy 读路径 / null / 精度截断 / 负 epoch / 跨日；</li>
 *   <li>双 JVM 可移植性由两次独立 JVM 运行（UTC 与 Asia/Shanghai）输出同一断言集证明（TC-002-2）；</li>
 *   <li>本测试 {@code iov.mysql.time.enabled=false}（不启用严格探针），与
 *       {@link CiamUtcStrictProbeIT}（enabled=true，严格启动/拒绝）分开（TC-002-2 分离要求）。</li>
 * </ul>
 */
@SpringBootTest(classes = CiamUtcItTestApplication.class,
        properties = {
                "spring.cloud.bootstrap.enabled=false",
                "spring.flyway.enabled=false",
                "iov.mysql.time.enabled=false"
        })
@DisplayName("C1: ciam_jwk 字段级 handler 真实 MP 读写（隔离库）")
class CiamJwkUtcMappingIT extends CiamUtcItBase {

    /** 毫秒对齐固定业务事件时刻（DATETIME(3) 可精确往返）。 */
    private static final Instant ISSUE = Instant.parse("2026-10-30T01:02:03.456Z");
    private static final Instant EXPIRE = Instant.parse("2026-10-30T02:02:03.456Z");
    /** 负 epoch（跨 UTC 换日边界）。 */
    private static final Instant NEGATIVE = Instant.parse("1969-12-31T23:59:59.999Z");
    /** 亚毫秒值：DATETIME(3) 存储截断到毫秒（记录行为，RD-002-5）。 */
    private static final Instant SUB_MILLI = Instant.parse("2026-10-30T01:02:03.456789Z");
    /** 容忍窗：DB 生成时刻与测试进程时钟的偏差上限（隔离本机库，5 分钟足够）。 */
    private static final long DB_NOW_TOLERANCE_MS = 5L * 60 * 1000;

    private static final DateTimeFormatter DB_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("UTC"));

    @Autowired
    private CiamJwkMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        mapper.delete(new LambdaQueryWrapper<JwkPo>()
                .likeRight(JwkPo::getKeyId, "it-jwk-" + RUN_SUFFIX));
    }

    private JwkPo newPo(String keyId) {
        JwkPo po = new JwkPo();
        po.setKeyId(keyId);
        po.setPrivateKeyPem("-----BEGIN PRIVATE KEY-----it-test-synthetic-----END PRIVATE KEY-----");
        po.setPublicKeyPem("-----BEGIN PUBLIC KEY-----it-test-synthetic-----END PUBLIC KEY-----");
        po.setAlgorithm("RSA");
        po.setKeySize(2048);
        po.setStatus(1);
        po.setIsPrimary(0);
        po.setIssueTime(ISSUE);
        po.setExpireTime(EXPIRE);
        return po;
    }

    private String key(String tag) {
        return "it-jwk-" + RUN_SUFFIX + "-" + tag;
    }

    @Test
    @DisplayName("insert: 业务事件毫秒精确往返 + DB 审计默认值(create/modify)UTC 生成")
    void insert_roundTrip_andDbAuditDefaultsAreUtc() {
        JwkPo po = newPo(key("base"));
        // 与真实 JwkDomainService 一致：createTime/modifyTime 不 set（DB DEFAULT/ON UPDATE 生成）
        int rows = mapper.insert(po);
        assertEquals(1, rows);
        assertNotNull(po.getId(), "insert 应回填自增主键");

        JwkPo loaded = mapper.selectById(po.getId());
        assertNotNull(loaded);
        assertEquals(ISSUE, loaded.getIssueTime(), "issue_time 毫秒级精确往返");
        assertEquals(EXPIRE, loaded.getExpireTime(), "expire_time 毫秒级精确往返");
        assertNotNull(loaded.getCreateTime(), "create_time 由 DB DEFAULT CURRENT_TIMESTAMP(3) 生成");
        assertNotNull(loaded.getModifyTime(), "modify_time 由 DB DEFAULT CURRENT_TIMESTAMP(3) 生成");

        // 证据：DB 生成的审计时刻必须为 UTC 墙钟（若存了本地墙钟，读回按 UTC 还原会偏差 8h）
        long createDeltaMs = Math.abs(System.currentTimeMillis() - loaded.getCreateTime().toEpochMilli());
        long modifyDeltaMs = Math.abs(System.currentTimeMillis() - loaded.getModifyTime().toEpochMilli());
        assertTrue(createDeltaMs < DB_NOW_TOLERANCE_MS,
                "create_time 应为近当前时刻(UTC 墙钟)，实际偏差 " + createDeltaMs + "ms: " + loaded.getCreateTime());
        assertTrue(modifyDeltaMs < DB_NOW_TOLERANCE_MS,
                "modify_time 应为近当前时刻(UTC 墙钟)，实际偏差 " + modifyDeltaMs + "ms: " + loaded.getModifyTime());

        // 证据：存储墙钟字符串 == UTC 墙钟（原始 SQL 核验，必须用 getObject(LocalDateTime.class)
        // —— Connector/J 的 getTimestamp() 会把 DATETIME 墙钟按会话时区换算后按 JVM 时区解释，
        // 是 JVM 相关的；getObject(LocalDateTime.class) 原样返回墙钟，JVM 无关，与 handler 读路径一致）
        LocalDateTime storedWallClock = jdbcTemplate.queryForObject(
                "SELECT issue_time FROM ciam_jwk WHERE id = ?",
                (rs, rowNum) -> rs.getObject(1, LocalDateTime.class), loaded.getId());
        assertEquals(DB_DATETIME_FORMAT.format(ISSUE),
                storedWallClock.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
                "DATETIME 列存储的必须是 UTC 墙钟（非本地墙钟），且读取不依赖 JVM 时区");
    }

    @Test
    @DisplayName("insert: 显式旧 create_time 覆盖 DB 默认 —— 记录现状，本批不切换属主")
    void insert_explicitOldAuditValueOverridesDefault_recordBehavior() {
        JwkPo po = newPo(key("explicit"));
        Instant oldCreate = Instant.parse("2020-01-01T00:00:00.000Z");
        po.setCreateTime(oldCreate);
        mapper.insert(po);

        JwkPo loaded = mapper.selectById(po.getId());
        // 记录现状：显式写入的审计列值被 DB 采纳（DEFAULT 仅在省略列时生效）；
        // 目标态（DB 唯一属主、app 排除写入）属 C3 协调，本批不改业务代码。
        assertEquals(oldCreate, loaded.getCreateTime(), "显式旧值覆盖 DB 默认（现状记录）");
    }

    @Test
    @DisplayName("updateById: 真实行变化触发 ON UPDATE(3)，业务事件列不变")
    void updateById_realChangeTriggersOnUpdate() throws InterruptedException {
        JwkPo po = newPo(key("upd"));
        mapper.insert(po);
        JwkPo before = mapper.selectById(po.getId());
        Instant modifyBefore = before.getModifyTime();
        assertNotNull(modifyBefore);

        Thread.sleep(1200); // 跨毫秒，保证 ON UPDATE 产生可观察的新值

        JwkPo update = new JwkPo();
        update.setId(po.getId());
        update.setStatus(2); // 真实行变化（撤销场景同型：JwkDomainService.revokeKey）
        int rows = mapper.updateById(update);
        assertEquals(1, rows);

        JwkPo after = mapper.selectById(po.getId());
        assertEquals(2, after.getStatus());
        assertTrue(after.getModifyTime().isAfter(modifyBefore),
                "ON UPDATE CURRENT_TIMESTAMP(3) 应在真实行变化时推进 modify_time");
        assertEquals(ISSUE, after.getIssueTime(), "业务事件列不受审计 ON UPDATE 影响");
        assertEquals(EXPIRE, after.getExpireTime());
    }

    @Test
    @DisplayName("updateById: 显式 modify_time 覆盖 DB 自动值 —— 记录现状")
    void updateById_explicitModifyTimeOverridesOnUpdate_recordBehavior() {
        JwkPo po = newPo(key("explicitmod"));
        mapper.insert(po);

        JwkPo update = new JwkPo();
        update.setId(po.getId());
        update.setStatus(3);
        Instant explicitModify = Instant.parse("2021-06-01T12:00:00.000Z");
        update.setModifyTime(explicitModify);
        mapper.updateById(update);

        JwkPo after = mapper.selectById(po.getId());
        assertEquals(explicitModify, after.getModifyTime(), "显式 modify_time 覆盖 ON UPDATE（现状记录）");
    }

    @Test
    @DisplayName("updateById: 无实际行变化时 ON UPDATE 不推进 —— 记录行为")
    void updateById_noRealChangeOnUpdateNotAdvanced() {
        JwkPo po = newPo(key("nochange"));
        mapper.insert(po);
        JwkPo loaded = mapper.selectById(po.getId());
        Instant modifyBefore = loaded.getModifyTime();

        // 整行原值重写（同值 SET）→ 行未变化 → ON UPDATE 不推进（RD-002-4：不承诺无变化递增）
        mapper.updateById(loaded);

        JwkPo again = mapper.selectById(po.getId());
        assertEquals(modifyBefore, again.getModifyTime(),
                "同值更新不触发 ON UPDATE（MySQL 行未变化）");
    }

    @Test
    @DisplayName("LambdaUpdateWrapper: 与 JwkRepositoryImpl.revokePrimary/update 同模式的更新路径")
    void lambdaUpdateWrapper_repoLikeUpdatePath() {
        JwkPo po = newPo(key("wrapper"));
        mapper.insert(po);

        // JwkRepositoryImpl.revokePrimary 同型：仅置 is_primary=0（不携带审计列）
        JwkPo revoke = new JwkPo();
        revoke.setIsPrimary(0);
        int rows = mapper.update(revoke,
                new LambdaUpdateWrapper<JwkPo>()
                        .eq(JwkPo::getIsPrimary, 0)
                        .eq(JwkPo::getKeyId, po.getKeyId()));
        assertEquals(1, rows);

        JwkPo after = mapper.selectById(po.getId());
        assertEquals(0, after.getIsPrimary());
        assertNotNull(after.getModifyTime());
        assertEquals(ISSUE, after.getIssueTime(), "Wrapper 更新不触碰业务事件列");
    }

    @Test
    @DisplayName("selectList: LambdaQueryWrapper 含 Instant 条件 —— 读路径与条件绑定")
    void selectList_lambdaWrapperInstantCondition() {
        mapper.insert(newPo(key("c1")));
        mapper.insert(newPo(key("c2")));
        mapper.insert(newPo(key("c3")));

        // eq Instant 条件（边界 3：验证 Wrapper 参数经字段 handler 绑定，而非默认 Date 语义）
        java.util.List<JwkPo> byExpire = mapper.selectList(
                new LambdaQueryWrapper<JwkPo>()
                        .eq(JwkPo::getExpireTime, EXPIRE)
                        .likeRight(JwkPo::getKeyId, "it-jwk-" + RUN_SUFFIX));
        assertEquals(3, byExpire.size(), "Instant 条件应与存储的 UTC 墙钟精确匹配");

        // gt Instant 条件：issue_time > 2026-10-30T01:02:03.455Z 应命中全部 3 行
        java.util.List<JwkPo> byIssueGt = mapper.selectList(
                new LambdaQueryWrapper<JwkPo>()
                        .gt(JwkPo::getIssueTime, Instant.parse("2026-10-30T01:02:03.455Z"))
                        .likeRight(JwkPo::getKeyId, "it-jwk-" + RUN_SUFFIX));
        assertEquals(3, byIssueGt.size());

        // lt 条件（跨日边界）：2026-10-30T03:00:00Z 应命中全部
        java.util.List<JwkPo> byIssueLt = mapper.selectList(
                new LambdaQueryWrapper<JwkPo>()
                        .lt(JwkPo::getIssueTime, Instant.parse("2026-10-30T03:00:00Z"))
                        .likeRight(JwkPo::getKeyId, "it-jwk-" + RUN_SUFFIX));
        assertEquals(3, byIssueLt.size());
    }

    @Test
    @DisplayName("selectOne: orderByDesc(issue_time) —— JwkRepositoryImpl.findPrimary 同型读路径")
    void selectOne_orderByDescIssueTime_findPrimaryLike() {
        mapper.insert(newPo(key("o1")));
        JwkPo newer = newPo(key("o2"));
        newer.setIssueTime(Instant.parse("2026-10-30T05:00:00.000Z"));
        mapper.insert(newer);

        JwkPo latest = mapper.selectOne(
                new LambdaQueryWrapper<JwkPo>()
                        .eq(JwkPo::getIsPrimary, 0)
                        .eq(JwkPo::getStatus, 1)
                        .orderByDesc(JwkPo::getIssueTime)
                        .last("LIMIT 1"));
        assertNotNull(latest);
        assertEquals(newer.getKeyId(), latest.getKeyId(), "ORDER BY issue_time DESC 应取最新");
    }

    @Test
    @DisplayName("null 往返: expire_time NULL（永不过期语义）不被转换破坏")
    void null_expireTimeRoundTrip() {
        JwkPo po = newPo(key("null"));
        po.setExpireTime(null);
        mapper.insert(po);
        JwkPo loaded = mapper.selectById(po.getId());
        assertNull(loaded.getExpireTime(), "NULL 应保持 NULL");
        assertEquals(ISSUE, loaded.getIssueTime());
    }

    @Test
    @DisplayName("负 epoch + 跨日边界 + 亚毫秒截断（记录 DATETIME(3) 精度行为）")
    void negativeEpoch_crossDay_subMilliTruncation() {
        JwkPo po = newPo(key("neg"));
        po.setIssueTime(NEGATIVE);
        mapper.insert(po);
        JwkPo loaded = mapper.selectById(po.getId());
        assertEquals(NEGATIVE, loaded.getIssueTime(), "负 epoch 精确往返");

        JwkPo sub = newPo(key("sub"));
        sub.setIssueTime(SUB_MILLI);
        mapper.insert(sub);
        JwkPo loadedSub = mapper.selectById(sub.getId());
        // 记录 MySQL 8.0.33 实际行为：DATETIME(3) 对亚毫秒小数秒做<b>四舍五入</b>（半进位）而非截断：
        // 01:02:03.456789 → .457（RD-002-5：存储精度行为由列定义决定，handler 不截断；
        // 该舍入对安全截止时刻有 <1ms 级影响，属 US-002-5 边界记录，不自动豁免）
        assertEquals(Instant.parse("2026-10-30T01:02:03.457Z"), loadedSub.getIssueTime(),
                "DATETIME(3) 亚毫秒按 MySQL 规则四舍五入到毫秒（实测 .456789→.457）");
    }

    @Test
    @DisplayName("记录: 当前 JVM 时区与固定 Instant 往返（双 JVM 证据的锚点断言）")
    void currentJvmTimezone_anchorRoundTrip() {
        // 本断言不依赖 JVM 默认时区：UTC 与 Asia/Shanghai 两次独立运行结果一致（TC-002-2）
        JwkPo po = newPo(key("tz"));
        mapper.insert(po);
        JwkPo loaded = mapper.selectById(po.getId());
        assertEquals(ISSUE, loaded.getIssueTime());
        System.out.println("[CiamUtcItBase] 锚点断言通过：JVM 默认时区=" + ZoneId.systemDefault()
                + "，issue_time 往返=" + loaded.getIssueTime());
    }
}
