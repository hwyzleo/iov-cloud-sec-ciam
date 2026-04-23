package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.InvitationRelation;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.InvitationRelationPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 邀请关系 PO 转换器
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvitationRelationPoConverter {

    InvitationRelationPoConverter INSTANCE = Mappers.getMapper(InvitationRelationPoConverter.class);

    /**
     * PO -> Domain
     */
    InvitationRelation toDomain(InvitationRelationPo po);

    /**
     * Domain -> PO
     */
    InvitationRelationPo toDo(InvitationRelation domain);
}
