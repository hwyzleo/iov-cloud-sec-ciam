package net.hwyz.iov.cloud.sec.ciam.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.UserStatus;

import java.util.Map;
import java.util.Set;

/**
 * 用户状态机 — 定义合法状态流转规则并拦截非法流转。
 * <p>
 * 合法流转：
 * <ul>
 *   <li>PENDING → ACTIVE（验证完成）</li>
 *   <li>ACTIVE → LOCKED（密码错误 / 管理员操作）</li>
 *   <li>ACTIVE → DISABLED（管理员禁用）</li>
 *   <li>ACTIVE → DEACTIVATING（注销申请）</li>
 *   <li>LOCKED → ACTIVE（解锁 / 超时恢复）</li>
 *   <li>DISABLED → ACTIVE（管理员启用）</li>
 *   <li>DEACTIVATING → DEACTIVATED（注销完成）</li>
 *   <li>DEACTIVATING → ACTIVE（注销取消）</li>
 * </ul>
 */
public final class UserStatusMachine {

    private static final Map<UserStatus, Set<UserStatus>> TRANSITIONS = Map.of(
            UserStatus.PENDING, Set.of(UserStatus.ACTIVE),
            UserStatus.ACTIVE, Set.of(UserStatus.LOCKED, UserStatus.DISABLED, UserStatus.DEACTIVATING),
            UserStatus.LOCKED, Set.of(UserStatus.ACTIVE),
            UserStatus.DISABLED, Set.of(UserStatus.ACTIVE),
            UserStatus.DEACTIVATING, Set.of(UserStatus.DEACTIVATED, UserStatus.ACTIVE)
    );

    private UserStatusMachine() {
    }

    /**
     * 校验状态流转是否合法。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return {@code true} 合法，{@code false} 非法
     */
    public static boolean canTransit(UserStatus from, UserStatus to) {
        Set<UserStatus> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * 执行状态流转校验，非法时抛出 {@link BusinessException}。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @throws BusinessException 非法流转时抛出 ILLEGAL_STATUS_TRANSITION
     */
    public static void validateTransition(UserStatus from, UserStatus to) {
        if (!canTransit(from, to)) {
            throw new BusinessException(CiamErrorCode.ILLEGAL_STATUS_TRANSITION,
                    String.format("不允许从 [%s] 流转到 [%s]", from.getDescription(), to.getDescription()));
        }
    }
}
