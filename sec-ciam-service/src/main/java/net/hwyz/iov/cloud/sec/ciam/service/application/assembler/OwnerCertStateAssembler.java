package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.OwnerCertStateVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.OwnerCertStateDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OwnerCertState;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 车主认证状态 Assembler
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OwnerCertStateAssembler {

    OwnerCertStateAssembler INSTANCE = Mappers.getMapper(OwnerCertStateAssembler.class);

    /**
     * Domain -> DTO
     */
    OwnerCertStateDto toDto(OwnerCertState domain);

    /**
     * DTO -> VO
     */
    OwnerCertStateVo toVo(OwnerCertStateDto dto);

    /**
     * DTO -> Domain
     */
    OwnerCertState toDomain(OwnerCertStateDto dto);
}
