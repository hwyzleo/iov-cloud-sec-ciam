package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.TagStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserTagRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserTagPo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 认证标签领域服务 — 封装标签写入、查询与变更逻辑。
 * <p>
 * 支持已实名（{@code real_name_verified}）、已车主认证（{@code owner_verified}）等标签的管理，
 * 并为下游系统提供标签读取能力。
 * <p>
 * 每个用户同一 tagCode 仅允许存在一条有效记录（唯一性约束）。
 */
@Service
@RequiredArgsConstructor
public class TagDomainService {

    private final CiamUserTagRepository tagRepository;

    /**
     * 为用户添加标签。
     * <p>
     * 若该用户已存在相同 tagCode 的有效标签，则抛出 {@link BusinessException}。
     *
     * @param userId    用户业务唯一标识
     * @param tagCode   标签编码（如 real_name_verified、owner_verified）
     * @param tagName   标签名称
     * @param tagSource 标签来源
     * @return 新创建的标签数据对象
     */
    public UserTagPo addTag(String userId, String tagCode, String tagName, String tagSource) {
        // 唯一性校验：同一用户同一 tagCode 仅允许一条有效记录
        Optional<UserTagPo> existing = tagRepository.findByUserIdAndTagCode(userId, tagCode);
        if (existing.isPresent() && existing.get().getTagStatus() == TagStatus.VALID.getCode()) {
            throw new BusinessException(CiamErrorCode.TAG_ALREADY_EXISTS);
        }

        UserTagPo tag = new UserTagPo();
        tag.setTagId(UserIdGenerator.generate());
        tag.setUserId(userId);
        tag.setTagCode(tagCode);
        tag.setTagName(tagName);
        tag.setTagStatus(TagStatus.VALID.getCode());
        tag.setTagSource(tagSource);
        tag.setEffectiveTime(DateTimeUtil.getNowInstant());
        tag.setRowVersion(1);
        tag.setRowValid(1);
        tag.setCreateTime(DateTimeUtil.getNowInstant());
        tag.setModifyTime(DateTimeUtil.getNowInstant());
        tagRepository.insert(tag);
        return tag;
    }

    /**
     * 移除用户标签（将 tag_status 设为失效）。
     *
     * @param userId  用户业务唯一标识
     * @param tagCode 标签编码
     */
    public void removeTag(String userId, String tagCode) {
        UserTagPo tag = tagRepository.findByUserIdAndTagCode(userId, tagCode)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.TAG_NOT_FOUND));
        tag.setTagStatus(TagStatus.INVALID.getCode());
        tag.setExpireTime(DateTimeUtil.getNowInstant());
        tag.setModifyTime(DateTimeUtil.getNowInstant());
        tagRepository.updateByTagId(tag);
    }

    /**
     * 获取用户所有有效标签。
     *
     * @param userId 用户业务唯一标识
     * @return 有效标签列表
     */
    public List<UserTagPo> getActiveTags(String userId) {
        return tagRepository.findByUserId(userId).stream()
                .filter(t -> t.getTagStatus() == TagStatus.VALID.getCode())
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否拥有指定的有效标签。
     *
     * @param userId  用户业务唯一标识
     * @param tagCode 标签编码
     * @return true 表示拥有该有效标签
     */
    public boolean hasTag(String userId, String tagCode) {
        return tagRepository.findByUserIdAndTagCode(userId, tagCode)
                .map(t -> t.getTagStatus() == TagStatus.VALID.getCode())
                .orElse(false);
    }

    /**
     * 更新用户标签状态。
     *
     * @param userId    用户业务唯一标识
     * @param tagCode   标签编码
     * @param newStatus 新状态值（参见 {@link TagStatus}）
     */
    public void updateTagStatus(String userId, String tagCode, int newStatus) {
        // 校验状态值合法性
        TagStatus.fromCode(newStatus);

        UserTagPo tag = tagRepository.findByUserIdAndTagCode(userId, tagCode)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.TAG_NOT_FOUND));
        tag.setTagStatus(newStatus);
        tag.setModifyTime(DateTimeUtil.getNowInstant());
        if (newStatus == TagStatus.INVALID.getCode()) {
            tag.setExpireTime(DateTimeUtil.getNowInstant());
        }
        tagRepository.updateByTagId(tag);
    }
}
