package net.hwyz.iov.cloud.sec.ciam.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeactivationRequestDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.MergeRequestDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserIdentityDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.DeactivationRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.MergeRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.UserIdentityMapper;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.UserQuery;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchService;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.*;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.AuditLogSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.RiskEventSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.UserSearchDocument;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 运营后台查询应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountQueryAppService {

    private final CiamUserRepository userRepository;
    private final CiamUserIdentityRepository identityRepository;
    private final CiamUserProfileRepository profileRepository;
    private final CiamUserTagRepository tagRepository;
    private final CiamMergeRequestRepository mergeRequestRepository;
    private final CiamDeactivationRequestRepository deactivationRequestRepository;
    private final SearchService searchService;
    private final FieldEncryptor fieldEncryptor;

    public UserDetail queryUser(String userId) {
        CiamUserDo user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));

        String identityType = null;
        String identityValue = null;

        List<CiamUserIdentityDo> identities = identityRepository.findByUserId(userId);
        for (CiamUserIdentityDo identity : identities) {
            if (IdentityType.fromCode(identity.getIdentityType()) == IdentityType.MOBILE ||
                    IdentityType.fromCode(identity.getIdentityType()) == IdentityType.EMAIL) {
                identityType = identity.getIdentityType();
                try {
                    identityValue = fieldEncryptor.decrypt(identity.getIdentityValue());
                } catch (Exception e) {
                    identityValue = identity.getIdentityValue();
                }
                break;
            }
        }

        String nickname = null;
        Integer gender = null;
        Optional<CiamUserProfileDo> profileOpt = profileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            nickname = profileOpt.get().getNickname();
            gender = profileOpt.get().getGender();
        }

        log.info("查询用户详情: userId={}", userId);
        return new UserDetail(
                user.getUserId(),
                user.getUserStatus(),
                user.getRegisterSource(),
                user.getRegisterChannel(),
                user.getPrimaryIdentityType(),
                DateTimeUtil.instantToOffsetDateTime(user.getLastLoginTime()),
                DateTimeUtil.instantToOffsetDateTime(user.getCreateTime()),
                user.getDescription(),
                nickname,
                gender,
                identityType,
                identityValue
        );
    }

    /**
     * 检索用户列表
     */
    public List<UserSearchDocument> queryUserList(UserQuery query) {
        List<CiamUserDo> userList = userRepository.search(query);

        // 使用 PageUtil.convert 确保分页元数据透传
        return PageUtil.convert(userList, user -> {
            UserSearchDocument doc = UserSearchDocument.builder()
                    .userId(user.getUserId())
                    .userStatus(user.getUserStatus())
                    .registerSource(user.getRegisterSource())
                    .registerChannel(user.getRegisterChannel())
                    .lastLoginTime(DateTimeUtil.instantToOffsetDateTime(user.getLastLoginTime()))
                    .createTime(DateTimeUtil.instantToOffsetDateTime(user.getCreateTime()))
                    .build();

            // 补充关联字段
            List<CiamUserIdentityDo> identities = identityRepository.findByUserId(user.getUserId());
            for (CiamUserIdentityDo identity : identities) {
                if (IdentityType.fromCode(identity.getIdentityType()) == IdentityType.MOBILE ||
                        IdentityType.fromCode(identity.getIdentityType()) == IdentityType.EMAIL) {
                    doc.setIdentityType(identity.getIdentityType());
                    try {
                        doc.setIdentityValue(fieldEncryptor.decrypt(identity.getIdentityValue()));
                    } catch (Exception e) {
                        doc.setIdentityValue(identity.getIdentityValue());
                    }
                    break;
                }
            }
            profileRepository.findByUserId(user.getUserId()).ifPresent(profile -> {
                doc.setNickname(profile.getNickname());
                doc.setGender(profile.getGender());
            });
            return doc;
        });
    }

    public List<UserIdentityDTO> queryBindingRelations(String userId) {
        userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));
        return identityRepository.findByUserId(userId).stream()
                .map(doObj -> UserIdentityMapper.INSTANCE.toDto(UserIdentityMapper.INSTANCE.toDomain(doObj)))
                .collect(Collectors.toList());
    }

    public List<MergeRequestDTO> queryMergeRequests(int reviewStatus) {
        List<CiamMergeRequestDo> all = mergeRequestRepository.findByReviewStatus(reviewStatus);
        return PageUtil.convert(all, doObj -> MergeRequestMapper.INSTANCE.toDto(MergeRequestMapper.INSTANCE.toDomain(doObj)));
    }

    public List<DeactivationRequestDTO> queryDeactivationRequests(int reviewStatus) {
        List<CiamDeactivationRequestDo> all = deactivationRequestRepository.findByReviewStatus(reviewStatus);
        return PageUtil.convert(all, doObj -> DeactivationRequestMapper.INSTANCE.toDto(DeactivationRequestMapper.INSTANCE.toDomain(doObj)));
    }

    public SearchResult<AuditLogSearchDocument> queryAuditLogs(String userId, String eventType,
                                                               LocalDateTime startTime, LocalDateTime endTime,
                                                               int page, int size) {
        return searchService.searchAuditLogs(userId, eventType, startTime, endTime, page, size);
    }

    public SearchResult<RiskEventSearchDocument> queryRiskEvents(String userId, Integer riskLevel,
                                                                 LocalDateTime startTime, LocalDateTime endTime,
                                                                 int page, int size) {
        return searchService.searchRiskEvents(userId, riskLevel, startTime, endTime, page, size);
    }

    public record UserDetail(
            String userId,
            Integer userStatus,
            String registerSource,
            String registerChannel,
            String primaryIdentityType,
            OffsetDateTime lastLoginTime,
            OffsetDateTime createTime,
            String description,
            String nickname,
            Integer gender,
            String identityType,
            String identityValue
    ) {
    }
}
