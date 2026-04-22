package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.UserConsentVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserConsentDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserConsent;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserConsentPo;
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
    UserConsent toDomain(UserConsentPo entity);
    
    /**
     * Domain -> DO
     */
    UserConsentPo toDo(UserConsent domain);

    /**
     * Domain -> DTO
     */
    UserConsentDto toDto(UserConsent domain);

    /**
     * DTO -> Domain
     */
    UserConsent toEntity(UserConsentDto dto);

    /**
     * DTO -> VO
     */
    UserConsentVo toVo(UserConsentDto dto);
}
