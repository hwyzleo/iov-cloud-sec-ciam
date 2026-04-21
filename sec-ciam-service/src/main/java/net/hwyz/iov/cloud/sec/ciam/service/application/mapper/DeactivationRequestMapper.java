package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.DeactivationRequestVO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeactivationRequestDTO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.DeactivationRequest;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamDeactivationRequestDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 账号注销申请 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeactivationRequestMapper {
    
    DeactivationRequestMapper INSTANCE = Mappers.getMapper(DeactivationRequestMapper.class);
    
    /**
     * DO -> Domain
     */
    DeactivationRequest toDomain(CiamDeactivationRequestDo entity);
    
    /**
     * Domain -> DO
     */
    CiamDeactivationRequestDo toDo(DeactivationRequest domain);

    /**
     * Domain -> DTO
     */
    DeactivationRequestDTO toDto(DeactivationRequest domain);

    /**
     * DTO -> Domain
     */
    DeactivationRequest toEntity(DeactivationRequestDTO dto);

    /**
     * DTO -> VO
     */
    DeactivationRequestVO toVo(DeactivationRequestDTO dto);
}
