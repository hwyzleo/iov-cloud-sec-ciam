package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.OwnerCertificationVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.OwnerCertificationDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OwnerCertification;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamOwnerCertStateDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 车主认证 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OwnerCertificationMapper {
    
    OwnerCertificationMapper INSTANCE = Mappers.getMapper(OwnerCertificationMapper.class);
    
    /**
     * DO -> Domain
     */
    OwnerCertification toDomain(CiamOwnerCertStateDo entity);
    
    /**
     * Domain -> DO
     */
    CiamOwnerCertStateDo toDo(OwnerCertification domain);

    /**
     * Domain -> DTO
     */
    OwnerCertificationDto toDto(OwnerCertification domain);

    /**
     * DTO -> Domain
     */
    OwnerCertification toEntity(OwnerCertificationDto dto);

    /**
     * DTO -> VO
     */
    OwnerCertificationVo toVo(OwnerCertificationDto dto);
}
