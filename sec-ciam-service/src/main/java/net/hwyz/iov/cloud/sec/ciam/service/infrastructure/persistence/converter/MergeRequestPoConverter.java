package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.MergeRequest;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.MergeRequestPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MergeRequestPoConverter {
    MergeRequestPoConverter INSTANCE = Mappers.getMapper(MergeRequestPoConverter.class);
    MergeRequest toDomain(MergeRequestPo po);
    MergeRequestPo toPo(MergeRequest domain);
}
