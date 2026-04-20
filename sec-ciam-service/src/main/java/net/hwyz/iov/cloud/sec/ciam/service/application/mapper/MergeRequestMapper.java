package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.MergeRequest;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamMergeRequestDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MergeRequestMapper {
    
    MergeRequestMapper INSTANCE = Mappers.getMapper(MergeRequestMapper.class);
    
    MergeRequest toDomain(CiamMergeRequestDo entity);
    
    CiamMergeRequestDo toDo(MergeRequest domain);
}
