package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.DeactivationRequest;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamDeactivationRequestDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeactivationRequestMapper {
    
    DeactivationRequestMapper INSTANCE = Mappers.getMapper(DeactivationRequestMapper.class);
    
    DeactivationRequest toDomain(CiamDeactivationRequestDo entity);
    
    CiamDeactivationRequestDo toDo(DeactivationRequest domain);
}
