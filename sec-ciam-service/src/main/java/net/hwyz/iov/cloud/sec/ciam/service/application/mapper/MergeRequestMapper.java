package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.MergeRequestVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.MergeRequestDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.MergeRequest;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamMergeRequestDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 账号合并申请 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MergeRequestMapper {
    
    MergeRequestMapper INSTANCE = Mappers.getMapper(MergeRequestMapper.class);
    
    /**
     * DO -> Domain
     */
    MergeRequest toDomain(CiamMergeRequestDo entity);
    
    /**
     * Domain -> DO
     */
    CiamMergeRequestDo toDo(MergeRequest domain);

    /**
     * Domain -> DTO
     */
    MergeRequestDto toDto(MergeRequest domain);

    /**
     * DTO -> Domain
     */
    MergeRequest toEntity(MergeRequestDto dto);

    /**
     * DTO -> VO
     */
    MergeRequestVo toVo(MergeRequestDto dto);
}
