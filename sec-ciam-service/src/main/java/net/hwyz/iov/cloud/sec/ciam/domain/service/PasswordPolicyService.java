package net.hwyz.iov.cloud.sec.ciam.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 密码策略服务 — 校验密码复杂度。
 * <p>
 * 策略要求：
 * <ul>
 *   <li>最小长度 8 位</li>
 *   <li>必须包含大写字母</li>
 *   <li>必须包含小写字母</li>
 *   <li>必须包含数字</li>
 *   <li>必须包含特殊字符</li>
 * </ul>
 */
@Service
public class PasswordPolicyService {

    static final int MIN_LENGTH = 8;

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^a-zA-Z0-9]");

    /**
     * 校验密码是否满足复杂度要求。
     *
     * @param rawPassword 原始密码
     * @throws BusinessException 不满足时抛出 PASSWORD_COMPLEXITY_INSUFFICIENT
     */
    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new BusinessException(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT);
        }
        if (!UPPERCASE.matcher(rawPassword).find()) {
            throw new BusinessException(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT);
        }
        if (!LOWERCASE.matcher(rawPassword).find()) {
            throw new BusinessException(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT);
        }
        if (!DIGIT.matcher(rawPassword).find()) {
            throw new BusinessException(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT);
        }
        if (!SPECIAL.matcher(rawPassword).find()) {
            throw new BusinessException(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT);
        }
    }

    /**
     * 检查密码是否满足复杂度要求（不抛异常）。
     *
     * @param rawPassword 原始密码
     * @return 满足返回 true
     */
    public boolean isValid(String rawPassword) {
        try {
            validate(rawPassword);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }
}
