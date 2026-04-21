package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.UserConsentVO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserConsentDTO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserConsent;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserConsentDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 用户同意 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConsentMapper {
    
    UserConsentMapper INSTANCE = Mappers.getMapper(UserConsentMapper.class);
    
    /**
     * DO -> Domain
     */
    UserConsent toDomain(CiamUserConsentDo entity);
    
    /**
     * Domain -> DO
     */
    CiamUserConsentDo toDo(UserConsent domain);

    /**
     * Domain -> DTO
     */
    UserConsentDTO toDto(UserConsent domain);

    /**
     * DTO -> Domain
     */
    UserConsent toEntity(UserConsentDTO dto);

    /**
     * DTO -> VO
     */
    UserConsentVO toVo(UserConsentDTO dto);
}
