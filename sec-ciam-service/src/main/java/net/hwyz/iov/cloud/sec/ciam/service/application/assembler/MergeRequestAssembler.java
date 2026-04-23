package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.MergeRequestVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.MergeRequestDto2;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.MergeRequest;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.MergeRequestPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 账号合并申请 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MergeRequestAssembler {
    
    MergeRequestAssembler INSTANCE = Mappers.getMapper(MergeRequestAssembler.class);
    
    /**
     * DO -> Domain
     */
    MergeRequest toDomain(MergeRequestPo entity);
    
    /**
     * Domain -> DO
     */
    MergeRequestPo toDo(MergeRequest domain);

    /**
     * Domain -> DTO
     */
    MergeRequestDto2 toDto(MergeRequest domain);

    /**
     * DTO -> Domain
     */
    MergeRequest toEntity(MergeRequestDto2 dto);

    /**
     * DTO -> VO
     */
    MergeRequestVo toVo(MergeRequestDto2 dto);
}
