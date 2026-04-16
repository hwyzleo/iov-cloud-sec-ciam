package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 登录标识领域服务 — 封装标识绑定、解绑、冲突检查与查询逻辑。
 * <p>
 * 支持手机号、邮箱、微信、Apple、Google、本机手机号等标识类型的管理。
 * 同一标识全局唯一，不允许挂载到多个账号。
 */
@Service
@RequiredArgsConstructor
public class IdentityDomainService {

    private final CiamUserIdentityRepository identityRepository;
    private final FieldEncryptor fieldEncryptor;

    /**
     * 绑定登录标识到用户。
     * <p>
     * 使用 FieldEncryptor 加密 identityValue 存储，并生成 SHA-256 哈希用于唯一查重。
     * 若该标识已被其他用户绑定，则抛出 {@link BusinessException}（IDENTITY_CONFLICT）。
     *
     * @param userId        用户业务唯一标识
     * @param identityType  标识类型
     * @param identityValue 标识原值（如手机号、邮箱）
     * @param countryCode   国家区号（手机号场景使用，其他可为 null）
     * @param bindSource    绑定来源
     * @return 新创建的标识数据对象
     */
    public CiamUserIdentityDo bindIdentity(String userId, IdentityType identityType,
                                           String identityValue, String countryCode,
                                           String bindSource) {
        String identityHash = FieldEncryptor.hash(identityValue);

        // 唯一性校验：检查该标识是否已被绑定
        Optional<CiamUserIdentityDo> existing = identityRepository.findByTypeAndHash(
                identityType.getCode(), identityHash);
        if (existing.isPresent() && existing.get().getIdentityStatus() == IdentityStatus.BOUND.getCode()) {
            if (!existing.get().getUserId().equals(userId)) {
                throw new BusinessException(CiamErrorCode.IDENTITY_CONFLICT);
            }
            // 已绑定到同一用户，直接返回
            return existing.get();
        }

        String encryptedValue = fieldEncryptor.encrypt(identityValue);

        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setIdentityId(UserIdGenerator.generate());
        identity.setUserId(userId);
        identity.setIdentityType(identityType.getCode());
        identity.setIdentityValue(encryptedValue);
        identity.setIdentityHash(identityHash);
        identity.setCountryCode(countryCode);
        identity.setVerifiedFlag(0);
        identity.setPrimaryFlag(0);
        identity.setBindSource(bindSource);
        identity.setBindTime(DateTimeUtil.now());
        identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
        identity.setRowVersion(1);
        identity.setRowValid(1);
        identity.setCreateTime(DateTimeUtil.now());
        identity.setModifyTime(DateTimeUtil.now());
        identityRepository.insert(identity);
        return identity;
    }

    /**
     * 解绑登录标识（将状态设为已解绑）。
     *
     * @param userId       用户业务唯一标识
     * @param identityType 标识类型
     * @param identityHash 标识哈希值
     */
    public void unbindIdentity(String userId, IdentityType identityType, String identityHash) {
        CiamUserIdentityDo identity = identityRepository.findByTypeAndHash(
                        identityType.getCode(), identityHash)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));

        if (!identity.getUserId().equals(userId)) {
            throw new BusinessException(CiamErrorCode.USER_NOT_FOUND);
        }

        identity.setIdentityStatus(IdentityStatus.UNBOUND.getCode());
        identity.setUnbindTime(DateTimeUtil.now());
        identity.setModifyTime(DateTimeUtil.now());
        identityRepository.updateByIdentityId(identity);
    }

    /**
     * 根据标识类型和哈希值查找标识记录。
     *
     * @param identityType 标识类型
     * @param identityHash 标识哈希值
     * @return 标识记录（如存在）
     */
    public Optional<CiamUserIdentityDo> findByTypeAndHash(IdentityType identityType, String identityHash) {
        return identityRepository.findByTypeAndHash(identityType.getCode(), identityHash);
    }

    /**
     * 查询用户所有已绑定的标识。
     *
     * @param userId 用户业务唯一标识
     * @return 已绑定标识列表
     */
    public List<CiamUserIdentityDo> findByUserId(String userId) {
        return identityRepository.findByUserId(userId).stream()
                .filter(i -> i.getIdentityStatus() == IdentityStatus.BOUND.getCode())
                .collect(Collectors.toList());
    }

    /**
     * 检查标识是否已被绑定到其他用户（冲突检测）。
     *
     * @param identityType  标识类型
     * @param identityValue 标识原值
     * @return 冲突用户的 userId（如存在），否则返回 empty
     */
    public Optional<String> checkConflict(IdentityType identityType, String identityValue) {
        String identityHash = FieldEncryptor.hash(identityValue);
        return identityRepository.findByTypeAndHash(identityType.getCode(), identityHash)
                .filter(i -> i.getIdentityStatus() == IdentityStatus.BOUND.getCode())
                .map(CiamUserIdentityDo::getUserId);
    }

    /**
     * 根据标识类型和原值查找标识记录（内部自动哈希）。
     *
     * @param identityType  标识类型
     * @param identityValue 标识原值（如手机号、邮箱）
     * @return 标识记录（如存在）
     */
    public Optional<CiamUserIdentityDo> findByTypeAndValue(IdentityType identityType, String identityValue) {
        String identityHash = FieldEncryptor.hash(identityValue);
        return identityRepository.findByTypeAndHash(identityType.getCode(), identityHash);
    }

    /**
     * 将标识标记为已验证。
     *
     * @param userId       用户业务唯一标识
     * @param identityType 标识类型
     * @param identityHash 标识哈希值
     */
    public void markVerified(String userId, IdentityType identityType, String identityHash) {
        CiamUserIdentityDo identity = identityRepository.findByTypeAndHash(
                        identityType.getCode(), identityHash)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));

        if (!identity.getUserId().equals(userId)) {
            throw new BusinessException(CiamErrorCode.USER_NOT_FOUND);
        }

        identity.setVerifiedFlag(1);
        identity.setModifyTime(DateTimeUtil.now());
        identityRepository.updateByIdentityId(identity);
    }

    /**
     * 统计用户已绑定的标识数量（用于解绑前校验是否至少保留一种登录方式）。
     *
     * @param userId 用户业务唯一标识
     * @return 已绑定标识数量
     */
    public long countBoundIdentities(String userId) {
        return identityRepository.findByUserId(userId).stream()
                .filter(i -> i.getIdentityStatus() == IdentityStatus.BOUND.getCode())
                .count();
    }

    /**
     * 检查标识绑定前是否存在冲突，返回冲突的完整标识记录。
     * <p>
     * 与 {@link #checkConflict} 不同，此方法返回完整的标识记录，
     * 便于账号合并流程获取冲突详情。
     *
     * @param identityType  标识类型
     * @param identityValue 标识原值
     * @param excludeUserId 排除的用户 ID（当前用户自身）
     * @return 冲突的标识记录（如存在），否则返回 empty
     */
    public Optional<CiamUserIdentityDo> checkConflictDetail(IdentityType identityType,
                                                            String identityValue,
                                                            String excludeUserId) {
        String identityHash = FieldEncryptor.hash(identityValue);
        return identityRepository.findByTypeAndHash(identityType.getCode(), identityHash)
                .filter(i -> i.getIdentityStatus() == IdentityStatus.BOUND.getCode())
                .filter(i -> !i.getUserId().equals(excludeUserId));
    }
}
