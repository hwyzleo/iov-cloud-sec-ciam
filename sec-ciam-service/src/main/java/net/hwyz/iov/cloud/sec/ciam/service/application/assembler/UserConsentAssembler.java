package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.UserConsentVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserConsentDto2;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserConsent;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserConsentPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 用户同意 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConsentAssembler {
    
    UserConsentAssembler INSTANCE = Mappers.getMapper(UserConsentAssembler.class);
    
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
    UserConsentDto2 toDto(UserConsent domain);

    /**
     * DTO -> Domain
     */
    UserConsent toEntity(UserConsentDto2 dto);

    /**
     * DTO -> VO
     */
    UserConsentVo toVo(UserConsentDto2 dto);
}
