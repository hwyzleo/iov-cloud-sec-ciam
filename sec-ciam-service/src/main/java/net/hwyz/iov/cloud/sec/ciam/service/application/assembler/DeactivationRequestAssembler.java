package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.DeactivationRequestVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeactivationRequestDto2;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.DeactivationRequest;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.DeactivationRequestPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 账号注销申请 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeactivationRequestAssembler {
    
    DeactivationRequestAssembler INSTANCE = Mappers.getMapper(DeactivationRequestAssembler.class);
    
    /**
     * DO -> Domain
     */
    DeactivationRequest toDomain(DeactivationRequestPo entity);
    
    /**
     * Domain -> DO
     */
    DeactivationRequestPo toDo(DeactivationRequest domain);

    /**
     * Domain -> DTO
     */
    DeactivationRequestDto2 toDto(DeactivationRequest domain);

    /**
     * DTO -> Domain
     */
    DeactivationRequest toEntity(DeactivationRequestDto2 dto);

    /**
     * DTO -> VO
     */
    DeactivationRequestVo toVo(DeactivationRequestDto2 dto);
}
