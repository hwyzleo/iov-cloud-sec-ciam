package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.InvitationRelationPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邀请关系表 Mapper。
 */
@Mapper
public interface CiamInvitationRelationMapper extends BaseMapper<InvitationRelationPo> {
}
