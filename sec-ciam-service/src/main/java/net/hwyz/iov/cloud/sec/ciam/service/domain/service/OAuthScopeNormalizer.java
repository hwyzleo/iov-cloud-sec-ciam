package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * OAuth scope 规范化（SEC-CIAM-DSN-CR-003 §4.3，RD-003-2）。
 * <p>
 * {@code ciam_oauth_client.scopes} 的 DDL 定义为空格分隔，但部分历史代码注释使用逗号分隔。
 * 本工具统一规范化规则：
 * <ol>
 *   <li>同时接受逗号和空白分隔；</li>
 *   <li>去除空项；</li>
 *   <li>保持配置中的声明顺序；</li>
 *   <li>拒绝重复项；</li>
 *   <li>输出单个空格分隔的 JWT scope；</li>
 *   <li>空配置视为非法，不签发硬编码 default scope。</li>
 * </ol>
 */
public final class OAuthScopeNormalizer {

    private static final String SEPARATOR_PATTERN = "[,\\s]+";
    private static final String SCOPE_JOINER = " ";

    private OAuthScopeNormalizer() {
    }

    /**
     * 规范化 scope 字符串。
     *
     * @param raw 原始 scope（逗号或空白分隔，可为 null/空白）
     * @return 单个空格分隔的 scope
     * @throws BusinessException {@link CiamErrorCode#SCOPE_EXCEEDED} 当 scope 为空或包含重复项时
     */
    public static String normalize(String raw) {
        List<String> entries = split(raw);
        if (entries.isEmpty()) {
            throw new BusinessException(CiamErrorCode.SCOPE_EXCEEDED, "客户端未配置有效授权范围");
        }

        Set<String> seen = new LinkedHashSet<>();
        for (String entry : entries) {
            if (!seen.add(entry)) {
                throw new BusinessException(CiamErrorCode.SCOPE_EXCEEDED, "客户端授权范围存在重复项：" + entry);
            }
        }
        return String.join(SCOPE_JOINER, seen);
    }

    /**
     * 拆分 scope 原始值（不校验重复，用于只读解析）。
     *
     * @param raw 原始 scope
     * @return 有序非空 scope 列表
     */
    public static List<String> split(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return List.of(raw.trim().split(SEPARATOR_PATTERN)).stream()
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .toList();
    }
}
