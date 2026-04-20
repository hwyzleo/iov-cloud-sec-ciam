package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OwnerCertification;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamOwnerCertStateDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OwnerCertificationMapper {
    
    OwnerCertificationMapper INSTANCE = Mappers.getMapper(OwnerCertificationMapper.class);
    
    OwnerCertification toDomain(CiamOwnerCertStateDo entity);
    
    CiamOwnerCertStateDo toDo(OwnerCertification domain);
}
