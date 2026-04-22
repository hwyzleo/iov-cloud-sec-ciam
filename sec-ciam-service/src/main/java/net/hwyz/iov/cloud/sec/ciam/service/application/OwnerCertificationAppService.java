package net.hwyz.iov.cloud.sec.ciam.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.OwnerCertificationDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.OwnerCertificationMapper;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CertStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamOwnerCertStateRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.TagDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamOwnerCertStateDo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 车主认证状态应用服务 — 编排认证回调处理、状态查询与补偿机制。
 * <p>
 * 职责：
 * <ul>
 *   <li>处理外部车主认证服务的异步回调，更新认证状态与标签</li>
 *   <li>查询用户当前车主认证状态</li>
 *   <li>补偿机制：对未收到回调的认证记录主动查询外部服务（预留）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerCertificationAppService {

    private final CiamOwnerCertStateRepository certStateRepository;
    private final TagDomainService tagDomainService;
    private final AuditLogger auditLogger;

    private static final String TAG_CODE_OWNER_VERIFIED = "owner_verified";
    private static final String TAG_NAME_OWNER_VERIFIED = "已车主认证";

    /**
     * 处理外部车主认证服务的异步回调。
     * <p>
     * 根据回调结果更新或创建认证状态记录，并同步维护 owner_verified 标签。
     *
     * @param userId     用户业务唯一标识
     * @param certResult 认证结果（对应 {@link CertStatus} 编码）
     * @param vin        车辆识别号
     * @param certSource 认证来源
     */
    public void handleCertificationCallback(String userId, int certResult,
                                            String vin, String certSource) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "userId 不能为空");
        }
        // Validate certResult is a known CertStatus
        CertStatus certStatus = CertStatus.fromCode(certResult);

        // Find existing record for this user+vin, or create new
        Optional<CiamOwnerCertStateDo> existing = findExistingRecord(userId, vin);

        if (existing.isPresent()) {
            CiamOwnerCertStateDo record = existing.get();
            record.setCertStatus(certResult);
            record.setCertSource(certSource);
            record.setCallbackTime(DateTimeUtil.getNowInstant());
            record.setModifyTime(DateTimeUtil.getNowInstant());
            if (certStatus == CertStatus.CERTIFIED) {
                record.setEffectiveTime(DateTimeUtil.getNowInstant());
            }
            certStateRepository.updateByOwnerCertId(record);
            log.info("车主认证回调更新: userId={}, vin={}, certStatus={}", userId, vin, certStatus);
        } else {
            CiamOwnerCertStateDo record = new CiamOwnerCertStateDo();
            record.setOwnerCertId(UserIdGenerator.generate());
            record.setUserId(userId);
            record.setVin(vin);
            record.setCertStatus(certResult);
            record.setCertSource(certSource);
            record.setCallbackTime(DateTimeUtil.getNowInstant());
            record.setRowVersion(1);
            record.setRowValid(1);
            record.setCreateTime(DateTimeUtil.getNowInstant());
            record.setModifyTime(DateTimeUtil.getNowInstant());
            if (certStatus == CertStatus.CERTIFIED) {
                record.setEffectiveTime(DateTimeUtil.getNowInstant());
            }
            certStateRepository.insert(record);
            log.info("车主认证回调新建: userId={}, vin={}, certStatus={}", userId, vin, certStatus);
        }

        // Sync owner_verified tag
        syncOwnerTag(userId, certStatus, certSource);

        logAudit(userId, AuditEventType.OWNER_CERT_CALLBACK, true);
    }

    /**
     * 查询用户当前车主认证状态。
     *
     * @param userId 用户业务唯一标识
     * @return 该用户所有认证状态记录
     */
    public List<OwnerCertificationDto> queryCertificationStatus(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "userId 不能为空");
        }
        List<CiamOwnerCertStateDo> records = certStateRepository.findByUserId(userId);
        logAudit(userId, AuditEventType.OWNER_CERT_QUERY, true);
        return records.stream()
                .map(doObj -> OwnerCertificationMapper.INSTANCE.toDto(OwnerCertificationMapper.INSTANCE.toDomain(doObj)))
                .collect(Collectors.toList());
    }

    /**
     * 补偿机制：对未收到回调的认证记录主动查询外部服务。
     * <p>
     * 当前为预留实现，查找处于"认证中"状态的记录并更新最后查询时间。
     * 实际外部查询逻辑待外部认证服务适配器就绪后补充。
     *
     * @param userId 用户业务唯一标识
     * @return 需要补偿的认证中记录列表
     */
    public List<OwnerCertificationDto> compensateCertificationStatus(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "userId 不能为空");
        }
        List<CiamOwnerCertStateDo> pendingRecords =
                certStateRepository.findByUserIdAndCertStatus(userId, CertStatus.CERTIFYING.getCode());

        for (CiamOwnerCertStateDo record : pendingRecords) {
            record.setLastQueryTime(DateTimeUtil.getNowInstant());
            record.setModifyTime(DateTimeUtil.getNowInstant());
            certStateRepository.updateByOwnerCertId(record);
        }

        if (!pendingRecords.isEmpty()) {
            log.info("车主认证补偿查询: userId={}, pendingCount={}", userId, pendingRecords.size());
        }

        logAudit(userId, AuditEventType.OWNER_CERT_COMPENSATE, true);
        return pendingRecords.stream()
                .map(doObj -> OwnerCertificationMapper.INSTANCE.toDto(OwnerCertificationMapper.INSTANCE.toDomain(doObj)))
                .collect(Collectors.toList());
    }

    /**
     * Find existing cert record for user+vin combination.
     */
    private Optional<CiamOwnerCertStateDo> findExistingRecord(String userId, String vin) {
        return certStateRepository.findByUserId(userId).stream()
                .filter(r -> vin != null && vin.equals(r.getVin()))
                .findFirst();
    }

    /**
     * Sync the owner_verified tag based on certification result.
     */
    private void syncOwnerTag(String userId, CertStatus certStatus, String certSource) {
        boolean hasTag = tagDomainService.hasTag(userId, TAG_CODE_OWNER_VERIFIED);

        if (certStatus == CertStatus.CERTIFIED && !hasTag) {
            tagDomainService.addTag(userId, TAG_CODE_OWNER_VERIFIED,
                    TAG_NAME_OWNER_VERIFIED, certSource);
            log.info("添加车主认证标签: userId={}", userId);
        } else if (certStatus != CertStatus.CERTIFIED && hasTag) {
            tagDomainService.removeTag(userId, TAG_CODE_OWNER_VERIFIED);
            log.info("移除车主认证标签: userId={}", userId);
        }
    }

    private void logAudit(String userId, AuditEventType eventType, boolean success) {
        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .eventType(eventType.getCategory())
                .eventName(eventType.getDescription())
                .success(success)
                .eventTime(DateTimeUtil.getNowInstant())
                .build());
    }
}
