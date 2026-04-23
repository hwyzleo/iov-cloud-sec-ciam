package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.application.dto.InvitationRelationDto2;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.InvitationRelation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 邀请关系 Assembler
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvitationRelationAssembler {

    InvitationRelationAssembler INSTANCE = Mappers.getMapper(InvitationRelationAssembler.class);

    /**
     * Domain -> DTO
     */
    InvitationRelationDto2 toDto(InvitationRelation domain);

    /**
     * DTO -> Domain
     */
    InvitationRelation toDomain(InvitationRelationDto2 dto);
}
